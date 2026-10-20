# `resurs_audit` native module

`resurs_audit` is a C++17 shared library for hashing, Ed25519 signing, and
verification of ordered audit chains. It uses OpenSSL and is built as:

| Platform | Library |
|---|---|
| Linux x86-64 | `libresurs_audit.so` |
| Windows x86-64 | `resurs_audit.dll` |

Java/JNA consumers should start with [JNA_INTEGRATION.md](JNA_INTEGRATION.md).
The authoritative exported API is declared in [wrapper.h](wrapper.h).

## Public native ABI

Exactly four functions are supported and exported:

- `generate_private_key`
- `get_public_key`
- `wrapper_hash_and_sign`
- `wrapper_verify_chain`

All other classes and functions are implementation details. In particular,
`wrapper_hash` and `wrapper_sign` are internal functions in an anonymous
namespace and are deliberately not exported. The CMake target hides symbols by
default and `RESURS_AUDIT_API` explicitly exports the four supported functions.

The functions have C linkage for stable symbol names. The current headers and
data definitions use C++ constructs, so direct native consumers must compile
as C++17; the headers are not currently C-source compatible.

| Function | Purpose | Success result |
|---|---|---:|
| `generate_private_key(output, output_length)` | Generate a raw 32-byte Ed25519 private key. | `0` |
| `get_public_key(private_key, private_key_length, output, output_length)` | Derive a raw 32-byte Ed25519 public key. | `0` |
| `wrapper_hash_and_sign(canonical_data, canonical_data_length, private_key, private_key_length, hash_output, hash_output_length, signature_output, signature_output_length)` | SHA-256 hash canonical bytes and sign that hash with Ed25519. | `0` |
| `wrapper_verify_chain(entries, entry_count, public_key, public_key_length)` | Validate an ordered complete audit chain and every signature. | `VerifyChainResult{0, 0}` |

All four exported functions are declared `noexcept` and contain their own
validation and exception handling. Expected failures are returned as result
codes rather than allowing a C++ exception to cross the native/JNA boundary.
Callers must treat outputs as invalid after any non-zero result.

## Audit entry contract

