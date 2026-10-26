#include "resurs_crypto.h"
#include <openssl/ssl.h>
#include <cstring>

/**
 * @file
 * @brief AES-256-GCM operations using the context owned by AES256_Encryption.
 * Method contracts and exception behavior are documented in resurs_crypto.h.
 */

EncryptionResult AES256_Encryption::AES256_Encrypt(std::string &plaintext, const unsigned char* aes_key)
{
    EncryptionResult result;

    // Generate a fresh 96-bit IV before initializing this encryption operation.
    if (RAND_bytes(result.iv.data(), static_cast<int>(result.iv.size())) != 1)
    {
        throw std::runtime_error("failed to generate IV");
    }

    // Reuse the algorithm fetched by the constructor; initialize with this key and IV.


    // AES-GCM's default IV length matches this module's 12-byte IV contract.
    if (EVP_EncryptInit_ex2(ctx.get(), cipher.get(), aes_key, result.iv.data(), nullptr) != 1)
    {
        throw std::runtime_error("failed to set key and IV");
    }

    // GCM requires no padding, so ciphertext has the same byte length as plaintext.
    result.ciphertext.resize(plaintext.size());

    int written = 0;
    int totalWritten = 0;

    // Explicit lengths preserve embedded null bytes in the input string.
    if (!plaintext.empty())
    {
        if (EVP_EncryptUpdate(ctx.get(), result.ciphertext.data(), &written, reinterpret_cast<const unsigned char *>(plaintext.data()), static_cast<int>(plaintext.size())) != 1)
        {
            throw std::runtime_error("encryption failed");
        }

        totalWritten += written;
    }

    // Finalize before reading the authentication tag for the completed message.
    if (EVP_EncryptFinal_ex(ctx.get(), result.ciphertext.data() + totalWritten, &written) != 1)
    {
        throw std::runtime_error("encryption finalization failed");
    }

    totalWritten += written;
    result.ciphertext.resize(totalWritten);

    // Return the tag with the ciphertext and IV; decryption requires all three.
    if (EVP_CIPHER_CTX_ctrl(ctx.get(), EVP_CTRL_AEAD_GET_TAG, static_cast<int>(result.tag.size()), result.tag.data()) != 1)
    {
        throw std::runtime_error("failed to retrieve authentication tag");
    }

    return result;
};

std::string AES256_Encryption::AES256_Decrypt(const std::vector<unsigned char> &ciphertext, const unsigned char* aes_key, const std::array<unsigned char, resurs::crypto::GCM_IV_SIZE_BYTES> &iv, const std::array<unsigned char, resurs::crypto::GCM_TAG_SIZE_BYTES> &tag)
{

    // Clear the previous operation's state before reusing this instance to decrypt.
    if (EVP_CIPHER_CTX_reset(ctx.get()) != 1)
    {
        throw std::runtime_error("failed to rset cipher context");
    }

    // Guard the fetched algorithm before initializing the decrypt operation.
    if (cipher == nullptr)
    {
        throw std::runtime_error("cipher not initalized");
    }


    // Borrow the caller's key and the original IV for this operation.
    if (EVP_DecryptInit_ex2(ctx.get(), cipher.get(), aes_key, iv.data(), nullptr) != 1)
    {
        throw std::runtime_error("failed to set KEY and IV");
    }

    // Keep tentative plaintext local until the final authentication check succeeds.
    std::vector<unsigned char> plaintext(ciphertext.size());

    int written = 0;
    int totalWritten = 0;

    // Decrypt the bytes; this update does not establish authenticity yet.
    if (!ciphertext.empty())
    {
        if (EVP_DecryptUpdate(ctx.get(), plaintext.data(), &written, ciphertext.data(), static_cast<int>(ciphertext.size())) != 1)
        {
            throw std::runtime_error("decrypt failed");
        }

        totalWritten += written;
    }

    // Supply the expected tag. OpenSSL's API takes a mutable pointer to this input.
    if (EVP_CIPHER_CTX_ctrl(ctx.get(), EVP_CTRL_AEAD_SET_TAG, static_cast<int>(tag.size()), const_cast<unsigned char *>(tag.data())) != 1)
    {
        throw std::runtime_error("failed to set authentication tag");
    }

    // GCM authenticates here. Throw before returning any unauthenticated plaintext.
    if (EVP_DecryptFinal_ex(ctx.get(), plaintext.data() + totalWritten, &written) != 1)
    {
        throw AuthenticationError();
    }

    totalWritten += written;
    plaintext.resize(totalWritten);

    return std::string(reinterpret_cast<const char *>(plaintext.data()), plaintext.size());
}
