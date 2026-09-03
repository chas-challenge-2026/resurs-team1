#include "wrapper.h"

int aes_256_gcm_encrypt(const uint8_t *plaintext, int plaintext_len, uint8_t *iv, int iv_len, uint8_t *ciphertext, int ciphertext_len, unsigned char *tag, int tag_len)
{
    if (!plaintext || !iv || !ciphertext || !tag)
        return -1;

    std::string plaintext_(reinterpret_cast<const char *>(plaintext), plaintext_len);
    AES256_Encryption encryption;
    EncryptionResult encryption_result = encryption.AES256_Encrypt(plaintext_);

    if (iv_len < static_cast<int>(encryption_result.iv.size()))
        return -2;
    if (ciphertext_len < static_cast<int>(encryption_result.ciphertext.size()))
        return -3;
    if (tag_len < static_cast<int>(encryption_result.tag.size()))
        return -4;

    memcpy(iv, encryption_result.iv.data(), encryption_result.iv.size());
    memcpy(tag, encryption_result.tag.data(), encryption_result.tag.size());
    memcpy(ciphertext, encryption_result.ciphertext.data(), encryption_result.ciphertext.size());
    return 0;
}

int aes_256_gcm_decrypt(const uint8_t *ciphertext, int ciphertext_len, const uint8_t *iv, int iv_len, uint8_t *plaintext, int plaintext_len, const unsigned char *tag, int tag_len)
{
    if (!ciphertext || !iv || !plaintext || !tag)
        return -1;

    if (iv_len < 12)
        return -2;

    if (tag_len < 16)
        return -3;

    std::vector<unsigned char> ciphertext_(ciphertext, ciphertext + ciphertext_len
    );

    std::array<unsigned char, 12> iv_;
    memcpy(iv_.data(), iv, iv_.size());

    std::array<unsigned char, 16> tag_;
    memcpy(tag_.data(), tag, tag_.size());

    AES256_Encryption encryption;

    std::string decrypted = encryption.AES256_Decrypt(ciphertext_, iv_,tag_);

    if (plaintext_len < static_cast<int>(decrypted.size()))
        return -4;

    memcpy(plaintext, decrypted.data(), decrypted.size());

    return 0;
}
