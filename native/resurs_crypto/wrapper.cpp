#include "wrapper.h"



int aes_256_gcm_encrypt(const uint8_t* plaintext, int plaintext_len, uint8_t* iv, uint8_t* ciphertext, int ciphertext_len, unsigned char *tag)
{
    std::string plaintext_(reinterpret_cast<const char*>(plaintext), plaintext_len);
    AES256_Encryption encryption(plaintext_);
    EncryptionResult encryption_result = encryption.AES256_Encrypt();

    memcpy(iv, encryption_result.iv.data(), encryption_result.iv.size());
    memcpy(tag, encryption_result.tag.data(), encryption_result.tag.size());
    memcpy(ciphertext, encryption_result.ciphertext.data(), encryption_result.ciphertext.size());
    return 0;
}

int aes_256_gcm_decrypt(const uint8_t* ciphertext, int ciphertext_len, const uint8_t* key, const uint8_t iv, const uint8_t* plaintext, int plaintext_len, unsigned char *tag)
{

    return 0;
}
