# `resurs_audit` Java/JNA integration

This is the integration handoff for Java and deployment teams. The native
library exposes a small C-linkage ABI from [wrapper.h](wrapper.h); C++-specific
details are documented separately in [README.md](README.md).

## Implementation summary

Implement the Java integration in this order:

1. Create the unsigned audit-entry object. Include `sequenceNumber`,
   `previousHash`, and every application field that must be protected. Do not
   include `currentHash` or `signature`.
2. Canonicalize the complete object with an RFC 8785 implementation. Sorting
   the JSON properties alone is not sufficient.
3. Convert the canonical result to a UTF-8 `byte[]`. If the canonicalization
   library already returns bytes, use them directly.
4. Call `wrapper_hash_and_sign` with the canonical bytes, private key, output
   buffers, and every corresponding length. Store `currentHash` and `signature`
   only when the returned result is `0`.
5. For the first entry, use 32 zero bytes as `previousHash`. For every later
   entry, use the preceding entry's `currentHash`.
6. Preserve the exact canonical bytes, or preserve immutable source values
   from which Java can reproduce exactly the same bytes during verification.
7. To verify the log, load the complete chain beginning with sequence number 1
   and order it by ascending `sequenceNumber`.
8. Build one contiguous JNA `AuditEntry` array with the canonical bytes and
   stored values, then call `wrapper_verify_chain` with the public key and its
   length.
9. Treat `VerifyChainResult.resultCode == 0` as success and every other value
   as failure. Read `index` only for the entry-specific codes documented below.

Java owns JSON construction, RFC 8785 canonicalization, UTF-8 conversion,
storage, and chain ordering. Native owns hashing, signing, and verification;
it does not parse or modify the JSON. The following sections provide the exact
JNA mappings, buffer sizes, and result codes.

## Important update for existing Java work

The native boundary has changed since the previous version of this document.
If Java development has already started, update the integration as follows:

1. `generate_private_key` now returns a result code and receives the output
   buffer length.
2. `get_public_key` now receives both the private-key length and the public-key
   output-buffer length. It also has detailed result codes.
3. `wrapper_hash_and_sign` now receives the hash and signature output-buffer
   lengths. It also has detailed validation and operation result codes.
4. `wrapper_verify_chain` has the same four parameters, but its numeric result
   codes have changed because validation errors were added before the existing
   verification errors.
5. Every exported function validates its normal pointers and buffer sizes
   before using them. Java must still allocate and retain valid native memory.
6. Output buffers are usable only when the function reports success. Ignore
   all output buffers after any non-zero result.
7. Native exceptions are handled inside the exported functions and are
   translated to result codes; no C++ exception is allowed across JNA.
8. Replace the packaged `.dll` and `.so` together with the Java declarations.
   Old binaries must not be used with the new declarations, or conversely.

The parameter changes can be summarized as:

| Function | Previous contract | Current contract |
|---|---|---|
| `generate_private_key` | Output buffer only; no result | Adds output length and returns `int` |
| `get_public_key` | Private-key and output buffers | Adds both buffer lengths and detailed `int` result |
| `wrapper_hash_and_sign` | No output-buffer lengths | Adds hash and signature buffer lengths and detailed `int` result |
| `wrapper_verify_chain` | Four parameters | Same parameters; expanded and renumbered result codes |

Existing verification constants must be updated as follows:

| Existing meaning | Previous value | Current value |
|---|---:|---:|
| Previous-hash mismatch | `-1` | `-6` |
| Sequence-number mismatch | `-2` | `-7` |
| Current-hash mismatch | `-3` | `-8` |
| Verification initialization failure | `-4` | `-9` |
| Invalid signature/verification failure | `-5` | `-10` |
| Public-key conversion failure | `-6` | `-11` |

Values `-1` through `-5` are now input-validation results, and `-12` is the
new internal verification error. Do not keep the previous numeric mapping.

Native-only safety changes were also made. OpenSSL key objects are now released
through native ownership, verification accepts only OpenSSL's explicit success
result, returned verification indexes are initialized, and the public boundary
contains exceptions. These changes do not add Java work beyond using the new
signatures, lengths, result codes, and rebuilt libraries described above.

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

```cpp
GeneratePrivateKeyResultCode generate_private_key(
    unsigned char *output_buffer,
    size_t output_buffer_length) noexcept;

GetPublicKeyResultCode get_public_key(
    unsigned char *private_key,
    size_t private_key_size,
    unsigned char *output_public_key,
    size_t output_public_key_size) noexcept;

HashAndSignResultCode wrapper_hash_and_sign(
    const uint8_t *canonical_data,
    size_t canonical_data_length,
    const uint8_t *private_key,
    size_t private_key_length,
    uint8_t *output_hash,
    size_t output_hash_length,
    uint8_t *output_signature,
    size_t output_signature_length) noexcept;

VerifyChainResult wrapper_verify_chain(
    const AuditEntry *entries,
    size_t entry_count,
    const uint8_t *public_key,
    size_t public_key_length) noexcept;
```

