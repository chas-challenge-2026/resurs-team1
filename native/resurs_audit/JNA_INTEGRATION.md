# `resurs_audit` Java/JNA integration

This is the integration handoff for Java and deployment teams. The native
library exposes a small C-linkage ABI from [wrapper.h](wrapper.h); C++-specific
details are documented separately in [README.md](README.md).

## Library names and packaged files

| Platform | File | JNA load name |
|---|---|---|
| Linux x86-64 | `libresurs_audit.so` | `resurs_audit` |
| Windows x86-64 | `resurs_audit.dll` | `resurs_audit` |

Prebuilt files are located at:

```text
prebuilt/linux-x86-64/libresurs_audit.so
prebuilt/win32-x86-64/resurs_audit.dll
```

Load the platform library without its prefix or extension:

```java
Native.load("resurs_audit", ResursAuditLibrary.class);
```

The Java application owns library placement, extraction from a JAR if used,
and configuration of `jna.library.path` or another native search location.

## Supported exported functions

Exactly these functions are public:

```c
void generate_private_key(unsigned char *output_buffer);

int get_public_key(
    unsigned char *private_key,
    unsigned char *output_public_key);

int wrapper_hash_and_sign(
    const uint8_t *canonical_data,
    size_t canonical_data_length,
    const uint8_t *private_key,
    size_t private_key_length,
    uint8_t *output_hash,
    uint8_t *output_signature);

VerifyChainResult wrapper_verify_chain(
    const AuditEntry *entries,
    size_t entry_count,
    const uint8_t *public_key,
    size_t public_key_length);
```

`wrapper_hash` and `wrapper_sign` are internal implementation helpers. They are
not exported and must not be declared or called by Java.

### Function results

| Function | Result |
|---|---|
| `generate_private_key` | No status return; writes 32 bytes to the output buffer. |
| `get_public_key` | `0` on success, `-1` if the private key cannot be converted. |
| `wrapper_hash_and_sign` | `0` on success, `-1` if its internal hash/sign operation fails. |
| `wrapper_verify_chain` | Returns `VerifyChainResult`; see the result-code table below. |

Output buffers are valid only after a successful call. The current ABI does
not receive output-buffer lengths, so Java must allocate the exact minimum
sizes below and must not pass null references.

## Required binary sizes

| Buffer | Required size |
|---|---:|
| Private key output/input | 32 bytes |
| Public key output/input | 32 bytes |
| Hash output/current hash/previous hash | 32 bytes |
| Signature output/input | 64 bytes |

All values are binary. Do not use Java strings or assume null termination.

## Native structures

The native field order is ABI-significant:

```cpp
struct AuditEntry
{
    const uint8_t *canonicalData;
    size_t canonicalDataLength;
    uint8_t signature[64];
    uint8_t previousHash[32];
    uint8_t currentHash[32];
    uint64_t sequenceNumber;
};

struct VerifyChainResult
{
    VerifyChainResultCode result_code;
    uint64_t index;
};
```

JNA must model `size_t` using a native-size integer. Do not map it to
`NativeLong`: C `long` is 32 bits on 64-bit Windows while `size_t` is 64 bits.
One portable mapping is:

```java
public final class SizeT extends IntegerType {
    public SizeT() {
        this(0);
    }

    public SizeT(long value) {
        super(Native.SIZE_T_SIZE, value, true);
    }
}
```

A structure mapping can follow this shape:

```java
@Structure.FieldOrder({
    "canonicalData",
    "canonicalDataLength",
    "signature",
    "previousHash",
    "currentHash",
    "sequenceNumber"
})
public class AuditEntry extends Structure {
    public Pointer canonicalData;
    public SizeT canonicalDataLength;
    public byte[] signature = new byte[64];
    public byte[] previousHash = new byte[32];
    public byte[] currentHash = new byte[32];
    public long sequenceNumber;
}

@Structure.FieldOrder({"resultCode", "index"})
public class VerifyChainResult extends Structure
        implements Structure.ByValue {
    public int resultCode;
    public long index;
}
```

Java `long` carries the 64-bit bit pattern used by native `uint64_t`. Current
sequence numbers and indexes are non-negative and fit within signed Java
`long` values.

The canonical-data memory referenced by each `Pointer` must stay alive until
the native call returns. Entries must be stored as one contiguous JNA structure
array in the exact field order above. Validate JNA structure sizes and offsets
on both Windows x86-64 and Linux x86-64 in Java integration tests.

## Suggested JNA interface

```java
public interface ResursAuditLibrary extends Library {
    ResursAuditLibrary INSTANCE =
            Native.load("resurs_audit", ResursAuditLibrary.class);

    void generate_private_key(byte[] outputPrivateKey);

    int get_public_key(
            byte[] privateKey,
            byte[] outputPublicKey);

    int wrapper_hash_and_sign(
            byte[] canonicalData,
            SizeT canonicalDataLength,
            byte[] privateKey,
            SizeT privateKeyLength,
            byte[] outputHash,
            byte[] outputSignature);

    VerifyChainResult wrapper_verify_chain(
            AuditEntry[] entries,
            SizeT entryCount,
            byte[] publicKey,
            SizeT publicKeyLength);
}
```

