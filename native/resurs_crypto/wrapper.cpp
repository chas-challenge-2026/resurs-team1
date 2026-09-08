#include "wrapper.h"
#include "resurs_crypto.h"
#include <cstring>


int aes_256_gcm_encrypt(const uint8_t *plaintext, int plaintext_len, uint8_t *iv, int iv_len, uint8_t *ciphertext, int ciphertext_len, unsigned char *tag, int tag_len)
{
    // First validate input
    if (!plaintext || !iv || !ciphertext || !tag)
        return CRYPTO_INVALID_ARGUMENT;
    
    if (ciphertext_len < 0 || iv_len < 0 || plaintext_len < 0 || tag_len < 0)
    {
        return CRYPTO_INVALID_ARGUMENT;
    }

    if (iv_len < resurs::crypto::GCM_IV_SIZE_BYTES || tag_len < resurs::crypto::GCM_TAG_SIZE_BYTES || ciphertext_len < plaintext_len)
    {
        return CRYPTO_BUFFER_TOO_SMALL;
    }

    // Then execute the code in a try-block, so we catch any errors and can return errors in a JNA-compatible way.
    try {

        // Reintrepret_cast makes a kopia of the underlying bytes to plaintext_ and changes how it viewes the bytes.
        // In this case the original pointer was a uint8_t, but we cast it do a const char*.
        // And then plaintext_len is in case we get a null value byte in the middle, we know we will keep going until we read the whole length.
        std::string plaintext_(reinterpret_cast<const char *>(plaintext), plaintext_len);
        AES256_Encryption encryption;
        EncryptionResult encryption_result = encryption.AES256_Encrypt(plaintext_);
        
        // static_cast does work in runtime.
        // It just means which typeconvertion that should be done, which in this case i a convertion from ciphertext's lenth(std::size_t) to a int. 
        if (iv_len < static_cast<int>(encryption_result.iv.size())
        || (ciphertext_len < static_cast<int>(encryption_result.ciphertext.size()))
        || (tag_len < static_cast<int>(encryption_result.tag.size()))
        ) {
            return CRYPTO_BUFFER_TOO_SMALL;
        }

        memcpy(iv, encryption_result.iv.data(), encryption_result.iv.size());
        memcpy(tag, encryption_result.tag.data(), encryption_result.tag.size());
        memcpy(ciphertext, encryption_result.ciphertext.data(), encryption_result.ciphertext.size());
        return CRYPTO_OK;
    }
    
    catch (const std::runtime_error &) {
        return CRYPTO_INTERNAL_ERROR;
    }

    catch (...) {
        return CRYPTO_INTERNAL_ERROR;
    }

}

int aes_256_gcm_decrypt(const uint8_t *ciphertext, int ciphertext_len, const uint8_t *iv, int iv_len, uint8_t *plaintext, int plaintext_len, const unsigned char *tag, int tag_len)
{
    if (!ciphertext || !iv || !plaintext || !tag )
    {
        return CRYPTO_INVALID_ARGUMENT;
    }
    
    if (ciphertext_len < 0 || iv_len < 0 || plaintext_len < 0 || tag_len < 0)
    {
        return CRYPTO_INVALID_ARGUMENT;
    }

    if (iv_len < resurs::crypto::GCM_IV_SIZE_BYTES || tag_len < resurs::crypto::GCM_TAG_SIZE_BYTES || plaintext_len < ciphertext_len)
    {
        return CRYPTO_BUFFER_TOO_SMALL;
    }

    
    try {        
        std::vector<unsigned char> ciphertext_(ciphertext, ciphertext + ciphertext_len
        );
        
        std::array<unsigned char, resurs::crypto::GCM_IV_SIZE_BYTES> iv_;
        memcpy(iv_.data(), iv, iv_.size());
        
        std::array<unsigned char, resurs::crypto::GCM_TAG_SIZE_BYTES> tag_;
        memcpy(tag_.data(), tag, tag_.size());
        
        AES256_Encryption encryption;
        
        std::string decrypted = encryption.AES256_Decrypt(ciphertext_, iv_,tag_);
        
        memcpy(plaintext, decrypted.data(), decrypted.size());
        return CRYPTO_OK;
    }
    catch (const AuthenticationError &)
    {
        return CRYPTO_AUTHENTICATION_FAILED;
    }
    catch (const std::runtime_error &)
    {
        return CRYPTO_INTERNAL_ERROR;
    }
    catch (...)
    {
        return CRYPTO_INTERNAL_ERROR;
    }
}