The named native result types map to a Java/JNA `int`. `noexcept` is a C++
boundary guarantee and is not included in the Java declaration.

`wrapper_hash` and `wrapper_sign` are internal implementation helpers. They are
not exported and must not be declared or called by Java.

### Function results

| Function | Result |
|---|---|
| `generate_private_key` | `0` on success; otherwise a validation or internal-error code. |
| `get_public_key` | `0` on success; otherwise a validation, key-creation, or internal-error code. |
| `wrapper_hash_and_sign` | `0` on success; otherwise a validation, hashing, signing, or internal-error code. |
| `wrapper_verify_chain` | Returns `VerifyChainResult`; see the result-code table below. |

Output buffers are valid only after a successful call. Java must pass every
buffer length explicitly and must not pass null references.

## Required binary sizes

| Buffer | Required size |
|---|---:|
| Private key output/input | 32 bytes |
| Public key output/input | 32 bytes |
| Hash output/current hash/previous hash | 32 bytes |
| Signature output/input | 64 bytes |

All values are binary. Do not use Java strings or assume null termination.

The exact validation rules are:

- `generate_private_key` accepts an output buffer of at least 32 bytes.
- `get_public_key` requires a private-key length of exactly 32 bytes and a
  public-key output buffer of at least 32 bytes.
- `wrapper_hash_and_sign` requires non-empty canonical data, a private-key
  length of exactly 32 bytes, a hash output length of exactly 32 bytes, and a
  signature output length of exactly 64 bytes.
- `wrapper_verify_chain` requires at least one entry, a public-key length of
  exactly 32 bytes, and non-empty canonical data for every entry.

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

    int generate_private_key(
            byte[] outputPrivateKey,
            SizeT outputPrivateKeyLength);

    int get_public_key(
            byte[] privateKey,
            SizeT privateKeyLength,
            byte[] outputPublicKey,
            SizeT outputPublicKeyLength);

    int wrapper_hash_and_sign(
            byte[] canonicalData,
            SizeT canonicalDataLength,
            byte[] privateKey,
            SizeT privateKeyLength,
            byte[] outputHash,
            SizeT outputHashLength,
            byte[] outputSignature,
            SizeT outputSignatureLength);

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

1. Allocate a 32-byte private-key output and call `generate_private_key` with
   the buffer and its length when creating development/test key material.
   Continue only when it returns `AUDIT_PRIVATE_KEY_OK` (`0`).
2. Allocate a 32-byte public-key output and call `get_public_key` with both
   buffers and both lengths. Continue only when it returns `AUDIT_KEY_OK` (`0`).
3. Create the complete unsigned audit JSON, including `sequenceNumber`,
   `previousHash`, and every application field that the signature must protect.
   Do not include the resulting `currentHash` or `signature` in their own input.
4. Canonicalize that JSON according to RFC 8785 and encode it as UTF-8.
5. Allocate a 32-byte hash and 64-byte signature.
6. Pass the complete canonical byte array, its total byte length, the 32-byte
   private key and its length, and both output buffers and their lengths to
   `wrapper_hash_and_sign`. Continue only when it returns
   `HASH_AND_SIGN_ALL_OK` (`0`).
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

## Result-code handling

Java may give these constants Java-style names, but their numeric values must
match the native interface exactly. Treat every non-zero value as failure.

### Private-key generation

| Value | Native name | Java meaning |
|---:|---|---|
| `0` | `AUDIT_PRIVATE_KEY_OK` | A 32-byte private key was generated. |
| `-1` | `AUDIT_INVALID_ARGUMENT` | The output-buffer reference is null. |
| `-2` | `AUDIT_PRIVATE_KEY_BUFFER_TOO_SMALL` | The output buffer is smaller than 32 bytes. |
| `-3` | `AUDIT_PRIVATE_KEY_INTERNAL_ERROR` | Native key generation failed internally. |

### Public-key derivation

| Value | Native name | Java meaning |
|---:|---|---|
| `0` | `AUDIT_KEY_OK` | The 32-byte public key was derived. |
| `-1` | `AUDIT_KEY_NULL_ARGUMENT` | A required buffer reference is null. |
| `-2` | `AUDIT_KEY_INVALID_PRIVATE_KEY_SIZE` | The private-key length is not exactly 32 bytes. |
| `-3` | `AUDIT_KEY_OUTPUT_BUFFER_TOO_SMALL` | The public-key output buffer is smaller than 32 bytes. |
| `-4` | `AUDIT_KEY_CREATION_FAILED` | OpenSSL could not create an Ed25519 key from the private bytes. |
| `-5` | `AUDIT_KEY_INTERNAL_ERROR` | Public-key derivation failed internally. |

