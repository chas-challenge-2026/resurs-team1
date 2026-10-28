#include "wrapper.h"
#include "resurs_audit.h"
#include "audit_types.h"

// These helpers keep C++ objects behind the exported C boundary. The public
// function validates buffer sizes first and translates any exception to a result code.
namespace {
    /**
     * Hash the exact canonical bytes and copy the 32-byte result to caller memory.
     * No JSON parsing or normalization happens in native code.
     */
    int wrapper_hash(const uint8_t *canonicalData, size_t canonicalDateLength, uint8_t *output_buffer)
    {
        DigitalSign digSign;
        std::vector<uint8_t> cData(canonicalData, canonicalData + canonicalDateLength);
        std::array<uint8_t, resurs::audit::SHA256_HASH_BYTES> result = digSign.hash(cData);

        if (result.size() != resurs::audit::SHA256_HASH_BYTES)
        {
            return -1;
        }

        std::copy(result.begin(), result.end(), output_buffer);
        return 0;
    }

    /**
     * Sign currentHash with a borrowed Ed25519 key. DigitalSign keeps all temporary OpenSSL ownership local to this call.
     */
    DigitalSignResultCode wrapper_sign(const uint8_t *privateKey, size_t privateKeyLength, uint8_t *input_buffer, uint8_t *output_buffer)
    {
        DigitalSign digSign;
        DigitalSignResultCode result = digSign.sign(privateKey, privateKeyLength, input_buffer, output_buffer);
        return result;
    }
}

// ----------- Exposed functions -----------

HashAndSignResultCode wrapper_hash_and_sign(const uint8_t *canonicalData, size_t canonicalDataLength, const uint8_t *privateKey, size_t privateKeyLength, uint8_t *output_hash_buffer, size_t output_hash_buffer_size,  uint8_t *output_signature_buffer, size_t output_signature_buffer_size) noexcept
{
    // Reject invalid input before constructing C++ objects or passing pointers into OpenSSL.
    if (canonicalData == nullptr ||
        privateKey == nullptr ||
        output_hash_buffer == nullptr ||
        output_signature_buffer == nullptr)
    {
        return HASH_AND_SIGN_INVALID_ARGUMENT;
    }

    if (canonicalDataLength == 0)
    {
        return HASH_AND_SIGN_CANONICAL_DATA_EMPTY;
    }

    if (privateKeyLength != static_cast<int>(resurs::audit::PKEY_BYTES)
    || output_hash_buffer_size != static_cast<int>(resurs::audit::SHA256_HASH_BYTES)
    || output_signature_buffer_size != static_cast<int>(resurs::audit::DIGITAL_SIGNATURE_BYTES))
    {
        return HASH_AND_SIGN_INVALID_BUFFER_SIZE;
    }

    // The work below can allocate or throw. This noexcept boundary turns every failure into a stable result code for JNA.
    try {
        int hash_result = wrapper_hash(canonicalData, canonicalDataLength, output_hash_buffer);
    
        if (hash_result != 0)
        {
            printf("hash fail");
            return HASH_AND_SIGN_HASHING_FAILED;
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
                    return HASH_AND_SIGN_SIGNING_SIGNATURE_LENGTH_FAILED;
                    break;
                case(EVP_DIGEST_SIGN_SIGNING_FAILED): 
                    return HASH_AND_SIGN_SIGNING_EVP_DIGEST_SIGNING_FAILED;
                    break;
                case(SIGN_FAILED_TO_CONVERT_PRIVATE_KEY): 
                    return HASH_AND_SIGN_SIGNING_FAILED_TO_CONVERT_TO_PRIVATE_KEY;
                    break;
                default:
                    return HASH_AND_SIGN_SIGNING_INTERNAL_ERROR;
            }
        }
        return HASH_AND_SIGN_ALL_OK;
    }
    catch (std::runtime_error&)
    {
        return HASH_AND_SIGN_INTERNAL_ERROR;
    }
    catch (...)
    {
        return HASH_AND_SIGN_INTERNAL_ERROR;
    }
}

VerifyChainResult wrapper_verify_chain(const AuditEntry *entries, size_t entryCount, const uint8_t *publicKey, size_t PublicKeyLength) noexcept
{
    if (entries == nullptr || publicKey == nullptr)
    {
        VerifyChainResult result;
        result.result_code = VERIFY_CHAIN_INVALID_ARGUMENT;
        result.index = 0;
        return result;
    }
    if (entryCount == 0)
    {
        VerifyChainResult result;
        result.result_code = VERIFY_CHAIN_ENTRY_COUNT_EMPTY;
        result.index = 0;
        return result;
    }
    if (PublicKeyLength != static_cast<int>(resurs::audit::PKEY_BYTES))
    {
        VerifyChainResult result;
        result.result_code = VERIFY_CHAIN_INVALID_PUBLIC_KEY_SIZE;
        result.index = 0;
        return result;
    }

    for (size_t i = 0; i < entryCount; i++)
    {
        if (entries[i].canonicalData == nullptr)
        {
            VerifyChainResult result;
            result.result_code = VERIFY_CHAIN_CANONICAL_DATA_EMPTY;
            result.index = i;
            return result;
        } 
        if (entries[i].canonicalDataLength <= 0)
        {
            VerifyChainResult result;
            result.result_code = VERIFY_CHAIN_CANONICAL_DATA_LENGTH_EMPTY;
            result.index = i;
            return result;
        }
    }    

    try {
        DigitalSign digSign;
        VerifyChainResult result = digSign.verify_chain(entries, entryCount, publicKey, PublicKeyLength);
        return result;
    }
    catch (std::runtime_error&)
    {
        VerifyChainResult result;
        result.result_code = VERIFY_CHAIN_RESULT_INTERNAL_ERROR;
        result.index = 0;
        return result;
    }
    catch (...)
    {
        VerifyChainResult result;
        result.result_code = VERIFY_CHAIN_RESULT_INTERNAL_ERROR;
        result.index = 0;
        return result;
    }
}


GeneratePrivateKeyResultCode generate_private_key(unsigned char* output_buffer, size_t output_buffer_length) noexcept
{
    if (output_buffer == nullptr)
    {
        return AUDIT_INVALID_ARGUMENT;
    }

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


// Public-key extraction needs a temporary OpenSSL key only for this call.
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
        // A local smart pointer gives that temporary key one clear owner and guarantees cleanup on success, failure, and exceptions.
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
