
#include "resurs_crypto.h"
#include <openssl/ssl.h>
#include <cstring>


EncryptionResult AES256_Encryption::AES256_Encrypt()
    {
        EncryptionResult result;
        
        // Create 96bit IV
        if (RAND_bytes(result.iv.data(), static_cast<int>(result.iv.size())) != 1)
        {
            throw std::runtime_error("failed to generate IV");
        }

        // Fetch the implementation
        //cipher = std::make_unique<EVP_CIPHER>(EVP_CIPHER_fetch(nullptr, "AES-256-GCM", nullptr), EVP_CIPHER_free);
        cipher.reset(EVP_CIPHER_fetch(nullptr, "AES-256-GCM", nullptr));

        if (cipher == nullptr)
        {
            throw std::runtime_error("failed to fetch aes-256-gcm");
        }

        // We could supply key and IV directly, but for readability we will explicitcly set the IV length to 12 byte below, even though the default is 12.
        if (EVP_EncryptInit_ex2(ctx.get(), cipher.get(), nullptr, nullptr, nullptr) != 1)
        {
            throw std::runtime_error("failed to init chipher");
        }

        if (EVP_CIPHER_CTX_ctrl(ctx.get(), EVP_CTRL_AEAD_SET_IVLEN, static_cast<int>(result.iv.size()), nullptr) != 1)
        {
            throw std::runtime_error("failed to set IV length");
        }

        // Här ska vi hämta key från docker secrets

        if (EVP_EncryptInit_ex2(ctx.get(), nullptr, key.data(), result.iv.data(), nullptr) != 1)
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
        if (EVP_EncryptFinal_ex(
                ctx.get(),
                result.ciphertext.data() + totalWritten,
                &written) != 1)
        {
            throw std::runtime_error("encryption finalization failed");
        }

        totalWritten += written;
        result.ciphertext.resize(totalWritten);

        // retrieve GCM authenticaiton tag
        if (EVP_CIPHER_CTX_ctrl(
                ctx.get(),
                EVP_CTRL_AEAD_GET_TAG,
                static_cast<int>(result.tag.size()),
               result.tag.data()) != 1)
        {
            throw std::runtime_error("failed to retrieve authentication tag");
        }

        return result;
    };

/*
int main()
{
    std::string text = "aaaaaaaabbbbbbbbbcccccccccccc";
    std::array<uint8_t, 12> iv;
    std::vector<uint8_t> cipher_text(text.size());
    std::array<unsigned char, 16> tag;
    int result = aes_256_gcm_encrypt(reinterpret_cast<uint8_t*>(text.data()), text.size(), iv.data(), cipher_text.data(), static_cast<int>(cipher_text.size()), tag.data());
    std::cout << "Encrypted string: " << cipher_text.data() << std::endl << "Result: " << result;
}

*/