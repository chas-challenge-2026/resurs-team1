#ifndef RESURS_AUDIT_TYPES_H
#define RESURS_AUDIT_TYPES_H

#include <iostream>
#include <cstdint>


#include <stdint.h>
#include <stdio.h>
#include <string.h>
#include <cstring>
static void print_hex(const uint8_t *data, size_t length)
{
    for (size_t i = 0; i < length; ++i)
    {
        printf("%02x ", data[i]);
    }
    putchar('\n');
}


namespace resurs::audit
{
    inline constexpr std::size_t SHA256_HASH_BYTES = 32;
    inline constexpr std::size_t PKEY_BYTES = 32;
    inline constexpr std::size_t DIGITAL_SIGNATURE_BYTES = 64;
}

struct AuditEntry
{
    const uint8_t *canonicalData;
    size_t canonicalDataLength;

    uint8_t signature[resurs::audit::DIGITAL_SIGNATURE_BYTES];
    uint8_t previousHash[resurs::audit::SHA256_HASH_BYTES];
    uint8_t currentHash[resurs::audit::SHA256_HASH_BYTES];
        
    //std::array<uint8_t, resurs::audit::SHA256_HASH_BYTES> previousHash;
    //std::array<uint8_t, resurs::audit::SHA256_HASH_BYTES> currentHash;
    uint64_t sequenceNumber;
};

enum DigitalSignResultCode
{
    SIGN_ALL_OK = 0,
    EVP_DIGEST_SIGN_INIT_FAILED = -1,
    EVP_DIGEST_SIGN_SIGNATURE_LENGTH_FAILED = -2,
    EVP_DIGEST_SIGN_SIGNING_FAILED = -3,
    SIGN_FAILED_TO_CONVERT_PRIVATE_KEY = -4
};

enum VerifyChainResultCode
{
    VERIFY_CHAIN_ALL_OK = 0,
    PREVIOUS_HASH_MISSMATCH = -1,
    CURRENT_HASH_MISSMATCH = -2,
    EVP_DIGEST_VERIFY_INIT_FAILED = -3,
    EVP_DIGEST_VERIFY_FAILED = -4,
    VERIFY_CHAIN_FAILED_TO_CONVERT_PUBLIC_KEY = -5

};

struct VerifyChainResult
{
    VerifyChainResultCode result_code;
    uint64_t index;
};

/*
struct AuditEntryChain
{
    std::vector<AuditEntry> entries;
    
};
*/

/* exemmple på wrappern
int verify_audit_chain(
    const AuditVerifyEntry* entries,
    size_t entryCount,
    EVP_PKEY* publicKey
);


*/

#endif