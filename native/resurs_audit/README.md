# resurs_audit native API

`resurs_audit` is a C++17 shared native library for creating and verifying
tamper-evident audit chains. It uses SHA-256 for entry hashes and Ed25519 for
digital signatures through OpenSSL.

The current native integration boundary is declared in `wrapper.h`, while the
shared data types and result codes are declared in `audit_types.h`. Unlike the
`resurs_crypto` API, this interface is not yet a stable, JNA-ready C ABI because
some exported signatures still use C++ types. A Java/JNA binding has therefore
not been added yet.

## Wrapper API

The module currently exposes these operations:

| Function | Purpose | Result |
|---|---|---|
| `generate_private_key` | Generates a raw 32-byte Ed25519 private key. | Writes the key to the caller-provided buffer. |
| `get_public_key` | Derives a raw 32-byte public key from a private key. | `0` on success, `-1` if the private key cannot be converted. |
| `wrapper_hash_and_sign` | Calculates SHA-256 over canonical entry data and signs the resulting hash with Ed25519. | `0` on success, otherwise `-1`. |
| `wrapper_verify_chain` | Verifies hash links, entry hashes, and signatures in order. | Returns `VerifyChainResult`. |

`wrapper_hash` and `wrapper_sign` also exist in `wrapper.h`, but are helper
operations used to implement and test `wrapper_hash_and_sign`. New integrations
should use the combined operation unless they specifically need the lower-level
behavior.

## Data contract

The caller supplies an ordered array of `AuditEntry` values:

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

The fields have the following meaning:

- `canonicalData` is the exact byte representation that is hashed. The module
  does not serialize or normalize application data.
- `canonicalDataLength` is the number of bytes to hash; canonical data is not
  treated as a null-terminated string.
- `signature` is the 64-byte Ed25519 signature of `currentHash`.
- `previousHash` is the preceding entry's `currentHash`. It must contain 32
  zero bytes for the first entry.
- `currentHash` is `SHA-256(canonicalData)`.
- `sequenceNumber` belongs to the audit record, but the current verifier relies
  on array order and hash links rather than validating this field separately.

The caller owns all input and output buffers. Key, hash, and signature data
must be handled as binary data rather than strings.

## Chain verification

`wrapper_verify_chain` processes entries from index `0` to `entryCount - 1`.
For each entry it verifies, in order:

1. `previousHash` matches the preceding entry's `currentHash`, or is all zeroes
   for the first entry.
2. `currentHash` matches a fresh SHA-256 hash of `canonicalData`.
3. `signature` is a valid Ed25519 signature of `currentHash` for the supplied
   public key.

Verification stops at the first failure. `VerifyChainResult.index` identifies
that entry when the failure is entry-specific.

### Using `VerifyChainResult`

Always inspect `result_code` before reading `index`:

```cpp
VerifyChainResult result = wrapper_verify_chain(
    entries,
    entryCount,
    publicKey,
    publicKeyLength
);

switch (result.result_code)
{
    case VERIFY_CHAIN_ALL_OK:
        // The complete chain is valid. Do not read result.index.
        break;

    case PREVIOUS_HASH_MISSMATCH:
    case CURRENT_HASH_MISSMATCH:
    case EVP_DIGEST_VERIFY_INIT_FAILED:
    case EVP_DIGEST_VERIFY_FAILED:
        // result.index is the zero-based index of the first invalid entry.
        break;

    case VERIFY_CHAIN_FAILED_TO_CONVERT_PUBLIC_KEY:
        // The public key is invalid. No entry was verified; do not read index.
        break;
}
```

`index` is only defined for entry-specific failures (`-1` through `-4`). Its
value is not defined for success (`0`) or public-key conversion failure (`-5`),
so callers must not read it in those cases. The index is zero-based and refers
to the position in the supplied `entries` array, not to `sequenceNumber`.

### Verification result codes

