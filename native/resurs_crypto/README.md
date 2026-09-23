# resurs_crypto native API

`resurs_crypto` is a shared native library exposing AES-256-GCM encryption and
decryption through a stable C ABI. It is intended to be loaded from Java through
JNA. The authoritative function contract is documented in `wrapper.h`.

## Public ABI

The library exports exactly these supported entry points:

- `aes_256_gcm_encrypt`
- `aes_256_gcm_decrypt`

Both functions return a `CryptoStatus` value:

| Value | Name | Meaning |
|---:|---|---|
| `0` | `CRYPTO_OK` | Operation completed successfully. |
| `-1` | `CRYPTO_INVALID_ARGUMENT` | A pointer, length, or key length is invalid. |
| `-2` | `CRYPTO_INVALID_BUFFER_SIZE` | A data output buffer is too small, or an IV/tag length is not the required exact size. |
| `-3` | `CRYPTO_AUTHENTICATION_FAILED` | Ciphertext authentication failed. |
| `-4` | `CRYPTO_INTERNAL_ERROR` | The native crypto operation failed internally. |

AES keys must be exactly 32 bytes. `iv_len` must be exactly 12 and `tag_len`
must be exactly 16 for both encryption and decryption; shorter and longer
values are rejected. Encryption generates a new 12-byte IV and a 16-byte
authentication tag. GCM does not change the data length, so the ciphertext
buffer must be at least `plaintext_len` bytes and the plaintext buffer must be
at least `ciphertext_len` bytes.

All pointers must be non-null. Plaintext and ciphertext lengths must be greater
than zero; empty input is rejected with `CRYPTO_INVALID_ARGUMENT`. Callers must
only consume output buffers after `CRYPTO_OK`.

## Build and test

From the `native` directory:

```bash
cmake -S . -B build -DCMAKE_BUILD_TYPE=Release -DBUILD_TESTING=ON
cmake --build build --target resurs_crypto resurs_crypto_tests resurs_crypto_symbol_tests
ctest --test-dir build --output-on-failure -R resurs_crypto
```

On Linux this produces `libresurs_crypto.so`. On Windows it produces
`resurs_crypto.dll`. Internal C++ symbols are hidden on platforms that support
symbol visibility; the two C ABI functions are explicitly exported.

Build and run the example with:

```bash
cmake -S . -B build -DRESURS_BUILD_CRYPTO_DEMO=ON
cmake --build build --target resurs_crypto_demo
./build/resurs_crypto_demo
```

The example encrypts data with a generated IV and then decrypts it through the
same public C API.

## Standardized AES-GCM verification

The automated test suite contains a known-answer test based on Test Case 14 in
McGrew and Viega's *The Galois/Counter Mode of Operation (GCM)* specification,
which was submitted during the NIST standardization work. The test uses:

- a 256-bit all-zero key;
- a 96-bit all-zero IV;
- one 128-bit all-zero plaintext block;
- expected ciphertext `cea7403d4d606b6e074ec5d3baf39d18`;
- expected tag `d0d1c8a799996bf0265b98b5d48ab919`.

The vector is passed through the public `aes_256_gcm_decrypt` function. This
checks compatibility against a standardized result rather than only verifying
that this library can decrypt its own output.

Reference: [GCM specification, Test Case 14](https://csrc.nist.rip/groups/ST/toolkit/BCM/documents/proposedmodes/gcm/gcm-spec.pdf).

The separate symbol test dynamically loads the completed shared library and
checks that both supported C ABI entry points can be resolved by name. It does
not link against `resurs_crypto`, so it tests the same exported-symbol boundary
used by JNA.

## Calling rules

- Treat the key, IV, tag, plaintext, and ciphertext as binary data, not
  null-terminated strings.
- Persist the IV and tag together with the ciphertext; all three are required
  for decryption.
- Never reuse an IV with the same key. The encrypt function generates the IV to
  prevent callers from accidentally reusing one.
- Treat `CRYPTO_AUTHENTICATION_FAILED` as an invalid or tampered encrypted
  value. Do not use the plaintext output buffer after any error.
- Key storage and lifecycle are owned by the calling application and must be
  handled separately from ciphertext storage.
