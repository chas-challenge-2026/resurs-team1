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
    SEQUENCE_NUMBER_MISSMATCH = -2,
    CURRENT_HASH_MISSMATCH = -3,
    EVP_DIGEST_VERIFY_INIT_FAILED = -4,
    EVP_DIGEST_VERIFY_FAILED = -5,
    VERIFY_CHAIN_FAILED_TO_CONVERT_PUBLIC_KEY = -6

};

struct VerifyChainResult
{
    VerifyChainResultCode result_code;
    uint64_t index;
};

enum GetPublicKeyResultCode
{
    AUDIT_KEY_OK = 0,
    AUDIT_KEY_NULL_ARGUMENT = -1,
    AUDIT_KEY_INVALID_PRIVATE_KEY_SIZE = -2,
    AUDIT_KEY_OUTPUT_BUFFER_TOO_SMALL = -3,
    AUDIT_KEY_CREATION_FAILED = -4,
    AUDIT_KEY_INTERNAL_ERROR = -5
};

enum GeneratePrivateKeyResultCode
{
    AUDIT_PRIVATE_KEY_OK = 0,
    AUDIT_PRIVATE_KEY_BUFFER_TOO_SMALL = -1,
    AUDIT_PRIVATE_KEY_INTERNAL_ERROR = -2
};

enum HashAndSignResultCode
{
    HASH_AND_SIGN_ALL_OK = 0,

    // validation errors

    // hash
    HASH_AND_SIGN_HASHING_FAILED = -5,
    // DigitalSign
    HASH_AND_SIGN_SIGNING_INIT_FAILED = -6,
    HASH_AND_SIGN_SIGNING_SIGNATURE_LENGTH_FAILED = -7,
    HASH_AND_SIGN_SIGNING_EVP_DIGEST_SIGNING_FAILED = -8,
    HASH_AND_SIGN_SIGNING_FAILED_TO_CONVERT_TO_PRIVATE_KEY = -9,
    HASH_AND_SIGN_SIGNING_INTERNAL_ERROR = -10,

    HASH_AND_SIGN_INTERNAL_ERROR = -11
};
#endif