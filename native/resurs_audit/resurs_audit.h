#ifndef RESURS_AUDIT_H
#define RESURS_AUDIT_H

#include <iostream>
#include <openssl/evp.h>
#include <openssl/err.h>
#include <array>
#include <vector>
#include <memory>

#include "audit_types.h"

class DigitalSign
{
    private:
        using PkeyCtxPtr = std::unique_ptr<EVP_PKEY_CTX, decltype(&EVP_PKEY_CTX_free)>;
        using PkeyPtr = std::unique_ptr<EVP_PKEY, decltype(&EVP_PKEY_free)>;
        using DigestSignCtxPtr = std::unique_ptr<EVP_MD_CTX, decltype(&EVP_MD_CTX_free)>;
        DigestSignCtxPtr ctx;
        PkeyCtxPtr pKeyCtxPtr;
        PkeyPtr pKeyPtr;
    
    public:
        DigitalSign() : ctx(EVP_MD_CTX_new(), EVP_MD_CTX_free), pKeyCtxPtr(nullptr, &EVP_PKEY_CTX_free), pKeyPtr(nullptr, &EVP_PKEY_free)
        {
            if (ctx == nullptr)
            {
                throw std::runtime_error("failed to create context.");
            }
        };
        std::array<unsigned char, resurs::audit::PKEY_BYTES> generate_private_key();
        std::array<uint8_t, 32> get_public_key(EVP_PKEY* privateKey);
        
        std::array<uint8_t, resurs::audit::SHA256_HASH_BYTES> hash(const std::vector<uint8_t> &data);
        DigitalSignResultCode sign(const uint8_t *privateKey, size_t privateKeyLength, uint8_t *input_hash_buffer, uint8_t *output_signature_buffer);
        VerifyChainResult verify_chain(const AuditEntry *entries, size_t entryCount, const uint8_t *publicKey, size_t publicKeyLength);

        PkeyPtr convert_c_private_key_to_EVP_PKEY_POINTER(const uint8_t *privateKey, size_t privateKeyLength);
        PkeyPtr convert_c_public_key_to_EVP_PKEY_POINTER(const uint8_t *publicKey, size_t publicKeyLength);
};


#endif