### Hashing and signing

| Value | Native name | Java meaning |
|---:|---|---|
| `0` | `HASH_AND_SIGN_ALL_OK` | Hash and signature outputs are valid. |
| `-1` | `HASH_AND_SIGN_INVALID_ARGUMENT` | A required buffer reference is null. |
| `-2` | `HASH_AND_SIGN_CANONICAL_DATA_EMPTY` | The canonical-data length is zero. |
| `-3` | `HASH_AND_SIGN_INVALID_BUFFER_SIZE` | The private key, hash output, or signature output has an invalid length. |
| `-4` | `HASH_AND_SIGN_HASHING_FAILED` | Native hashing failed. |
| `-5` | `HASH_AND_SIGN_SIGNING_INIT_FAILED` | OpenSSL could not initialize signing. |
| `-6` | `HASH_AND_SIGN_SIGNING_SIGNATURE_LENGTH_FAILED` | OpenSSL could not determine the signature length. |
| `-7` | `HASH_AND_SIGN_SIGNING_EVP_DIGEST_SIGNING_FAILED` | OpenSSL could not create the signature. |
| `-8` | `HASH_AND_SIGN_SIGNING_FAILED_TO_CONVERT_TO_PRIVATE_KEY` | The raw private key could not be converted to an OpenSSL Ed25519 key. |
| `-9` | `HASH_AND_SIGN_SIGNING_INTERNAL_ERROR` | Signing returned an unexpected internal result. |
| `-10` | `HASH_AND_SIGN_INTERNAL_ERROR` | An exception or other internal failure was contained by the native boundary. |

The hash may already have been written before a later signing error occurs.
Java must nevertheless ignore both output buffers unless the result is `0`.

### Chain verification

| Value | Native name | Java meaning | `index` valid? |
|---:|---|---|---|
| `0` | `VERIFY_CHAIN_ALL_OK` | The supplied complete chain passed. | No |
| `-1` | `VERIFY_CHAIN_INVALID_ARGUMENT` | The entries or public-key reference is null. | No |
| `-2` | `VERIFY_CHAIN_ENTRY_COUNT_EMPTY` | The entry count is zero. | No |
| `-3` | `VERIFY_CHAIN_INVALID_PUBLIC_KEY_SIZE` | The public-key length is not exactly 32 bytes. | No |
| `-4` | `VERIFY_CHAIN_CANONICAL_DATA_EMPTY` | An entry has a null canonical-data reference. | Yes |
| `-5` | `VERIFY_CHAIN_CANONICAL_DATA_LENGTH_EMPTY` | An entry has a zero canonical-data length. | Yes |
| `-6` | `PREVIOUS_HASH_MISSMATCH` | The previous-hash link is invalid. | Yes |
| `-7` | `SEQUENCE_NUMBER_MISSMATCH` | The chain does not start at 1 or has a duplicate, gap, or ordering error. | Yes |
| `-8` | `CURRENT_HASH_MISSMATCH` | Canonical bytes do not match the stored current hash. | Yes |
| `-9` | `EVP_DIGEST_VERIFY_INIT_FAILED` | OpenSSL could not initialize verification. | Yes |
| `-10` | `EVP_DIGEST_VERIFY_FAILED` | The Ed25519 signature is invalid or verification failed. | Yes |
| `-11` | `VERIFY_CHAIN_FAILED_TO_CONVERT_PUBLIC_KEY` | The public key could not be converted to an OpenSSL Ed25519 key. | No |
| `-12` | `VERIFY_CHAIN_RESULT_INTERNAL_ERROR` | An exception or other internal failure was contained by the native boundary. | No |

When `index` is valid, it is the zero-based position of the first failing entry
in the supplied array. It is not the entry's sequence number. Native sets the
field to zero for non-entry-specific results, but Java must not interpret that
zero as a failing entry.

The misspelled native name `MISSMATCH` is retained for compatibility. Java may
use clearer constant names internally if the numeric values remain identical.

## Error and exception boundary

The exported functions validate their normal pointers and sizes and catch
native exceptions before returning to Java. Validation and internal failures
are reported through the result codes above. Java should convert those codes
to its own exceptions or service-level failures after the native call returns.

This boundary cannot determine whether an arbitrary non-null native pointer is
actually readable or writable. Java must keep every JNA buffer, `Memory`
instance, structure array, and canonical-data allocation alive and unchanged
until the call has completed. Length values must describe the supplied memory
accurately.

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
