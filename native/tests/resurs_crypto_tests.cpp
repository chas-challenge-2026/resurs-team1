#include "crypto_types.h"
#include "resurs_crypto.h"
#include "wrapper.h"

#include <algorithm>
#include <array>
#include <cstdint>
#include <functional>
#include <iostream>
#include <set>
#include <stdexcept>
#include <string>
#include <utility>
#include <vector>

namespace {

using Iv = std::array<unsigned char, resurs::crypto::GCM_IV_SIZE_BYTES>;
using Tag = std::array<unsigned char, resurs::crypto::GCM_TAG_SIZE_BYTES>;
using Key = std::array<unsigned char, resurs::crypto::AES_256_KEY_SIZE_BYTES>;

constexpr Key TEST_KEY = {
    0x52, 0x86, 0x5A, 0x9C, 0x22, 0xEE, 0x88, 0xE5,
    0x03, 0x25, 0x6B, 0x6D, 0x04, 0x01, 0x21, 0x6B,
    0xDE, 0xD4, 0x06, 0xA1, 0xFD, 0x88, 0x61, 0x6C,
    0x1A, 0x7A, 0x77, 0x92, 0x18, 0x76, 0xCF, 0x9C
};

constexpr Key WRONG_KEY = {
    0xA1, 0xA2, 0xA3, 0xA4, 0xA5, 0xA6, 0xA7, 0xA8,
    0xA9, 0xAA, 0xAB, 0xAC, 0xAD, 0xAE, 0xAF, 0xB0,
    0xB1, 0xB2, 0xB3, 0xB4, 0xB5, 0xB6, 0xB7, 0xB8,
    0xB9, 0xBA, 0xBB, 0xBC, 0xBD, 0xBE, 0xBF, 0xC0
};

static_assert(resurs::crypto::AES_256_KEY_SIZE_BYTES == 32, "AES-256 requires a 32-byte key");
static_assert(resurs::crypto::GCM_IV_SIZE_BYTES == 12, "This API requires a 96-bit GCM IV");
static_assert(resurs::crypto::GCM_TAG_SIZE_BYTES == 16, "This API requires a 128-bit GCM tag");

void require(bool condition, const std::string &message)
{
    if (!condition)
        throw std::runtime_error(message);
}

std::vector<uint8_t> bytes(const std::string &text)
{
    return std::vector<uint8_t>(text.begin(), text.end());
}

EncryptionResult core_encrypt(AES256_Encryption &crypto, const std::string &plaintext)
{
    std::string mutable_plaintext = plaintext;
    return crypto.AES256_Encrypt(mutable_plaintext, TEST_KEY.data());
}

EncryptionResult core_encrypt(const std::string &plaintext)
{
    AES256_Encryption crypto;
    return core_encrypt(crypto, plaintext);
}

void expect_core_authentication_failure(const EncryptionResult &encrypted)
{
    AES256_Encryption crypto;
    try {
        (void)crypto.AES256_Decrypt(
            encrypted.ciphertext, TEST_KEY.data(), encrypted.iv, encrypted.tag);
    } catch (const AuthenticationError &) {
        return;
    } catch (const std::exception &error) {
        throw std::runtime_error(std::string("wrong exception type: ") + error.what());
    }
    throw std::runtime_error("manipulated input was accepted");
}

struct WrapperResult
{
    Iv iv{};
    Tag tag{};
    std::vector<uint8_t> ciphertext;
};

WrapperResult wrapper_encrypt(const std::vector<uint8_t> &plaintext)
{
    uint8_t empty_byte = 0;
    WrapperResult result;
    result.ciphertext.resize(std::max<std::size_t>(plaintext.size(), 1));
    const uint8_t *plaintext_data = plaintext.empty() ? &empty_byte : plaintext.data();

    const int status = aes_256_gcm_encrypt(
        plaintext_data,
        static_cast<int>(plaintext.size()),
        TEST_KEY.data(),
        static_cast<int>(TEST_KEY.size()),
        result.iv.data(),
        static_cast<int>(result.iv.size()),
        result.ciphertext.data(),
        static_cast<int>(plaintext.size()),
        result.tag.data(),
        static_cast<int>(result.tag.size()));

    require(status == CRYPTO_OK, "wrapper encryption returned " + std::to_string(status));
    result.ciphertext.resize(plaintext.size());
    return result;
}

std::vector<uint8_t> wrapper_decrypt(const WrapperResult &encrypted)
{
    uint8_t empty_byte = 0;
    const uint8_t *ciphertext_data = encrypted.ciphertext.empty()
        ? &empty_byte
        : encrypted.ciphertext.data();
    std::vector<uint8_t> plaintext(std::max<std::size_t>(encrypted.ciphertext.size(), 1));

    const int status = aes_256_gcm_decrypt(
        ciphertext_data,
        static_cast<int>(encrypted.ciphertext.size()),
        TEST_KEY.data(),
        static_cast<int>(TEST_KEY.size()),
        encrypted.iv.data(),
        static_cast<int>(encrypted.iv.size()),
        plaintext.data(),
        static_cast<int>(encrypted.ciphertext.size()),
        encrypted.tag.data(),
        static_cast<int>(encrypted.tag.size()));

    require(status == CRYPTO_OK, "wrapper decryption returned " + std::to_string(status));
    plaintext.resize(encrypted.ciphertext.size());
    return plaintext;
}

int wrapper_decrypt_status(const WrapperResult &encrypted, std::vector<uint8_t> &plaintext)
{
    uint8_t empty_byte = 0;
    const uint8_t *ciphertext_data = encrypted.ciphertext.empty()
        ? &empty_byte
        : encrypted.ciphertext.data();
    return aes_256_gcm_decrypt(
        ciphertext_data,
        static_cast<int>(encrypted.ciphertext.size()),
        TEST_KEY.data(),
        static_cast<int>(TEST_KEY.size()),
        encrypted.iv.data(),
        static_cast<int>(encrypted.iv.size()),
        plaintext.data(),
        static_cast<int>(plaintext.size()),
        encrypted.tag.data(),
        static_cast<int>(encrypted.tag.size()));
}

void test_core_round_trips()
{
    std::vector<std::string> inputs = {
        "",
        "a",
        "556000-1234",
        "Raksmoergas, kaffe och UTF-8: \xC3\xA5\xC3\xA4\xC3\xB6 \xF0\x9F\x94\x90",
        std::string("abc\0def", 7),
        std::string(4096, 'x'),
        std::string(1024 * 1024, 'L')
    };

    std::string every_byte;
    for (int value = 0; value <= 255; ++value)
        every_byte.push_back(static_cast<char>(value));
    inputs.push_back(every_byte);

    for (const auto &input : inputs) {
        AES256_Encryption encryptor;
        const auto encrypted = core_encrypt(encryptor, input);
        require(encrypted.ciphertext.size() == input.size(), "GCM changed the data length");

        AES256_Encryption decryptor;
        require(decryptor.AES256_Decrypt(
                    encrypted.ciphertext, TEST_KEY.data(), encrypted.iv, encrypted.tag) == input,
                "core round trip changed the plaintext");
    }
}

void test_core_random_iv_and_reuse()
{
    AES256_Encryption crypto;
    std::set<Iv> observed_ivs;
    const std::string plaintext = "identical plaintext";
    std::vector<unsigned char> previous_ciphertext;

    for (int attempt = 0; attempt < 64; ++attempt) {
        const auto encrypted = core_encrypt(crypto, plaintext);
        require(observed_ivs.insert(encrypted.iv).second, "an IV was reused");
        if (!previous_ciphertext.empty())
            require(encrypted.ciphertext != previous_ciphertext,
                    "identical plaintext produced identical ciphertext");
        require(crypto.AES256_Decrypt(
                    encrypted.ciphertext, TEST_KEY.data(), encrypted.iv, encrypted.tag) == plaintext,
                "reused context failed a round trip");
        previous_ciphertext = encrypted.ciphertext;
    }
}

void test_core_rejects_every_modified_ciphertext_byte()
{
    const auto original = core_encrypt("each ciphertext byte is authenticated");
    for (std::size_t index = 0; index < original.ciphertext.size(); ++index) {
        auto modified = original;
        modified.ciphertext[index] ^= 0x01;
        expect_core_authentication_failure(modified);
    }
}

void test_core_rejects_every_modified_iv_and_tag_byte()
{
    const auto original = core_encrypt("IV and tag are authenticated");
    for (std::size_t index = 0; index < original.iv.size(); ++index) {
        auto modified = original;
        modified.iv[index] ^= 0x01;
        expect_core_authentication_failure(modified);
    }
    for (std::size_t index = 0; index < original.tag.size(); ++index) {
        auto modified = original;
        modified.tag[index] ^= 0x01;
        expect_core_authentication_failure(modified);
    }
}

void test_core_rejects_wrong_length_and_mixed_metadata()
{
    const auto first = core_encrypt("first authenticated message");
    const auto second = core_encrypt("second message is different");

    auto truncated = first;
    truncated.ciphertext.pop_back();
    expect_core_authentication_failure(truncated);

    auto extended = first;
    extended.ciphertext.push_back(0x00);
    expect_core_authentication_failure(extended);

    auto wrong_iv = first;
    wrong_iv.iv = second.iv;
    expect_core_authentication_failure(wrong_iv);

    auto wrong_tag = first;
    wrong_tag.tag = second.tag;
    expect_core_authentication_failure(wrong_tag);
}

void test_wrapper_round_trips()
{
    std::vector<std::vector<uint8_t>> inputs = {
        {},
        {0x00},
        bytes("ordinary PII value"),
        bytes(std::string("binary\0value", 12)),
        std::vector<uint8_t>(1024 * 1024, 0xA5)
    };

    std::vector<uint8_t> every_byte;
    for (int value = 0; value <= 255; ++value)
        every_byte.push_back(static_cast<uint8_t>(value));
    inputs.push_back(every_byte);

    for (const auto &input : inputs)
        require(wrapper_decrypt(wrapper_encrypt(input)) == input,
                "wrapper round trip changed the plaintext");
}

void test_wrapper_rejects_null_pointers()
{
    uint8_t byte = 0;
    Iv iv{};
    Tag tag{};
    const int key_size = static_cast<int>(TEST_KEY.size());
    const int iv_size = static_cast<int>(iv.size());
    const int tag_size = static_cast<int>(tag.size());

    require(aes_256_gcm_encrypt(nullptr, 1, TEST_KEY.data(), key_size, iv.data(), iv_size, &byte, 1, tag.data(), tag_size) == CRYPTO_INVALID_ARGUMENT, "encrypt accepted null plaintext");
    require(aes_256_gcm_encrypt(&byte, 1, nullptr, key_size, iv.data(), iv_size, &byte, 1, tag.data(), tag_size) == CRYPTO_INVALID_ARGUMENT, "encrypt accepted null key");
    require(aes_256_gcm_encrypt(&byte, 1, TEST_KEY.data(), key_size, nullptr, iv_size, &byte, 1, tag.data(), tag_size) == CRYPTO_INVALID_ARGUMENT, "encrypt accepted null IV");
    require(aes_256_gcm_encrypt(&byte, 1, TEST_KEY.data(), key_size, iv.data(), iv_size, nullptr, 1, tag.data(), tag_size) == CRYPTO_INVALID_ARGUMENT, "encrypt accepted null ciphertext");
    require(aes_256_gcm_encrypt(&byte, 1, TEST_KEY.data(), key_size, iv.data(), iv_size, &byte, 1, nullptr, tag_size) == CRYPTO_INVALID_ARGUMENT, "encrypt accepted null tag");

    require(aes_256_gcm_decrypt(nullptr, 1, TEST_KEY.data(), key_size, iv.data(), iv_size, &byte, 1, tag.data(), tag_size) == CRYPTO_INVALID_ARGUMENT, "decrypt accepted null ciphertext");
    require(aes_256_gcm_decrypt(&byte, 1, nullptr, key_size, iv.data(), iv_size, &byte, 1, tag.data(), tag_size) == CRYPTO_INVALID_ARGUMENT, "decrypt accepted null key");
    require(aes_256_gcm_decrypt(&byte, 1, TEST_KEY.data(), key_size, nullptr, iv_size, &byte, 1, tag.data(), tag_size) == CRYPTO_INVALID_ARGUMENT, "decrypt accepted null IV");
    require(aes_256_gcm_decrypt(&byte, 1, TEST_KEY.data(), key_size, iv.data(), iv_size, nullptr, 1, tag.data(), tag_size) == CRYPTO_INVALID_ARGUMENT, "decrypt accepted null plaintext");
    require(aes_256_gcm_decrypt(&byte, 1, TEST_KEY.data(), key_size, iv.data(), iv_size, &byte, 1, nullptr, tag_size) == CRYPTO_INVALID_ARGUMENT, "decrypt accepted null tag");
}

void test_wrapper_rejects_negative_lengths()
{
    uint8_t byte = 0;
    Iv iv{};
    Tag tag{};
    const int key_size = static_cast<int>(TEST_KEY.size());
    const int iv_size = static_cast<int>(iv.size());
    const int tag_size = static_cast<int>(tag.size());

    require(aes_256_gcm_encrypt(&byte, -1, TEST_KEY.data(), key_size, iv.data(), iv_size, &byte, 1, tag.data(), tag_size) == CRYPTO_INVALID_ARGUMENT, "encrypt accepted negative plaintext length");
    require(aes_256_gcm_encrypt(&byte, 1, TEST_KEY.data(), -1, iv.data(), iv_size, &byte, 1, tag.data(), tag_size) == CRYPTO_INVALID_ARGUMENT, "encrypt accepted negative key length");
    require(aes_256_gcm_encrypt(&byte, 1, TEST_KEY.data(), key_size, iv.data(), -1, &byte, 1, tag.data(), tag_size) == CRYPTO_INVALID_ARGUMENT, "encrypt accepted negative IV length");
    require(aes_256_gcm_encrypt(&byte, 1, TEST_KEY.data(), key_size, iv.data(), iv_size, &byte, -1, tag.data(), tag_size) == CRYPTO_INVALID_ARGUMENT, "encrypt accepted negative ciphertext length");
    require(aes_256_gcm_encrypt(&byte, 1, TEST_KEY.data(), key_size, iv.data(), iv_size, &byte, 1, tag.data(), -1) == CRYPTO_INVALID_ARGUMENT, "encrypt accepted negative tag length");

    require(aes_256_gcm_decrypt(&byte, -1, TEST_KEY.data(), key_size, iv.data(), iv_size, &byte, 1, tag.data(), tag_size) == CRYPTO_INVALID_ARGUMENT, "decrypt accepted negative ciphertext length");
    require(aes_256_gcm_decrypt(&byte, 1, TEST_KEY.data(), -1, iv.data(), iv_size, &byte, 1, tag.data(), tag_size) == CRYPTO_INVALID_ARGUMENT, "decrypt accepted negative key length");
    require(aes_256_gcm_decrypt(&byte, 1, TEST_KEY.data(), key_size, iv.data(), -1, &byte, 1, tag.data(), tag_size) == CRYPTO_INVALID_ARGUMENT, "decrypt accepted negative IV length");
    require(aes_256_gcm_decrypt(&byte, 1, TEST_KEY.data(), key_size, iv.data(), iv_size, &byte, -1, tag.data(), tag_size) == CRYPTO_INVALID_ARGUMENT, "decrypt accepted negative plaintext length");
    require(aes_256_gcm_decrypt(&byte, 1, TEST_KEY.data(), key_size, iv.data(), iv_size, &byte, 1, tag.data(), -1) == CRYPTO_INVALID_ARGUMENT, "decrypt accepted negative tag length");
}

void test_wrapper_rejects_small_buffers()
{
    const auto plaintext = bytes("buffer boundaries");
    Iv iv{};
    Tag tag{};
    std::vector<uint8_t> ciphertext(plaintext.size());

    require(aes_256_gcm_encrypt(plaintext.data(), static_cast<int>(plaintext.size()), TEST_KEY.data(), static_cast<int>(TEST_KEY.size()), iv.data(), static_cast<int>(iv.size()) - 1, ciphertext.data(), static_cast<int>(ciphertext.size()), tag.data(), static_cast<int>(tag.size())) == CRYPTO_BUFFER_TOO_SMALL, "encrypt accepted a short IV buffer");
    require(aes_256_gcm_encrypt(plaintext.data(), static_cast<int>(plaintext.size()), TEST_KEY.data(), static_cast<int>(TEST_KEY.size()), iv.data(), static_cast<int>(iv.size()), ciphertext.data(), static_cast<int>(ciphertext.size()) - 1, tag.data(), static_cast<int>(tag.size())) == CRYPTO_BUFFER_TOO_SMALL, "encrypt accepted a short ciphertext buffer");
    require(aes_256_gcm_encrypt(plaintext.data(), static_cast<int>(plaintext.size()), TEST_KEY.data(), static_cast<int>(TEST_KEY.size()), iv.data(), static_cast<int>(iv.size()), ciphertext.data(), static_cast<int>(ciphertext.size()), tag.data(), static_cast<int>(tag.size()) - 1) == CRYPTO_BUFFER_TOO_SMALL, "encrypt accepted a short tag buffer");

    const auto encrypted = wrapper_encrypt(plaintext);
    std::vector<uint8_t> output(plaintext.size());
    require(aes_256_gcm_decrypt(encrypted.ciphertext.data(), static_cast<int>(encrypted.ciphertext.size()), TEST_KEY.data(), static_cast<int>(TEST_KEY.size()), encrypted.iv.data(), static_cast<int>(encrypted.iv.size()) - 1, output.data(), static_cast<int>(output.size()), encrypted.tag.data(), static_cast<int>(encrypted.tag.size())) == CRYPTO_BUFFER_TOO_SMALL, "decrypt accepted a short IV buffer");
    require(aes_256_gcm_decrypt(encrypted.ciphertext.data(), static_cast<int>(encrypted.ciphertext.size()), TEST_KEY.data(), static_cast<int>(TEST_KEY.size()), encrypted.iv.data(), static_cast<int>(encrypted.iv.size()), output.data(), static_cast<int>(output.size()) - 1, encrypted.tag.data(), static_cast<int>(encrypted.tag.size())) == CRYPTO_BUFFER_TOO_SMALL, "decrypt accepted a short plaintext buffer");
    require(aes_256_gcm_decrypt(encrypted.ciphertext.data(), static_cast<int>(encrypted.ciphertext.size()), TEST_KEY.data(), static_cast<int>(TEST_KEY.size()), encrypted.iv.data(), static_cast<int>(encrypted.iv.size()), output.data(), static_cast<int>(output.size()), encrypted.tag.data(), static_cast<int>(encrypted.tag.size()) - 1) == CRYPTO_BUFFER_TOO_SMALL, "decrypt accepted a short tag buffer");
}

void test_wrapper_rejects_invalid_key_lengths()
{
    const auto plaintext = bytes("key length validation");
    Iv iv{};
    Tag tag{};
    std::vector<uint8_t> ciphertext(plaintext.size());
    const int key_size = static_cast<int>(TEST_KEY.size());

    require(aes_256_gcm_encrypt(plaintext.data(), static_cast<int>(plaintext.size()), TEST_KEY.data(), key_size - 1, iv.data(), static_cast<int>(iv.size()), ciphertext.data(), static_cast<int>(ciphertext.size()), tag.data(), static_cast<int>(tag.size())) == CRYPTO_INVALID_ARGUMENT, "encrypt accepted a short key");
    require(aes_256_gcm_encrypt(plaintext.data(), static_cast<int>(plaintext.size()), TEST_KEY.data(), key_size + 1, iv.data(), static_cast<int>(iv.size()), ciphertext.data(), static_cast<int>(ciphertext.size()), tag.data(), static_cast<int>(tag.size())) == CRYPTO_INVALID_ARGUMENT, "encrypt accepted a long key");

    const auto encrypted = wrapper_encrypt(plaintext);
    std::vector<uint8_t> output(plaintext.size());
    require(aes_256_gcm_decrypt(encrypted.ciphertext.data(), static_cast<int>(encrypted.ciphertext.size()), TEST_KEY.data(), key_size - 1, encrypted.iv.data(), static_cast<int>(encrypted.iv.size()), output.data(), static_cast<int>(output.size()), encrypted.tag.data(), static_cast<int>(encrypted.tag.size())) == CRYPTO_INVALID_ARGUMENT, "decrypt accepted a short key");
    require(aes_256_gcm_decrypt(encrypted.ciphertext.data(), static_cast<int>(encrypted.ciphertext.size()), TEST_KEY.data(), key_size + 1, encrypted.iv.data(), static_cast<int>(encrypted.iv.size()), output.data(), static_cast<int>(output.size()), encrypted.tag.data(), static_cast<int>(encrypted.tag.size())) == CRYPTO_INVALID_ARGUMENT, "decrypt accepted a long key");
}

void test_wrapper_rejects_wrong_key()
{
    const auto plaintext = bytes("wrong key must fail authentication");
    const auto encrypted = wrapper_encrypt(plaintext);
    std::vector<uint8_t> output(plaintext.size(), 0xA5);

    int status = CRYPTO_INTERNAL_ERROR;
    try {
        status = aes_256_gcm_decrypt(
            encrypted.ciphertext.data(),
            static_cast<int>(encrypted.ciphertext.size()),
            WRONG_KEY.data(),
            static_cast<int>(WRONG_KEY.size()),
            encrypted.iv.data(),
            static_cast<int>(encrypted.iv.size()),
            output.data(),
            static_cast<int>(output.size()),
            encrypted.tag.data(),
            static_cast<int>(encrypted.tag.size()));
    } catch (...) {
        throw std::runtime_error("an exception escaped the wrapper for a wrong key");
    }

    require(status == CRYPTO_AUTHENTICATION_FAILED,
            "wrong key returned status " + std::to_string(status));
    require(std::all_of(output.begin(), output.end(),
                [](uint8_t value) { return value == 0xA5; }),
            "plaintext was copied after authentication with a wrong key");
}

void test_wrapper_does_not_overwrite_oversized_buffers()
{
    const auto plaintext = bytes("sentinel bytes must survive");
    constexpr uint8_t sentinel = 0xCC;
    std::vector<uint8_t> ciphertext(plaintext.size() + 8, sentinel);
    std::vector<uint8_t> iv(resurs::crypto::GCM_IV_SIZE_BYTES + 8, sentinel);
    std::vector<unsigned char> tag(resurs::crypto::GCM_TAG_SIZE_BYTES + 8, sentinel);

    require(aes_256_gcm_encrypt(plaintext.data(), static_cast<int>(plaintext.size()),
                TEST_KEY.data(), static_cast<int>(TEST_KEY.size()),
                iv.data(), static_cast<int>(iv.size()), ciphertext.data(),
                static_cast<int>(ciphertext.size()), tag.data(), static_cast<int>(tag.size())) == CRYPTO_OK,
            "encrypt rejected oversized buffers");
    require(std::all_of(ciphertext.begin() + static_cast<std::ptrdiff_t>(plaintext.size()), ciphertext.end(), [sentinel](uint8_t value) { return value == sentinel; }), "encrypt wrote beyond ciphertext length");
    require(std::all_of(iv.begin() + static_cast<std::ptrdiff_t>(resurs::crypto::GCM_IV_SIZE_BYTES), iv.end(), [sentinel](uint8_t value) { return value == sentinel; }), "encrypt wrote beyond IV length");
    require(std::all_of(tag.begin() + static_cast<std::ptrdiff_t>(resurs::crypto::GCM_TAG_SIZE_BYTES), tag.end(), [sentinel](unsigned char value) { return value == sentinel; }), "encrypt wrote beyond tag length");

    std::vector<uint8_t> output(plaintext.size() + 8, sentinel);
    require(aes_256_gcm_decrypt(ciphertext.data(), static_cast<int>(plaintext.size()),
                TEST_KEY.data(), static_cast<int>(TEST_KEY.size()),
                iv.data(), static_cast<int>(iv.size()), output.data(), static_cast<int>(output.size()),
                tag.data(), static_cast<int>(tag.size())) == CRYPTO_OK,
            "decrypt rejected oversized buffers");
    require(std::equal(plaintext.begin(), plaintext.end(), output.begin()), "decrypt returned wrong plaintext");
    require(std::all_of(output.begin() + static_cast<std::ptrdiff_t>(plaintext.size()), output.end(), [sentinel](uint8_t value) { return value == sentinel; }), "decrypt wrote beyond plaintext length");
}

void test_wrapper_maps_all_tampering_to_authentication_failure()
{
    const auto original = wrapper_encrypt(bytes("wrapper authentication boundary"));

    auto verify_rejected = [](const WrapperResult &modified) {
        std::vector<uint8_t> output(modified.ciphertext.size(), 0xA5);
        int status = CRYPTO_INTERNAL_ERROR;
        try {
            status = wrapper_decrypt_status(modified, output);
        } catch (...) {
            throw std::runtime_error("an exception escaped the C/JNA wrapper");
        }
        require(status == CRYPTO_AUTHENTICATION_FAILED,
                "tampering returned status " + std::to_string(status));
        require(std::all_of(output.begin(), output.end(), [](uint8_t value) { return value == 0xA5; }),
                "unauthenticated plaintext was copied to the output buffer");
    };

    for (std::size_t index = 0; index < original.ciphertext.size(); ++index) {
        auto modified = original;
        modified.ciphertext[index] ^= 0x80;
        verify_rejected(modified);
    }
    for (std::size_t index = 0; index < original.iv.size(); ++index) {
        auto modified = original;
        modified.iv[index] ^= 0x80;
        verify_rejected(modified);
    }
    for (std::size_t index = 0; index < original.tag.size(); ++index) {
        auto modified = original;
        modified.tag[index] ^= 0x80;
        verify_rejected(modified);
    }
}

} // namespace

