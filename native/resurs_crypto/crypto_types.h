#ifndef RESURS_CRYPTO_TYPES_H
#define RESURS_CRYPTO_TYPES_H

#include <cstddef>

namespace resurs::crypto
{
    inline constexpr std::size_t AES_256_KEY_SIZE_BYTES = 32;
    inline constexpr std::size_t GCM_IV_SIZE_BYTES = 12;
    inline constexpr std::size_t GCM_TAG_SIZE_BYTES = 16;
}

#endif