| Value | Name | Meaning |
|---:|---|---|
| `0` | `VERIFY_CHAIN_ALL_OK` | Every entry passed chain, hash, and signature verification. |
| `-1` | `PREVIOUS_HASH_MISSMATCH` | The entry does not reference the expected previous hash. |
| `-2` | `CURRENT_HASH_MISSMATCH` | The stored hash does not match the canonical data. |
| `-3` | `EVP_DIGEST_VERIFY_INIT_FAILED` | OpenSSL could not initialize Ed25519 verification. |
| `-4` | `EVP_DIGEST_VERIFY_FAILED` | The signature is invalid. |
| `-5` | `VERIFY_CHAIN_FAILED_TO_CONVERT_PUBLIC_KEY` | The supplied public key could not be converted to an OpenSSL key. |

The enum names intentionally match the current implementation, including the
existing `MISSMATCH` spelling, and should not be renamed without coordinating
the consuming interface.

The caller should treat every non-zero code as a failed verification. Codes
`-1` and `-2` normally indicate modified data or a broken chain, while `-4`
indicates that the signature does not match the stored hash and supplied public
key. Codes `-3` and `-5` indicate that verification could not be performed and
should be handled as operational or key-material errors rather than proof of a
specific audit-entry modification.

## Build and test

From the `native` directory:

```bash
cmake -S . -B build \
  -DCMAKE_BUILD_TYPE=Release \
  -DBUILD_TESTING=ON \
  -DRESURS_BUILD_AUDIT_DEMO=ON
cmake --build build \
  --target resurs_audit resurs_audit_demo resurs_audit_tests \
  --parallel
ctest --test-dir build --output-on-failure -R resurs_audit
```

On Linux this produces `libresurs_audit.so`. On Windows it produces
`resurs_audit.dll`.

Build and run only the example with:

```bash
cmake -S . -B build -DRESURS_BUILD_AUDIT_DEMO=ON
cmake --build build --target resurs_audit_demo
./build/resurs_audit_demo
```

The example generates an Ed25519 key pair, hashes and signs one canonical audit
entry, and verifies it through the wrapper API. It prints the generated key
material for demonstration purposes; those keys must never be reused in a real
environment.

### Docker build for Linux

From the repository root:

```bash
docker build -t resurs-native ./native
docker run --rm resurs-native
```

The image contains both `resurs_crypto` and `resurs_audit`, their demos, and
their test executables. Tests run automatically while the image is built, and
the default container command runs `resurs_audit_demo`.

Run the audit tests again without rebuilding:

```bash
docker run --rm resurs-native /resurs-build/resurs_audit_tests
```

See [native/DOCKER.md](../DOCKER.md) for all available Docker commands.

## Standardized Ed25519 verification

The automated tests use the private seed and public key from RFC 8032,
Section 7.1, Test 1. This verifies that the module derives the expected public
key from a standardized Ed25519 test key.

The suite also contains a deterministic known-answer vector for this module's
`SHA-256-then-Ed25519` construction. For the canonical bytes `abc`, it verifies:

- the standard SHA-256 digest
  `ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad`;
- the expected 64-byte Ed25519 signature generated from the RFC 8032 test key;
- successful verification of the complete audit entry;
- detection of modified canonical data, current hash, signature, and previous
  hash.

The remaining tests cover deterministic hashing, different-input hashing,
hash-and-sign behavior, a valid two-entry chain, and detection of a broken
link in a multi-entry chain.

Reference: [RFC 8032, Section 7.1](https://www.rfc-editor.org/rfc/rfc8032.html#section-7.1).

## Calling and storage rules

- Create one deterministic canonical byte representation for each audit event;
  semantically identical data with different byte formatting produces a
  different hash and signature.
- Persist the canonical data, signature, previous hash, current hash, and
  sequence number required to reconstruct each `AuditEntry`.
- Preserve entry order. Removing, inserting, or reordering entries must break
  a hash link when the chain is assembled correctly.
- Treat any non-zero verification result as an invalid or unverifiable chain.
- Keep private signing keys outside the application database, preferably in a
  secrets manager or KMS-backed solution.
- Public keys may be distributed to services or auditors that verify chains.
- Define key rotation and public-key versioning before using more than one
  signing key; the current `AuditEntry` structure does not store a key ID.
