# resurs_audit native module

`resurs_audit` is a C++17 shared library for detecting modification or
reordering of audit entries. It uses OpenSSL for SHA-256 hashing and Ed25519
signing and verification.

The current native wrapper interface is declared in `wrapper.h`. A Java/JNA
binding has not been added yet.

## Audit entry format

The `AuditEntry` structure is declared in `audit_types.h` and contains:

- canonical event data and its length;
- a 64-byte Ed25519 signature;
- the previous entry's 32-byte SHA-256 hash;
- the current entry's 32-byte SHA-256 hash;
- a sequence number.

The first entry uses an all-zero previous hash. Every later entry must contain
the preceding entry's current hash. The caller is responsible for supplying
the entries in their intended order.

## Wrapper functions

The current wrapper exposes these operations:

- `generate_private_key` generates a 32-byte Ed25519 private key.
- `get_public_key` derives the 32-byte public key from a private key.
- `wrapper_hash_and_sign` hashes canonical data with SHA-256 and signs the
  resulting 32-byte hash.
- `wrapper_verify_chain` verifies the hash link, current hash, and signature
  for every entry in a chain.

`wrapper_verify_chain` returns a `VerifyChainResult`. A successful result uses
`VERIFY_CHAIN_ALL_OK`. On failure, `index` identifies the first invalid entry
for chain, hash, or signature errors.

## Build and test

From the repository root:

```bash
cmake -S native -B native/build \
  -DCMAKE_BUILD_TYPE=Release \
  -DBUILD_TESTING=ON \
  -DRESURS_BUILD_AUDIT_DEMO=ON
cmake --build native/build \
  --target resurs_audit resurs_audit_demo resurs_audit_tests \
  --parallel
ctest --test-dir native/build --output-on-failure -R resurs_audit
```

On Linux, the shared library is named `libresurs_audit.so`. On Windows, it is
named `resurs_audit.dll`.

### Docker

Docker builds both native modules, their demos, and their tests. Run these
commands from the repository root:

```bash
docker build -t resurs-native ./native
docker run --rm resurs-native
```

The default container command runs `resurs_audit_demo`. Run only the audit test
executable with:

```bash
docker run --rm resurs-native /resurs-build/resurs_audit_tests
```

See `native/DOCKER.md` for the complete Docker workflow.

## Key handling

- Keep the private signing key outside the application database, for example
  in a secrets manager or KMS-backed solution.
- Distribute only the public key to services or auditors that verify chains.
- Do not use the keys printed by the demo as production credentials.

## Current status

- [x] SHA-256 hashing
- [x] Ed25519 key generation and public-key derivation
- [x] Ed25519 signing
- [x] Audit-chain verification with invalid-entry reporting
- [x] Automated audit tests
- [x] Docker build and demo
- [ ] Java/JNA binding
