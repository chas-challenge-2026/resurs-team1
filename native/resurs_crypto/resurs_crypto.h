#ifndef RESURS_CRYPTO_H
#define RESURS_CRYPTO_H
#include <iostream>
#include <stdint.h>
#include <openssl/evp.h>
#include <openssl/rand.h>
#include <array>
#include <vector>
#include <climits>
#include <stdexcept>
#include <memory>

struct EncryptionResult
{
    std::array<unsigned char, 16> tag;
    std::array<unsigned char, 12> iv;
    std::vector<unsigned char> ciphertext;
};

class AES256_Encryption
{
public:
    AES256_Encryption(const std::string &_plaintext) : plaintext(_plaintext), ctx(EVP_CIPHER_CTX_new(), EVP_CIPHER_CTX_free), cipher(nullptr, EVP_CIPHER_free)
    {
        if (ctx == nullptr)
        {
            throw std::runtime_error("failed to create cipher context");
        }
    };

    EncryptionResult AES256_Encrypt();

    std::string AES256_Decrypt(const std::string &ciphertext, const std::string &key, const uint8_t iv, const std::string &plaintext);

private:
    std::array<unsigned char, 32> key =
        {0x52, 0x86, 0x5A, 0x9C, 0x22, 0xEE, 0x88, 0xE5,
         0x03, 0x25, 0x6B, 0x6D, 0x04, 0x01, 0x21, 0x6B,
         0xDE, 0xD4, 0x06, 0xA1, 0xFD, 0x88, 0x61, 0x6C,
         0x1A, 0x7A, 0x77, 0x92, 0x18, 0x76, 0xCF, 0x9C};
    const std::string &plaintext;
    // std::unique_ptr<EVP_CIPHER_CTX> ctx;
    // std::unique_ptr<EVP_CIPHER> cipher;
    using CipherCtxPtr = std::unique_ptr<EVP_CIPHER_CTX, decltype(&EVP_CIPHER_CTX_free)>;
    using CipherPtr = std::unique_ptr<EVP_CIPHER, decltype(&EVP_CIPHER_free)>;
    CipherCtxPtr ctx;
    CipherPtr cipher;
};

#endif
