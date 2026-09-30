#ifndef RESURS_AUDIT_WRAPPER_H
#define RESURS_AUDIT_WRAPPER_H
#include "audit_types.h"

#ifdef __cplusplus
#include <vector>
extern "C" {
#endif

std::array<uint8_t, resurs::audit::SHA256_HASH_BYTES> hash(const std::vector<uint8_t> &data);


int wrapper_hash(const uint8_t *canonicalData, size_t canonicalDateLength, uint8_t* output_buffer);

int wrapper_sign(const uint8_t *privateKey, size_t privateKeyLength, uint8_t* output_hash_buffer, uint8_t *output_buffer);

int wrapper_hash_and_sign(const uint8_t* canonicalData, size_t canonicalDataLength, const uint8_t* privateKey, size_t privateKeyLength, uint8_t* output_hash_buffer, uint8_t* output_signature_buffer);

VerifyChainResult wrapper_verify_chain(const AuditEntry *entries, size_t entryCount, const uint8_t *publicKey, size_t PublicKeyLength);


void generate_private_key(unsigned char* output_buffer);

int get_public_key(unsigned char* privateKey, unsigned char* output_publicKey);





#ifdef __cplusplus
}
#endif

#endif