The verifier receives a contiguous, ordered array of entries:

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
```

- `canonicalData` is the RFC 8785-canonicalized, UTF-8 encoded JSON byte
  sequence supplied by Java. Native performs no JSON parsing, serialization,
  or normalization.
- `signature` is a 64-byte Ed25519 signature of `currentHash`.
- `previousHash` is all zeroes for entry 1 and otherwise equals the preceding
  entry's `currentHash`.
- `currentHash` is the 32-byte value `SHA-256(canonicalData)`.
- `sequenceNumber` must start at 1 and equal the entry's one-based position.

`wrapper_verify_chain` therefore expects a complete chain for one application,
sorted by ascending sequence number. A page or partial chain beginning at a
number other than 1 will fail verification.

For every entry, verification checks in this order:

1. The previous-hash link.
2. `sequenceNumber == array index + 1`.
3. The SHA-256 hash of the canonical bytes.
4. The Ed25519 signature of `currentHash`.

Verification stops on the first failure.

### Canonical-data contract

Java owns construction of the audit JSON. Before signing, the complete
unsigned audit entry is canonicalized according to RFC 8785 and encoded as
UTF-8. The canonical object must include the same `sequenceNumber` and
`previousHash` values that Java also supplies as separate `AuditEntry` fields.
It must also include every application field that should be protected by the
signature.

`currentHash` and `signature` must not be included in their own canonical
input because they are outputs of the operation. A format/schema version and
signing-key identifier should be included when those values are part of the
persisted audit model.

RFC 8785 canonicalizes one entry's JSON representation; it does not sort the
audit chain. Java must separately order the `AuditEntry` array by ascending
`sequenceNumber` before verification.

Native treats `canonicalData` as opaque bytes:

```text
currentHash = SHA-256(canonicalData)
signature   = Ed25519-sign(currentHash)
```

Consequently, `sequenceNumber` and `previousHash` are cryptographically
protected when Java includes them in `canonicalData`. Native additionally
checks their separate `AuditEntry` representations for chain structure. Native
does not parse the JSON to compare the embedded values with the separate
fields, so Java must derive both representations from the same authoritative
values.

The shared Java/native contract must freeze the exact canonical object schema,
field types, timestamp representation, null handling, and representation of
binary values such as `previousHash`. Changing that contract changes hashes and
signatures for otherwise equivalent entries and therefore requires format
versioning.

### Hash-and-sign buffers

`wrapper_hash_and_sign` receives the complete canonical byte buffer and its
total byte length, not each JSON field separately. Java must provide a 32-byte
private key, a writable 32-byte hash output, and a writable 64-byte signature
output. On success, Java persists the resulting hash and signature together
with the values needed to reproduce the exact canonical entry.

The private-key length must be exactly 32 bytes, the hash output length exactly
32 bytes, and the signature output length exactly 64 bytes. All pointers must
be non-null and canonical data must contain at least one byte. The hash can be
written before a subsequent signing failure, so neither output may be consumed
unless the complete operation returns `HASH_AND_SIGN_ALL_OK`.

## Public result codes

### Private-key generation

| Value | Name | Meaning |
|---:|---|---|
| `0` | `AUDIT_PRIVATE_KEY_OK` | A private key was generated. |
| `-1` | `AUDIT_INVALID_ARGUMENT` | The output pointer is null. |
| `-2` | `AUDIT_PRIVATE_KEY_BUFFER_TOO_SMALL` | The output buffer is smaller than 32 bytes. |
| `-3` | `AUDIT_PRIVATE_KEY_INTERNAL_ERROR` | Native key generation failed internally. |

### Public-key derivation

| Value | Name | Meaning |
|---:|---|---|
| `0` | `AUDIT_KEY_OK` | A public key was derived. |
| `-1` | `AUDIT_KEY_NULL_ARGUMENT` | A required pointer is null. |
| `-2` | `AUDIT_KEY_INVALID_PRIVATE_KEY_SIZE` | The private-key length is not exactly 32 bytes. |
| `-3` | `AUDIT_KEY_OUTPUT_BUFFER_TOO_SMALL` | The output buffer is smaller than 32 bytes. |
| `-4` | `AUDIT_KEY_CREATION_FAILED` | OpenSSL could not create an Ed25519 key from the supplied bytes. |
| `-5` | `AUDIT_KEY_INTERNAL_ERROR` | Public-key derivation failed internally. |

### Hashing and signing

| Value | Name | Meaning |
|---:|---|---|
| `0` | `HASH_AND_SIGN_ALL_OK` | Both outputs are valid. |
| `-1` | `HASH_AND_SIGN_INVALID_ARGUMENT` | A required pointer is null. |
| `-2` | `HASH_AND_SIGN_CANONICAL_DATA_EMPTY` | Canonical-data length is zero. |
| `-3` | `HASH_AND_SIGN_INVALID_BUFFER_SIZE` | A key or output-buffer length is invalid. |
| `-4` | `HASH_AND_SIGN_HASHING_FAILED` | Hashing failed. |
| `-5` | `HASH_AND_SIGN_SIGNING_INIT_FAILED` | OpenSSL signing initialization failed. |
| `-6` | `HASH_AND_SIGN_SIGNING_SIGNATURE_LENGTH_FAILED` | OpenSSL could not determine the signature length. |
| `-7` | `HASH_AND_SIGN_SIGNING_EVP_DIGEST_SIGNING_FAILED` | OpenSSL signing failed. |
| `-8` | `HASH_AND_SIGN_SIGNING_FAILED_TO_CONVERT_TO_PRIVATE_KEY` | The private key could not be converted. |
| `-9` | `HASH_AND_SIGN_SIGNING_INTERNAL_ERROR` | Signing returned an unexpected result. |
| `-10` | `HASH_AND_SIGN_INTERNAL_ERROR` | An exception or other internal failure was contained. |

### Chain verification

| Value | Name | Meaning | `index` valid? |
|---:|---|---|---|
| `0` | `VERIFY_CHAIN_ALL_OK` | Every entry passed verification. | No |
| `-1` | `VERIFY_CHAIN_INVALID_ARGUMENT` | The entries or public-key pointer is null. | No |
| `-2` | `VERIFY_CHAIN_ENTRY_COUNT_EMPTY` | The entry count is zero. | No |
| `-3` | `VERIFY_CHAIN_INVALID_PUBLIC_KEY_SIZE` | The public-key length is not exactly 32 bytes. | No |
| `-4` | `VERIFY_CHAIN_CANONICAL_DATA_EMPTY` | An entry has a null canonical-data pointer. | Yes |
| `-5` | `VERIFY_CHAIN_CANONICAL_DATA_LENGTH_EMPTY` | An entry has a zero canonical-data length. | Yes |
| `-6` | `PREVIOUS_HASH_MISSMATCH` | The previous-hash link is invalid. | Yes |
| `-7` | `SEQUENCE_NUMBER_MISSMATCH` | The sequence is not `1, 2, 3, ...`. | Yes |
| `-8` | `CURRENT_HASH_MISSMATCH` | Canonical data does not match `currentHash`. | Yes |
| `-9` | `EVP_DIGEST_VERIFY_INIT_FAILED` | OpenSSL could not initialize verification. | Yes |
| `-10` | `EVP_DIGEST_VERIFY_FAILED` | The Ed25519 signature is invalid or verification failed. | Yes |
| `-11` | `VERIFY_CHAIN_FAILED_TO_CONVERT_PUBLIC_KEY` | The public key could not be converted. | No |
| `-12` | `VERIFY_CHAIN_RESULT_INTERNAL_ERROR` | An exception or other internal failure was contained. | No |

The `MISSMATCH` spelling is part of the current native interface. Coordinate
with consumers before renaming it.

`VerifyChainResult.index` is zero-based and identifies a position in the
supplied array, not a sequence number. Read it only for entry-specific failures
`-4` through `-10`. Native initializes it to zero for all other results, but
that zero does not identify a failing entry.

## Key and output sizes

| Value | Size |
|---|---:|
| Raw Ed25519 private key | 32 bytes |
| Raw Ed25519 public key | 32 bytes |
| SHA-256 hash | 32 bytes |
| Ed25519 signature | 64 bytes |

All values are binary buffers, not null-terminated strings. The caller owns
every input and output buffer and must allocate the required size.

## Build and test

Run tests in a Debug build because the current tests use C++ `assert`, which is
disabled by `NDEBUG` in Release builds:

From the repository root, the supported Docker test script builds and tests all
native modules on both Ubuntu 24.04 and Ubuntu 22.04:

```bash
bash native/test-linux.sh
```

For one local Linux build instead:

```bash
cmake -S native -B build/audit-debug \
  -DCMAKE_BUILD_TYPE=Debug \
  -DBUILD_TESTING=ON \
  -DRESURS_BUILD_AUDIT_DEMO=ON
