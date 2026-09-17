#ifndef RESURS_AUDIT_WRAPPER_H
#define RESURS_AUDIT_WRAPPER_H
#include "audit_types.h"

#ifdef __cplusplus
extern "C" {
#endif

std::array<uint8_t, resurs::audit::SHA256_HASH_BYTES> hash(const std::vector<uint8_t> &data);


int hash(const uint8_t *canonicalData, size_t canonicalDateLength, uint8_t* output_buffer);

int sign(const uint8_t *canonicalData, size_t canonicalDateLength, const uint8_t *privateKey, size_t privateKeyLength, uint8_t output_buffer);

int verify_chain(const AuditEntry *entries, size_t entryCount, const uint8_t *publicKey, size_t PublicKeyLength);

unsigned char* generate_private_key();





#ifdef __cplusplus
}
#endif

#endif