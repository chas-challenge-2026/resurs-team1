#ifndef RESURS_CRYPTO_TYPES_H
#define RESURS_CRYPTO_TYPES_H

#include <cstddef>

enum CryptoStatus
{
    CRYPTO_OK = 0,
    CRYPTO_INVALID_ARGUMENT = -1,
    CRYPTO_BUFFER_TOO_SMALL = -2,
    CRYPTO_AUTHENTICATION_FAILED = -3,
    CRYPTO_INTERNAL_ERROR = -4
};

namespace resurs::crypto
{
    inline constexpr std::size_t AES_256_KEY_SIZE_BYTES = 32;
    inline constexpr std::size_t GCM_IV_SIZE_BYTES = 12;
    inline constexpr std::size_t GCM_TAG_SIZE_BYTES = 16;

}

#endif