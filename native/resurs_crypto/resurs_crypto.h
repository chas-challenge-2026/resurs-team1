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

#include "crypto_types.h"

struct EncryptionResult
{
    std::array<unsigned char, resurs::crypto::GCM_TAG_SIZE_BYTES> tag;
    std::array<unsigned char, resurs::crypto::GCM_IV_SIZE_BYTES> iv;
    std::vector<unsigned char> ciphertext;
};


class AuthenticationError : public std::runtime_error
{
    public:
        AuthenticationError() : std::runtime_error("authentication failed") {}
};


struct DecryptionResult
{
    std::string plaintext;
};

class AES256_Encryption
{
public:
    AES256_Encryption() : ctx(EVP_CIPHER_CTX_new(), EVP_CIPHER_CTX_free), cipher(EVP_CIPHER_fetch(nullptr, "AES-256-GCM", nullptr), EVP_CIPHER_free)
    {
        if (ctx == nullptr)
        {
            throw std::runtime_error("failed to create cipher context");
        }

        if (cipher == nullptr)
        {
            throw std::runtime_error("failed to fetch aes-256-gcm");
        }
    };

    EncryptionResult AES256_Encrypt(std::string& plaintext, const unsigned char *aes_key);

    std::string AES256_Decrypt(const std::vector<unsigned char> &ciphertext, const unsigned char *aes_key, const std::array<unsigned char, resurs::crypto::GCM_IV_SIZE_BYTES> &iv, const std::array<unsigned char, resurs::crypto::GCM_TAG_SIZE_BYTES> &tag);

private:
    // CipherCtxPtr becomes an alias of the whole thing after, which is a unique pointer of a sepcific type(EVP_CIPHER_CTX) and a custom delete, the "&EVP_CIPHER_CTX_free"-part
    using CipherCtxPtr = std::unique_ptr<EVP_CIPHER_CTX, decltype(&EVP_CIPHER_CTX_free)>;
    using CipherPtr = std::unique_ptr<EVP_CIPHER, decltype(&EVP_CIPHER_free)>;
    CipherCtxPtr ctx;
    CipherPtr cipher;
};

#endif