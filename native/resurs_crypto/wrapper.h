#include <stdint.h>
#include "resurs_crypto.h"
#include <cstring>

int aes_256_gcm_encrypt(const uint8_t* plaintext, int plaintext_len, uint8_t* iv, uint8_t* ciphertext, int ciphertext_len, unsigned char *tag);

int aes_256_gcm_decrypt(const uint8_t* ciphertext, int ciphertext_len, const uint8_t* key, const uint8_t iv, const uint8_t* plaintext, int plaintext_len, unsigned char *tag);