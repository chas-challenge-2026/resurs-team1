# `resurs_crypto` JNA integration

[Native overview](../README.md) | [Crypto API](README.md)

This document describes the native contract and the implemented Java integration.
The authoritative native API contract is
[`wrapper.h`](wrapper.h). General native build instructions are available in
[`../DOCKER.md`](../DOCKER.md).

## Backend implementation

The backend already implements this bridge:

| Source | Responsibility |
|---|---|
| [NativeCryptographerInterface](../../backend/ResursPortal/src/main/java/se/comerit/resurs/jna/NativeCryptographerInterface.java) | JNA declarations for both exported functions. |
| [JnaConfig](../../backend/ResursPortal/src/main/java/se/comerit/resurs/jna/JnaConfig.java) | Loads `resurs_crypto` and provides the interface as a Spring bean. |
| [JNAEncryptionComponent](../../backend/ResursPortal/src/main/java/se/comerit/resurs/jna/JNAEncryptionComponent.java) | Allocates buffers, calls native, maps errors and packs/unpacks the stored payload. |
| [EncryptionKey](../../backend/ResursPortal/src/main/java/se/comerit/resurs/config/EncryptionKey.java) | Base64-decodes `encryption.cipherKey` and requires a 32-byte AES key. |

The component stores `IV || ciphertext || tag`: 12 IV bytes, the ciphertext,
then 16 tag bytes. This is a Java storage format; the native functions receive
those buffers separately. Text conversion currently uses Java's default
charset in `encode` and `decode`; the native API operates on bytes and does not
select a text encoding.

## Library names

| Platform | Library file | JNA load name |
|---|---|---|
| Linux x86-64 prebuilt | `libresurs_crypto.so` | `resurs_crypto` |
| Windows x86-64 prebuilt | `resurs_crypto.dll` | `resurs_crypto` |

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
Lengths use signed 32-bit `int` values and count bytes, not Java characters.
Pass actual buffer capacities; native cannot determine the size of an allocation
behind a non-null pointer.

## Status codes

| Value | Native name | Meaning |
|---:|---|---|
| `0` | `CRYPTO_OK` | Operation completed successfully. |
| `-1` | `CRYPTO_INVALID_ARGUMENT` | A pointer is null, an input-data length is not positive, another length is negative, or the key length is not 32. |
| `-2` | `CRYPTO_INVALID_BUFFER_SIZE` | An output buffer is too small or an IV/tag length is not exact. |
| `-3` | `CRYPTO_AUTHENTICATION_FAILED` | The key, IV, tag, or ciphertext did not authenticate. |
| `-4` | `CRYPTO_INTERNAL_ERROR` | The native crypto operation failed internally. |

Output buffers are valid only when the function returns `CRYPTO_OK`. In
particular, Java must not use the plaintext output after authentication fails.

## Illustrative JNA interface

The following standalone example uses the same parameter types and order as
`NativeCryptographerInterface`. The backend obtains its instance through
`JnaConfig` rather than the static `INSTANCE` shown here.

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

The Java team owns the production interface, wrapper service, exception mapping,
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

The source requires OpenSSL 3.x. The dynamically linked Linux build depends on
compatible versions of:

- `libcrypto.so.3`;
- `libstdc++.so.6`;
- `libgcc_s.so.1`;
- glibc.

## Windows build requirements

The C ABI uses `__declspec(dllexport)` on Windows, and the symbol test uses
`LoadLibrary` and `GetProcAddress`. A native Windows build requires:

- an x64 C++ compiler, such as MSVC;
- CMake;
- OpenSSL 3.x development libraries for the same architecture.

The provided x86-64 prebuilt requires an x86-64 JVM. It links OpenSSL statically
and requires the x64 MSVC runtime and Windows system libraries. A build linked
against dynamic OpenSSL also requires the matching `libcrypto` DLL beside the
native library or available through the Windows DLL search path. Inspect the
actual DLL with `dumpbin /dependents`.

Native build and behavior/export tests can be run on Linux and Windows. Follow
the [Windows build instructions](../../docs/README.md#windows-with-visual-studio)
for Visual Studio. Native CI automatically covers only the two Ubuntu Debug
builds; Java/JNA loading and application behavior require separate tests.

## Runtime placement

The calling application is responsible for placing the correct library in its
runtime environment and making it discoverable by JNA. The backend packages
libraries under these resource paths:

```text
src/main/resources/linux-x86-64/libresurs_crypto.so
src/main/resources/win32-x86-64/resurs_crypto.dll
```

JNA can extract a library from the matching platform resource directory when
loading from the classpath or JAR. See [JNA's loading implementation](https://github.com/java-native-access/jna/blob/5.19.1/src/com/sun/jna/NativeLibrary.java).
Other placement options include:

- set `-Djna.library.path=<native-library-directory>`;
- place the library in an operating-system library search directory;
- package and extract the platform-specific library before calling
  `Native.load`.

Selected distribution files are intentionally tracked in
`native/resurs_crypto/prebuilt/` and in the backend resource directories. Keep
these copies aligned when replacing a prebuilt. Ordinary object files, CMake
build directories and intermediate outputs are not distribution files. Native
CI does not publish or refresh the tracked binaries.

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
