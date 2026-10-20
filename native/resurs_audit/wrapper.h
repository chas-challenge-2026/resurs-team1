#ifndef RESURS_AUDIT_WRAPPER_H
#define RESURS_AUDIT_WRAPPER_H

#if defined(_WIN32)
    #if defined(RESURS_AUDIT_BUILDING_DLL)
        #define RESURS_AUDIT_API __declspec(dllexport)
    #else
        #define RESURS_AUDIT_API __declspec(dllimport)
    #endif
#else
    #define RESURS_AUDIT_API __attribute__((visibility("default")))
#endif


#include "audit_types.h"

#ifdef __cplusplus
extern "C" {
#endif

// Exposed functions
//RESURS_AUDIT_API int wrapper_hash_and_sign(const uint8_t* canonicalData, size_t canonicalDataLength, const uint8_t* privateKey, size_t privateKeyLength, uint8_t* output_hash_buffer, uint8_t* output_signature_buffer);
RESURS_AUDIT_API HashAndSignResultCode wrapper_hash_and_sign(const uint8_t* canonicalData, size_t canonicalDataLength, const uint8_t* privateKey, size_t privateKeyLength, uint8_t* output_hash_buffer, size_t output_hash_buffer_length, uint8_t* output_signature_buffer, size_t output_signature_buffer_length);
RESURS_AUDIT_API VerifyChainResult wrapper_verify_chain(const AuditEntry *entries, size_t entryCount, const uint8_t *publicKey, size_t PublicKeyLength);

//RESURS_AUDIT_API void generate_private_key(unsigned char* output_buffer);
RESURS_AUDIT_API GeneratePrivateKeyResultCode generate_private_key(unsigned char* output_buffer, size_t output_buffer_length);
//RESURS_AUDIT_API int get_public_key(unsigned char* private_key, size_t private_key_size, unsigned char* output_public_key, size_t output_public_key_size);
RESURS_AUDIT_API GetPublicKeyResultCode get_public_key(unsigned char* private_key, size_t private_key_size, unsigned char* output_public_key, size_t output_public_key_size) noexcept;


#ifdef __cplusplus
}
#endif

#endif
