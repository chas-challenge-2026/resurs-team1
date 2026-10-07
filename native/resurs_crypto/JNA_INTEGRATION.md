# `resurs_crypto` JNA integration

This document is the integration handoff from the native team to the Java and
deployment teams. The authoritative native API contract is
[`wrapper.h`](wrapper.h). General native build instructions are available in
[`../DOCKER.md`](../DOCKER.md).

## Library names

| Platform | Library file | JNA load name |
|---|---|---|
| Linux | `libresurs_crypto.so` | `resurs_crypto` |
| Windows | `resurs_crypto.dll` | `resurs_crypto` |

JNA adds the platform-specific prefix and extension. Java should therefore
load the library without `lib`, `.so`, or `.dll`:

```java
Native.load("resurs_crypto", ResursCryptoLibrary.class);
```

## Exported functions

The supported C ABI consists of exactly these two functions:

```c
int aes_256_gcm_encrypt(
    const uint8_t *plaintext,
    int plaintext_len,
    const unsigned char *aes_key,
    int aes_key_len,
    uint8_t *iv,
    int iv_len,
    uint8_t *ciphertext,
    int ciphertext_len,
    unsigned char *tag,
    int tag_len);

int aes_256_gcm_decrypt(
    const uint8_t *ciphertext,
    int ciphertext_len,
    const unsigned char *aes_key,
    int aes_key_len,
    const uint8_t *iv,
    int iv_len,
    uint8_t *plaintext,
    int plaintext_len,
    const unsigned char *tag,
    int tag_len);
```

All data is binary. Do not pass keys, IVs, tags, plaintext, or ciphertext as
null-terminated strings.

## Required sizes

| Value | Required size |
|---|---:|
| AES-256 key | 32 bytes |
| GCM IV | 12 bytes |
| GCM authentication tag | 16 bytes |
| Ciphertext output | At least `plaintext_len` bytes |
| Plaintext output | At least `ciphertext_len` bytes |

AES-GCM does not change the data length. Encryption generates a fresh IV and
writes it to the supplied 12-byte IV buffer. The caller must persist the IV and
authentication tag together with the ciphertext.

Empty plaintext and ciphertext are currently unsupported. All pointers must be
non-null, including when a length is zero.

## Status codes

| Value | Native name | Meaning |
|---:|---|---|
| `0` | `CRYPTO_OK` | Operation completed successfully. |
| `-1` | `CRYPTO_INVALID_ARGUMENT` | A pointer, length, or key length is invalid. |
| `-2` | `CRYPTO_INVALID_BUFFER_SIZE` | An output buffer is too small or an IV/tag length is not exact. |
| `-3` | `CRYPTO_AUTHENTICATION_FAILED` | The key, IV, tag, or ciphertext did not authenticate. |
| `-4` | `CRYPTO_INTERNAL_ERROR` | The native crypto operation failed internally. |

Output buffers are valid only when the function returns `CRYPTO_OK`. In
particular, Java must not use the plaintext output after authentication fails. 

## Suggested JNA interface

```java
import com.sun.jna.Library;
import com.sun.jna.Native;

public interface ResursCryptoLibrary extends Library {
    ResursCryptoLibrary INSTANCE =
        Native.load("resurs_crypto", ResursCryptoLibrary.class);

    int aes_256_gcm_encrypt(
        byte[] plaintext,
        int plaintextLength,
        byte[] aesKey,
        int aesKeyLength,
        byte[] iv,
        int ivLength,
        byte[] ciphertext,
        int ciphertextLength,
        byte[] tag,
        int tagLength);

    int aes_256_gcm_decrypt(
        byte[] ciphertext,
        int ciphertextLength,
        byte[] aesKey,
        int aesKeyLength,
        byte[] iv,
        int ivLength,
        byte[] plaintext,
        int plaintextLength,
        byte[] tag,
        int tagLength);
}
```

The Java team owns the final interface, wrapper service, exception mapping,
key lifecycle, and integration tests.

## Linux build commands

Run from the repository root.

Ubuntu 24.04:

```bash
docker build --build-arg UBUNTU_VERSION=24.04 -t resurs-crypto-ubuntu-24 ./native
```

Ubuntu 22.04/Jammy:

```bash
docker build --build-arg UBUNTU_VERSION=22.04 -t resurs-crypto-jammy ./native
```

Both builds run the crypto behavior tests and exported-symbol test. The Linux
library is created at:

```text
/resurs-build/libresurs_crypto.so
```

The `.so` distributed to an application should be built on the same Ubuntu
release as the application runtime, or on an older compatible release. A
library built on Ubuntu 24.04 must not be assumed to work on Ubuntu 22.04.

The Linux build depends on compatible versions of:

- `libcrypto.so.3`;
- `libstdc++.so.6`;
- `libgcc_s.so.1`;
- glibc.

## Windows build requirements

The C ABI uses `__declspec(dllexport)` on Windows, and the symbol test uses
`LoadLibrary` and `GetProcAddress`. A native Windows build requires:

- an x64 C++ compiler, such as MSVC;
- CMake;
- OpenSSL development libraries and runtime DLLs for the same architecture.

A 64-bit JVM requires a 64-bit `resurs_crypto.dll` and 64-bit OpenSSL runtime.
The required OpenSSL `libcrypto` DLL must be beside the native library or
available through the Windows DLL search path.

The Linux `.so` builds have been verified on Ubuntu 22.04 and 24.04. The
Windows export path is implemented, but a complete native Windows build and
load test must pass before the DLL is considered verified.

## Runtime placement

The calling application is responsible for placing the correct library in its
runtime environment and making it discoverable by JNA. Common options include:

- set `-Djna.library.path=<native-library-directory>`;
- place the library in an operating-system library search directory;
- package and extract the platform-specific library before calling
  `Native.load`.

Do not commit generated `.so`, `.dll`, object, or CMake build files to Git.

## Ownership

The native team provides:

- the C/C++ source and CMake target;
- the stable C ABI in `wrapper.h`;
- platform export declarations;
- crypto behavior tests and exported-symbol tests;
- reproducible Linux build commands.

The Java and deployment teams provide:

- the JNA dependency and Java interface;
- native-library placement and JNA search-path configuration;
- key storage, selection, and rotation;
- Java-to-JNA integration tests;
- packaging of the correct native library for each runtime platform.