This is a suggested mapping, not Java-owned production code. The Java team
owns the final interface, memory-lifetime handling, error mapping, and
integration tests.

## Creating and verifying an entry

1. Allocate a 32-byte private-key output and call `generate_private_key` when
   creating development/test key material.
2. Allocate a 32-byte public-key output and call `get_public_key`.
3. Create the complete unsigned audit JSON, including `sequenceNumber`,
   `previousHash`, and every application field that the signature must protect.
   Do not include the resulting `currentHash` or `signature` in their own input.
4. Canonicalize that JSON according to RFC 8785 and encode it as UTF-8.
5. Allocate a 32-byte hash and 64-byte signature.
6. Pass the complete canonical byte array, its total byte length, the 32-byte
   private key, and its length to `wrapper_hash_and_sign`.
7. Persist the canonical bytes or all immutable source values required to
   reproduce them byte-for-byte, together with the returned hash, signature,
   separate previous hash, and sequence number.
8. For verification, reconstruct the same canonical bytes and a complete
   ordered `AuditEntry` array for one application.
9. Call `wrapper_verify_chain` and treat every non-zero result as failure.

Production key generation, storage, access control, rotation, and key IDs must
be defined by the application/deployment architecture. Do not persist private
keys with audit entries.

### RFC 8785 and chain ordering are separate

RFC 8785 canonicalizes the JSON representation of each individual entry. It
does not sort entries in the audit chain and does not reorder JSON array
elements. Java must separately query/order the chain by ascending
`sequenceNumber` before constructing the contiguous JNA structure array.

The Java and native teams must freeze the canonical object's exact schema,
field types, UTF-8 encoding, timestamp format, null handling, and binary-field
representation. In particular, `previousHash` must have one defined JSON
representation, and Java must convert that same value to the raw 32-byte
`AuditEntry.previousHash` field.

## Complete-chain requirement

The verifier requires:

- entries for exactly one application;
- the complete chain beginning with sequence number 1;
- ascending order with no duplicate or missing sequence numbers;
- 32 zero bytes in the first entry's `previousHash`;
- every later `previousHash` equal to the preceding `currentHash`.

Pagination or a partial range such as sequence numbers 21 through 40 cannot be
passed directly to the current verifier.

## Verification result handling

| Value | Native name | Java meaning |
|---:|---|---|
| `0` | `VERIFY_CHAIN_ALL_OK` | The supplied complete chain passed. |
| `-1` | `PREVIOUS_HASH_MISSMATCH` | Broken previous-hash link. |
| `-2` | `SEQUENCE_NUMBER_MISSMATCH` | Wrong start, duplicate, gap, or ordering error. |
| `-3` | `CURRENT_HASH_MISSMATCH` | Canonical bytes do not match the stored hash. |
| `-4` | `EVP_DIGEST_VERIFY_INIT_FAILED` | Native verification initialization failed. |
| `-5` | `EVP_DIGEST_VERIFY_FAILED` | Invalid signature. |
| `-6` | `VERIFY_CHAIN_FAILED_TO_CONVERT_PUBLIC_KEY` | Invalid or unusable public key. |

For codes `-1` through `-5`, `index` is the zero-based position of the first
failing entry. Do not read `index` for success or code `-6`.

The misspelled native name `MISSMATCH` is retained for compatibility. Java may
use clearer constant names internally if the numeric values remain identical.

## Security and format limitations

- `currentHash` currently equals `SHA-256(canonicalData)`.
- The signature authenticates `currentHash`.
- When Java includes `sequenceNumber` and `previousHash` in `canonicalData`,
  they are covered by the hash and signature. They are also supplied separately
  in `AuditEntry` for native sequence and chain-link checks.
- Native treats canonical data as opaque bytes and does not compare values
  embedded in JSON with the separate structure fields. Java must derive both
  representations from the same authoritative values.
- Verifying entries `1..N` cannot prove that an entry `N+1` was not removed.
  Detecting tail truncation requires a trusted expected count, final sequence,
  or chain head outside the supplied array.
- Canonicalization is owned by Java and must follow RFC 8785. The exact UTF-8
  result is the signed input.

Do not change canonical serialization, structure layout, field widths, enum
values, or hashing format without versioning the format and coordinating both
native and Java migrations.

## Deployment and compatibility

The Linux library depends on compatible versions of OpenSSL `libcrypto`,
libstdc++, libgcc, and glibc. The selected shared Linux prebuilt is built on
Ubuntu 22.04 for compatibility with Ubuntu 22.04 and newer tested runtimes. A
binary built on Ubuntu 24.04 must not be assumed to load on Ubuntu 22.04.

A 64-bit JVM requires an x86-64 native library. The application should run a
smoke test that loads the exact packaged prebuilt and calls the API on every
supported operating system; native C++ tests alone do not prove the JNA
structure mapping and resource packaging.

## Ownership

The native team provides:

- C++ source and CMake configuration;
- the exported ABI and numeric result codes;
- Linux and Windows x86-64 prebuilt libraries;
- native behavior tests and export verification.

The Java/deployment teams provide:

- the JNA dependency and final Java mappings;
- deterministic canonical serialization;
- native-resource packaging and extraction;
- key storage, selection, rotation, and key-ID persistence;
- end-to-end JNA load, signing, storage, and verification tests.
