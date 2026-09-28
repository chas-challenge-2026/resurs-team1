#ifndef RESURS_CRYPTO_WRAPPER_H
#define RESURS_CRYPTO_WRAPPER_H

#if defined(_WIN32)
    #if defined(RESURS_CRYPTO_BUILDING_DLL)
        #define RESURS_CRYPTO_API __declspec(dllexport)
    #else
        #define RESURS_CRYPTO_API __declspec(dllimport)
    #endif
#else
    #define RESURS_CRYPTO_API __attribute__((visibility("default")))
#endif

#include <stdint.h>

enum CryptoStatus
{
    CRYPTO_OK = 0,
    CRYPTO_INVALID_ARGUMENT = -1,
    CRYPTO_INVALID_BUFFER_SIZE = -2,
    CRYPTO_AUTHENTICATION_FAILED = -3,
    CRYPTO_INTERNAL_ERROR = -4
};


#ifdef __cplusplus
extern "C" {
#endif

/**
 * Encrypt plaintext with AES-256-GCM.
 *
 * Contract:
 * - Every pointer must be non-null.
 * - plaintext_len must be greater than zero; empty plaintext is unsupported.
 * - Every buffer length must be non-negative.
 * - aes_key_len must be exactly 32 bytes.
 * - iv_len must be exactly 12 and iv must provide 12 writable bytes.
 * - tag_len must be exactly 16 and tag must provide 16 writable bytes.
 * - ciphertext must provide at least plaintext_len writable bytes.
 * - A fresh random 12-byte IV is generated for every successful call.
 * - GCM ciphertext has the same length as the plaintext.
 * - Output buffers are valid only when CRYPTO_OK is returned.
 *
 * The function returns a CryptoStatus value and never lets a C++ exception
 * cross the C ABI boundary.
 */
RESURS_CRYPTO_API int aes_256_gcm_encrypt(const uint8_t *plaintext, int plaintext_len, const unsigned char *aes_key, int aes_key_len, uint8_t *iv, int iv_len, uint8_t *ciphertext, int ciphertext_len, unsigned char *tag, int tag_len);

/**
 * Authenticate and decrypt AES-256-GCM ciphertext.
 *
 * Contract:
 * - Every pointer must be non-null.
 * - ciphertext_len must be greater than zero; empty ciphertext is unsupported.
 * - Every buffer length must be non-negative.
 * - aes_key_len must be exactly 32 bytes.
 * - iv_len must be exactly 12 and iv must contain the encryption IV.
 * - tag_len must be exactly 16 and tag must contain the authentication tag.
 * - plaintext must provide at least ciphertext_len writable bytes.
 * - GCM plaintext has the same length as the ciphertext.
 * - plaintext is not written when authentication fails.
 * - Output buffers are valid only when CRYPTO_OK is returned.
 *
 * The function returns CRYPTO_AUTHENTICATION_FAILED for an invalid key, IV,
 * tag, or modified ciphertext. No C++ exception crosses the C ABI boundary.
 */
RESURS_CRYPTO_API int aes_256_gcm_decrypt(const uint8_t *ciphertext, int ciphertext_len, const unsigned char *aes_key, int aes_key_len, const uint8_t *iv, int iv_len, uint8_t *plaintext, int plaintext_len, const unsigned char *tag, int tag_len);

#ifdef __cplusplus
}
#endif

#endif
