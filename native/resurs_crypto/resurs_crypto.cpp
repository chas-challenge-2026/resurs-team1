#include "resurs_crypto.h"
#include <openssl/ssl.h>
#include <cstring>

EncryptionResult AES256_Encryption::AES256_Encrypt(std::string &plaintext, const unsigned char* aes_key)
{
    EncryptionResult result;

    // Create 96bit IV
    if (RAND_bytes(result.iv.data(), static_cast<int>(result.iv.size())) != 1)
    {
        throw std::runtime_error("failed to generate IV");
    }

    // The class constructor fetched the algoritm for us, so no need for specific code here.


    // We supply the key and IV directly. The default length of the IV is already set to 12 bytes(96 bits), so no need to explicitly set it.
    if (EVP_EncryptInit_ex2(ctx.get(), cipher.get(), aes_key, result.iv.data(), nullptr) != 1)
    {
        throw std::runtime_error("failed to set key and IV");
    }

    // GCM ciphertext is the same size as plaintext
    result.ciphertext.resize(plaintext.size());

    int written = 0;
    int totalWritten = 0;

    // Now encrypt the plaintext.
    if (!plaintext.empty())
    {
        if (EVP_EncryptUpdate(ctx.get(), result.ciphertext.data(), &written, reinterpret_cast<const unsigned char *>(plaintext.data()), static_cast<int>(plaintext.size())) != 1)
        {
            throw std::runtime_error("encryption failed");
        }

        totalWritten += written;
    }

    // finalize encryption
    if (EVP_EncryptFinal_ex(ctx.get(), result.ciphertext.data() + totalWritten, &written) != 1)
    {
        throw std::runtime_error("encryption finalization failed");
    }

    totalWritten += written;
    result.ciphertext.resize(totalWritten);

    // retrieve GCM authenticaiton tag
    if (EVP_CIPHER_CTX_ctrl(ctx.get(), EVP_CTRL_AEAD_GET_TAG, static_cast<int>(result.tag.size()), result.tag.data()) != 1)
    {
        throw std::runtime_error("failed to retrieve authentication tag");
    }

    return result;
};

std::string AES256_Encryption::AES256_Decrypt(const std::vector<unsigned char> &ciphertext, const unsigned char* aes_key, const std::array<unsigned char, resurs::crypto::GCM_IV_SIZE_BYTES> &iv, const std::array<unsigned char, resurs::crypto::GCM_TAG_SIZE_BYTES> &tag)
{

    // Resuse existing context, but reset/clear previous encryption state.
    if (EVP_CIPHER_CTX_reset(ctx.get()) != 1)
    {
        throw std::runtime_error("failed to rset cipher context");
    }

    // cipher already fetched, so just a safey error check
    if (cipher == nullptr)
    {
        throw std::runtime_error("cipher not initalized");
    }


    // init 
    if (EVP_DecryptInit_ex2(ctx.get(), cipher.get(), aes_key, iv.data(), nullptr) != 1)
    {
        throw std::runtime_error("failed to set KEY and IV");
    }

    // gcm plaintext will be same size as ciphertext
    std::vector<unsigned char> plaintext(ciphertext.size());

    int written = 0;
    int totalWritten = 0;

    // decrypt
    if (!ciphertext.empty())
    {
        if (EVP_DecryptUpdate(ctx.get(), plaintext.data(), &written, ciphertext.data(), static_cast<int>(ciphertext.size())) != 1)
        {
            throw std::runtime_error("decrypt failed");
        }

        totalWritten += written;
    }

    // Supply the authenticaiton tag that was created during encryption
    if (EVP_CIPHER_CTX_ctrl(ctx.get(), EVP_CTRL_AEAD_SET_TAG, static_cast<int>(tag.size()), const_cast<unsigned char *>(tag.data())) != 1)
    {
        throw std::runtime_error("failed to set authentication tag");
    }

    // finalize decryptiong
    // for GCM, this is also where we verify atuthentication
    if (EVP_DecryptFinal_ex(ctx.get(), plaintext.data() + totalWritten, &written) != 1)
    {
        throw AuthenticationError();
    }

    totalWritten += written;
    plaintext.resize(totalWritten);

    return std::string(reinterpret_cast<const char *>(plaintext.data()), plaintext.size());
}