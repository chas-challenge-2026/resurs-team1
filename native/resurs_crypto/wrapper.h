#include <stdint.h>
#include "resurs_crypto.h"
#include <cstring>

int aes_256_gcm_encrypt(const uint8_t *plaintext, int plaintext_len, uint8_t *iv, int iv_len, uint8_t *ciphertext, int ciphertext_len, unsigned char *tag, int tag_len);

int aes_256_gcm_decrypt(const uint8_t *ciphertext, int ciphertext_len, const uint8_t *iv, int iv_len, uint8_t *plaintext, int plaintext_len, const unsigned char *tag, int tag_len);
