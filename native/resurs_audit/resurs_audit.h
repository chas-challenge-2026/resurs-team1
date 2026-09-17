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
public:
    // Konstruktor
    DigitalSign() : ctx(EVP_MD_CTX_new(), EVP_MD_CTX_free), pKeyCtxPtr(nullptr, &EVP_PKEY_CTX_free), pKeyPtr(nullptr, &EVP_PKEY_free) 
    {
        if (ctx == nullptr)
        {
            throw std::runtime_error("failed to create context.");
        }
    };

private:
    //std::array<std::size_t, resurs::audit::DIGITAL_SIGNATURE_BYTES> sign(const std::vector<uint8_t>& data, EVP_PKEY *pkey);
    //std::array<uint8_t, resurs::audit::DIGITAL_SIGNATURE_BYTES> sign(const std::vector<uint8_t>& data, EVP_PKEY *pkey);
    std::vector<uint8_t> sign(const std::vector<uint8_t>& data, EVP_PKEY *pkey);

    
    std::array<unsigned char, resurs::audit::PKEY_BYTES> generate_private_key();

    std::array<uint8_t, resurs::audit::SHA256_HASH_BYTES> hash(const std::vector<uint8_t> &data);

    std::array<uint8_t, resurs::audit::SHA256_HASH_BYTES> hash_chain(const std::string &data, const std::array<uint8_t, resurs::audit::SHA256_HASH_BYTES>& prev_hash);

    int verify_chain(const AuditEntryChain* chain, size_t entryCount, EVP_PKEY* publicKey);
 

    using PkeyCtxPtr = std::unique_ptr<EVP_PKEY_CTX, decltype(&EVP_PKEY_CTX_free)>;
    using PkeyPtr = std::unique_ptr<EVP_PKEY, decltype(&EVP_PKEY_free)>;
    using DigestSignCtxPtr = std::unique_ptr<EVP_MD_CTX, decltype(&EVP_MD_CTX_free)>;
    DigestSignCtxPtr ctx;
    PkeyCtxPtr pKeyCtxPtr;
    PkeyPtr pKeyPtr;

    std::array<unsigned char, resurs::audit::PKEY_BYTES> key = {0x52, 0x86, 0x5A, 0x9C, 0x22, 0xEE, 0x88, 0xE5,
                                                                0x03, 0x25, 0x6B, 0x6D, 0x04, 0x01, 0x21, 0x6B,
                                                                0xDE, 0xD4, 0x06, 0xA1, 0xFD, 0x88, 0x61, 0x6C,
                                                                0x1A, 0x7A, 0x77, 0x92, 0x18, 0x76, 0xCF, 0x9C};
};

#endif
