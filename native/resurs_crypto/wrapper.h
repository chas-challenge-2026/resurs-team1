#ifndef RESURS_CRYPTO_WRAPPER_H
#define RESURS_CRYPTO_WRAPPER_H
#include <stdint.h>
#include "crypto_types.h"

#ifdef __cplusplus
extern "C" {
#endif

int aes_256_gcm_encrypt(const uint8_t *plaintext, int plaintext_len, uint8_t *iv, int iv_len, uint8_t *ciphertext, int ciphertext_len, unsigned char *tag, int tag_len);

int aes_256_gcm_decrypt(const uint8_t *ciphertext, int ciphertext_len, const uint8_t *iv, int iv_len, uint8_t *plaintext, int plaintext_len, const unsigned char *tag, int tag_len);

#ifdef __cplusplus
}
#endif

#endif