cmake --build build/audit-debug \
  --target resurs_audit resurs_audit_tests resurs_audit_demo \
  --parallel
ctest --test-dir build/audit-debug \
  -R resurs_audit_tests \
  --output-on-failure
```

Build distribution binaries separately with `CMAKE_BUILD_TYPE=Release` after
the Debug tests pass. A direct CMake distribution build may use
`BUILD_TESTING=OFF`; the Dockerfile keeps tests enabled while producing the
library. Linux builds have been verified in Ubuntu 22.04 and 24.04 containers,
and Windows builds with MSVC x64.

When one Linux binary must run on both supported Ubuntu releases, build the
distribution `.so` in Ubuntu 22.04:

```bash
docker build --no-cache \
  --build-arg UBUNTU_VERSION=22.04 \
  --build-arg CMAKE_BUILD_TYPE=Release \
  -t resurs-audit-release-ubuntu-22 \
  ./native
container_id="$(docker create resurs-audit-release-ubuntu-22)"
docker cp \
  "${container_id}:/resurs-build/libresurs_audit.so" \
  native/resurs_audit/prebuilt/linux-x86-64/libresurs_audit.so
docker rm "${container_id}"
```

Build the Windows x86-64 DLL from an x64 Visual Studio developer prompt, using
the Visual Studio generator installed on that machine and the vcpkg toolchain.
Run the Debug tests before copying the Release DLL into `prebuilt/`.

Inspect the Linux exports with:

```bash
nm -D --defined-only libresurs_audit.so
```

Inspect the Windows exports from an x64 Native Tools prompt with:

```text
dumpbin /exports resurs_audit.dll
```

Only the four public ABI functions listed above should be exported.

## Prebuilt libraries

Selected distribution binaries live under:

```text
prebuilt/
├── linux-x86-64/libresurs_audit.so
└── win32-x86-64/resurs_audit.dll
```

The shared Linux prebuilt should be produced on Ubuntu 22.04 when one binary
must support both Ubuntu 22.04 and 24.04. A library built on a newer Linux
release must not be assumed to work on an older runtime. Platform-specific
files in `artifacts/` are intermediate build outputs and are not distribution
files.

The prebuilt files are part of the public integration contract. Rebuild and
replace both platform files after changing any exported parameter, result type,
enum value, structure field, or structure layout. Update the JNA declarations
in the same change. Mixing an old binary with new JNA declarations can pass
arguments in the wrong positions and can crash the process.

## C/C++ implementation details (native team only)

This section is for native C/C++ development; Java/JNA consumers do not call
these implementation types directly.

- `DigitalSign` in `resurs_audit.h/.cpp` owns the OpenSSL implementation.
- `wrapper.cpp` translates the public buffer-based ABI to `DigitalSign`.
- Internal `wrapper_hash` and `wrapper_sign` helpers have internal linkage.
- The tests call only the four public functions so they exercise the same
  boundary used by JNA.
- Known-answer tests use the RFC 8032 test key and a deterministic
  SHA-256-then-Ed25519 result for canonical bytes `abc`.
- Tests cover modified data, hash, signature, previous hash, invalid sequence
  start, sequence gaps, and valid multi-entry chains.

Do not expose `DigitalSign`, OpenSSL objects, STL containers, or exceptions as
part of the supported JNA boundary. Changes to `AuditEntry`,
`VerifyChainResult`, enum values, field order, or field widths are ABI changes
and must be coordinated with the Java mapping and rebuilt prebuilt libraries.

## Operational responsibilities

- Build the complete unsigned audit JSON with `sequenceNumber` and
  `previousHash`, canonicalize it with RFC 8785, and pass its UTF-8 bytes and
  total byte length to native.
- Persist the exact canonical bytes, or persist sufficient immutable values to
  reproduce byte-for-byte identical RFC 8785 output during verification.
- Keep signing private keys outside the application database, preferably in a
  secrets manager or KMS-backed solution.
- Persist a signing-key identifier alongside audit data when key rotation is
  introduced; `AuditEntry` currently contains no key ID.
- Treat every non-zero verification result as invalid or unverifiable.
- A valid prefix does not prove that no entries were removed from the end. A
  trusted expected final sequence number, entry count, or chain head is needed
  to detect tail truncation.
