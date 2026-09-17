#ifndef RESURS_AUDIT_TYPES_H
#define RESURS_AUDIT_TYPES_H

#include <iostream>

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
    //uint8_t previousHash[resurs::audit::SHA256_HASH_BYTES];
    //uint8_t currentHash[resurs::audit::SHA256_HASH_BYTES];
    
    std::array<uint8_t, resurs::audit::SHA256_HASH_BYTES> previousHash;
    std::array<uint8_t, resurs::audit::SHA256_HASH_BYTES> currentHash;
    uint64_t sequenceNumber;
};

struct AuditEntryChain
{
    std::vector<AuditEntry> entries;

};

/* exemmple på wrappern
int verify_audit_chain(
    const AuditVerifyEntry* entries,
    size_t entryCount,
    EVP_PKEY* publicKey
);


*/

#endif