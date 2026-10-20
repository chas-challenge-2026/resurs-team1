#include "wrapper.h"
#include "resurs_audit.h"
#include "audit_types.h"

// ----------- Internal functions in anonymous namespace so it wont get exported  -----------
namespace {
    int wrapper_hash(const uint8_t *canonicalData, size_t canonicalDateLength, uint8_t *output_buffer)
    {
        DigitalSign digSign;

        std::vector<uint8_t> cData(canonicalData, canonicalData + canonicalDateLength);

        std::array<uint8_t, resurs::audit::SHA256_HASH_BYTES> result = digSign.hash(cData);

        if (result.size() != resurs::audit::SHA256_HASH_BYTES)
        {
            //return WRAPPER_HASH_FAILED;
            return -1;
        }

        std::copy(result.begin(), result.end(), output_buffer);

        //return WRAPPER_HASH_OK;
        return 0;
    }

    //int wrapper_sign(const uint8_t *privateKey, size_t privateKeyLength, uint8_t *input_buffer, uint8_t *output_buffer)
    DigitalSignResultCode wrapper_sign(const uint8_t *privateKey, size_t privateKeyLength, uint8_t *input_buffer, uint8_t *output_buffer)
    {
        DigitalSign digSign;

        DigitalSignResultCode result = digSign.sign(privateKey, privateKeyLength, input_buffer, output_buffer);

        return result;
        // if (result != SIGN_ALL_OK)
        // {
        //     return DigitalSignResultCode;
        //     //return WRAPPER_SIGN_FAILED;
        //     //return -1;
        // }

        // return WRAPPER_SIGN_OK;
        // //return 0;
    }

}

// ----------- Exposed functions -----------

int wrapper_hash_and_sign(const uint8_t *canonicalData, size_t canonicalDataLength, const uint8_t *privateKey, size_t privateKeyLength, uint8_t *output_hash_buffer, uint8_t *output_signature_buffer)
{
    // validation

    // code
    try {
        int hash_result = wrapper_hash(canonicalData, canonicalDataLength, output_hash_buffer);
    
        if (hash_result != 0)
        {
            printf("hash fail");
            return HASH_AND_SIGN_HASHING_FAILED;
            //return hash_result;
        }
    
        DigitalSignResultCode signature_result = wrapper_sign(privateKey, privateKeyLength, output_hash_buffer, output_signature_buffer);
        
        if (signature_result != SIGN_ALL_OK)
        {
            switch (signature_result)
            {
                case(EVP_DIGEST_SIGN_INIT_FAILED): 
                    return HASH_AND_SIGN_SIGNING_INIT_FAILED;
                    break;
                case(EVP_DIGEST_SIGN_SIGNATURE_LENGTH_FAILED): 
                    return HASH_AND_SIGN_SIGNING_INIT_FAILED;
                    break;
                case(EVP_DIGEST_SIGN_SIGNING_FAILED): 
                    return HASH_AND_SIGN_SIGNING_INIT_FAILED;
                    break;
                case(SIGN_FAILED_TO_CONVERT_PRIVATE_KEY): 
                    return HASH_AND_SIGN_SIGNING_INIT_FAILED;
                    break;
                default:
                    return HASH_AND_SIGN_SIGNING_INTERNAL_ERROR;
            }
        }
        return HASH_AND_SIGN_ALL_OK;

        // if (signature_result != SIGN_ALL_OK)
        // {
        //     printf("signature fail");
        //     //return signature_result;
        // }
    
        //return 0;
    }
    catch (std::runtime_error&)
    {
        return HASH_AND_SIGN_INTERNAL_ERROR;
        //return -1;
    }
    catch (...)
    {
        return HASH_AND_SIGN_INTERNAL_ERROR;
        //return -1;
    }
}

VerifyChainResult wrapper_verify_chain(const AuditEntry *entries, size_t entryCount, const uint8_t *publicKey, size_t PublicKeyLength)
{
    DigitalSign digSign;

    VerifyChainResult result = digSign.verify_chain(entries, entryCount, publicKey, PublicKeyLength);

    return result;
}


GeneratePrivateKeyResultCode generate_private_key(unsigned char* output_buffer, size_t output_buffer_length)
{
    if (output_buffer_length < resurs::audit::PKEY_BYTES)
    {
        return AUDIT_PRIVATE_KEY_BUFFER_TOO_SMALL;
    }
    try {

        DigitalSign digSign;
        
        std::array<unsigned char, resurs::audit::PKEY_BYTES> pKey = digSign.generate_private_key();
        
        std::copy(pKey.begin(), pKey.end(), output_buffer);

        return AUDIT_PRIVATE_KEY_OK;
    }
    catch (std::runtime_error&)
    {
        return AUDIT_PRIVATE_KEY_INTERNAL_ERROR;
    }
    catch (...)
    {
        return AUDIT_PRIVATE_KEY_INTERNAL_ERROR;
    }
}

// void generate_private_key(unsigned char *output_buffer)
// {
//     DigitalSign digSign;

//     std::array<unsigned char, resurs::audit::PKEY_BYTES> pKey = digSign.generate_private_key();

//     std::copy(pKey.begin(), pKey.end(), output_buffer);
// }

// New version, now using custom PkeyPtr to take advantage of RAII so as to not leave the ownership externally and not freeing
GetPublicKeyResultCode get_public_key(unsigned char* private_key, size_t private_key_size, unsigned char* output_public_key, size_t output_public_key_size) noexcept
{
    if (private_key == nullptr || output_public_key == nullptr)
    {
        return AUDIT_KEY_NULL_ARGUMENT;
    }

    if (private_key_size != resurs::audit::PKEY_BYTES)
    {
        return AUDIT_KEY_INVALID_PRIVATE_KEY_SIZE;
    }
    if (output_public_key_size < resurs::audit::PKEY_BYTES)
    {
        return AUDIT_KEY_OUTPUT_BUFFER_TOO_SMALL;
    }


    try {
        
        // The class has this set, but it's private.
        // Since this is the only method that requires it, we declare it locally in this scope again rather than moving the original to public
        using PkeyPtr = std::unique_ptr<EVP_PKEY, decltype(&EVP_PKEY_free)>;
        
        PkeyPtr pkey(
            EVP_PKEY_new_raw_private_key(
                EVP_PKEY_ED25519,
                nullptr,
                private_key,
                resurs::audit::PKEY_BYTES),
                &EVP_PKEY_free
            );
            
            if (!pkey)
            {
                return AUDIT_KEY_CREATION_FAILED;
            }
            
            DigitalSign digSign;
            
            const auto public_key = digSign.get_public_key(pkey.get());
            
            std::copy(public_key.begin(), public_key.end(), output_public_key);
            
            return AUDIT_KEY_OK;
    }
    catch (const std::runtime_error &) {
        return AUDIT_KEY_INTERNAL_ERROR;
    }
    catch (...)
    {
        return AUDIT_KEY_INTERNAL_ERROR;
    }
}