int main()
{
    const std::vector<std::pair<std::string, std::function<void()>>> tests = {
        {"core round trips", test_core_round_trips},
        {"core random IV and context reuse", test_core_random_iv_and_reuse},
        {"core rejects every modified ciphertext byte", test_core_rejects_every_modified_ciphertext_byte},
        {"core rejects every modified IV and tag byte", test_core_rejects_every_modified_iv_and_tag_byte},
        {"core rejects wrong length and mixed metadata", test_core_rejects_wrong_length_and_mixed_metadata},
        {"wrapper round trips", test_wrapper_round_trips},
        {"wrapper rejects null pointers", test_wrapper_rejects_null_pointers},
        {"wrapper rejects negative lengths", test_wrapper_rejects_negative_lengths},
        {"wrapper rejects small buffers", test_wrapper_rejects_small_buffers},
        {"wrapper rejects invalid key lengths", test_wrapper_rejects_invalid_key_lengths},
        {"wrapper rejects a different valid-length key", test_wrapper_rejects_wrong_key},
        {"wrapper preserves oversized buffer boundaries", test_wrapper_does_not_overwrite_oversized_buffers},
        {"wrapper maps tampering to authentication failure", test_wrapper_maps_all_tampering_to_authentication_failure},
    };

    int failures = 0;
    for (const auto &test : tests) {
        try {
            test.second();
            std::cout << "[PASS] " << test.first << '\n';
        } catch (const std::exception &error) {
            ++failures;
            std::cerr << "[FAIL] " << test.first << ": " << error.what() << '\n';
        } catch (...) {
            ++failures;
            std::cerr << "[FAIL] " << test.first << ": unknown exception\n";
        }
    }

    std::cout << tests.size() - static_cast<std::size_t>(failures) << "/"
              << tests.size() << " test groups passed\n";
    return failures == 0 ? 0 : 1;
}
