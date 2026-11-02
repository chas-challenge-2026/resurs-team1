#include "wrapper.h"
#include "audit_types.h"

#include <algorithm>
#include <array>
#include <cassert>
#include <cstddef>
#include <cstring>
#include <iostream>
#include <iterator>
#include <stdexcept>
#include <string>
#include <type_traits>
#include <utility>
#include <vector>


namespace {

void require_test(
    bool condition,
    const char* expression,
    int line)
{
    if (!condition)
    {
        throw std::runtime_error(
            "line " + std::to_string(line) +
            ": requirement failed: " + expression
        );
    }
}

} // namespace


// Standard assert is disabled by NDEBUG in Release builds. Keep these checks
// and all function calls inside them active in every build configuration.
#undef assert
#define assert(condition) \
    require_test(static_cast<bool>(condition), #condition, __LINE__)


// ============================================================
// TEST HELPERS
// ============================================================

std::array<uint8_t, resurs::audit::PKEY_BYTES>
get_private_key()
{
    std::array<uint8_t, resurs::audit::PKEY_BYTES> privateKey{};
    assert(
        generate_private_key(
            privateKey.data(),
            privateKey.size()
        ) == AUDIT_PRIVATE_KEY_OK
    );
    return privateKey;
}


std::array<uint8_t, resurs::audit::PKEY_BYTES>
get_public_key(
    const std::array<uint8_t, resurs::audit::PKEY_BYTES>& privateKey)
{
    auto privateKeyCopy = privateKey;
    std::array<uint8_t, resurs::audit::PKEY_BYTES> publicKey{};
    assert(
        ::get_public_key(
            privateKeyCopy.data(),
            privateKeyCopy.size(),
            publicKey.data(),
            publicKey.size()
        ) == AUDIT_KEY_OK
    );

    return publicKey;
}


// Reference - https://www.rfc-editor.org/rfc/rfc8032.html#section-7.1
// RFC 8032, section 7.1, Test 1 supplies the fixed Ed25519 seed and
// corresponding public key. The hash and signature below form a deterministic
// known-answer vector for this module's SHA-256-then-Ed25519 construction.
constexpr std::array<uint8_t, 3> KNOWN_ANSWER_CANONICAL_DATA = {
    'a', 'b', 'c'
};

constexpr std::array<uint8_t, resurs::audit::PKEY_BYTES>
KNOWN_ANSWER_PRIVATE_KEY = {
    0x9D, 0x61, 0xB1, 0x9D, 0xEF, 0xFD, 0x5A, 0x60,
    0xBA, 0x84, 0x4A, 0xF4, 0x92, 0xEC, 0x2C, 0xC4,
    0x44, 0x49, 0xC5, 0x69, 0x7B, 0x32, 0x69, 0x19,
    0x70, 0x3B, 0xAC, 0x03, 0x1C, 0xAE, 0x7F, 0x60
};

constexpr std::array<uint8_t, resurs::audit::PKEY_BYTES>
KNOWN_ANSWER_PUBLIC_KEY = {
    0xD7, 0x5A, 0x98, 0x01, 0x82, 0xB1, 0x0A, 0xB7,
    0xD5, 0x4B, 0xFE, 0xD3, 0xC9, 0x64, 0x07, 0x3A,
    0x0E, 0xE1, 0x72, 0xF3, 0xDA, 0xA6, 0x23, 0x25,
    0xAF, 0x02, 0x1A, 0x68, 0xF7, 0x07, 0x51, 0x1A
};

constexpr std::array<uint8_t, resurs::audit::SHA256_HASH_BYTES>
KNOWN_ANSWER_HASH = {
    0xBA, 0x78, 0x16, 0xBF, 0x8F, 0x01, 0xCF, 0xEA,
    0x41, 0x41, 0x40, 0xDE, 0x5D, 0xAE, 0x22, 0x23,
    0xB0, 0x03, 0x61, 0xA3, 0x96, 0x17, 0x7A, 0x9C,
    0xB4, 0x10, 0xFF, 0x61, 0xF2, 0x00, 0x15, 0xAD
};

constexpr std::array<uint8_t, resurs::audit::DIGITAL_SIGNATURE_BYTES>
KNOWN_ANSWER_SIGNATURE = {
    0x09, 0x6F, 0x55, 0x69, 0xD8, 0x07, 0xEE, 0x8A,
    0xC7, 0xB1, 0x91, 0x3D, 0xA7, 0x0C, 0xF0, 0xAA,
    0xB3, 0x35, 0xC2, 0x58, 0xF4, 0xB9, 0x4C, 0x8F,
    0x21, 0x0D, 0xD1, 0x41, 0xE9, 0x74, 0x39, 0x27,
    0xC8, 0xD1, 0xA6, 0xB3, 0x78, 0x87, 0x2A, 0x72,
    0xC9, 0x44, 0x6C, 0x1F, 0x75, 0xE6, 0xDC, 0x7B,
    0x2D, 0xEF, 0x98, 0xBD, 0x0C, 0x21, 0x4B, 0xE6,
    0x70, 0x6D, 0x48, 0x79, 0x1F, 0x57, 0x68, 0x0A
};

// This is a production-shaped RFC 8785 result, including nested data,
// non-ASCII UTF-8, escaped quotes, and an escaped newline. Its expected hash
// and signature were produced independently with Node.js SHA-256 and Ed25519,
// not by the implementation under test.
constexpr char REALISTIC_CANONICAL_DATA[] =
    u8"{\"action\":\"MANUAL_DECISION\","
    u8"\"actor\":\"handlaggare@example.com\","
    u8"\"applicationId\":\"8f14e45f-ea4d-4c2f-a987-3f2dbcb4a671\","
    u8"\"data\":{\"actorType\":\"CASE_WORKER\","
    u8"\"comment\":\"Godk\u00E4nd efter komplettering.\\nKund skrev: \\\"Tack!\\\"\","
    u8"\"newStatus\":\"APPROVED\",\"previousStatus\":\"UNDER_REVIEW\","
    u8"\"workerName\":\"\u00C5sa \u00D6berg\"},"
    u8"\"eventId\":\"550e8400-e29b-41d4-a716-446655440000\","
    u8"\"occurredAt\":\"2026-10-28T10:15:30Z\","
    u8"\"previousHash\":\"0000000000000000000000000000000000000000000000000000000000000000\","
    u8"\"schemaVersion\":1,\"sequenceNumber\":1,"
    u8"\"signingKeyId\":\"audit-signing-key-2026-01\"}";

static_assert(sizeof(REALISTIC_CANONICAL_DATA) - 1 == 546,
              "The realistic known-answer bytes must remain unchanged");

constexpr std::array<uint8_t, resurs::audit::SHA256_HASH_BYTES>
REALISTIC_KNOWN_ANSWER_HASH = {
    0xFF, 0xA3, 0x52, 0x70, 0xBA, 0x49, 0x96, 0x22,
    0xE3, 0xAE, 0x0F, 0x5F, 0x25, 0x11, 0xC0, 0x68,
    0x17, 0x17, 0x2C, 0xCA, 0xBC, 0x60, 0xDC, 0x76,
    0x68, 0x6D, 0xEB, 0xB8, 0x5A, 0x1F, 0x8C, 0x73
};

constexpr std::array<uint8_t, resurs::audit::DIGITAL_SIGNATURE_BYTES>
REALISTIC_KNOWN_ANSWER_SIGNATURE = {
    0xA8, 0x3F, 0xC5, 0xDD, 0x3F, 0x59, 0xDC, 0xB3,
    0x69, 0x9A, 0x4A, 0xFD, 0x8A, 0x0B, 0x69, 0x9F,
    0x16, 0x3A, 0xCF, 0x00, 0xDC, 0xDB, 0x59, 0x62,
    0x56, 0xC6, 0xED, 0x8D, 0xD8, 0xF7, 0x93, 0x7B,
    0xD5, 0x3A, 0x98, 0x87, 0xF7, 0xE0, 0xAF, 0x5D,
    0x60, 0x02, 0x41, 0xED, 0x42, 0x12, 0xA3, 0xF5,
    0xD9, 0x16, 0x9D, 0x75, 0xF8, 0x19, 0xD1, 0xA9,
    0xBE, 0x05, 0x70, 0x9C, 0xD7, 0xB3, 0x6D, 0x05
};

AuditEntry make_known_answer_entry(const uint8_t* canonicalData)
{
    AuditEntry entry{};
    entry.canonicalData = canonicalData;
    entry.canonicalDataLength = KNOWN_ANSWER_CANONICAL_DATA.size();
    entry.sequenceNumber = 1;

    std::copy(
        KNOWN_ANSWER_HASH.begin(),
        KNOWN_ANSWER_HASH.end(),
        entry.currentHash
    );

    std::copy(
        KNOWN_ANSWER_SIGNATURE.begin(),
        KNOWN_ANSWER_SIGNATURE.end(),
        entry.signature
    );

    return entry;
}

// Builds a valid signed chain so tests can focus on changing one property and
// checking the resulting failure, instead of repeating chain setup each time.
// It also owns the canonical strings for as long as their entry pointers are used.
// Each signed object includes its link and sequence, matching the Java contract.
struct AuditChainFixture
{
    std::vector<std::string> canonicalData;
    std::vector<AuditEntry> entries;

    explicit AuditChainFixture(size_t entryCount)
        : canonicalData(entryCount), entries(entryCount)
    {
        constexpr char hex[] = "0123456789abcdef";
        for (size_t index = 0; index < entryCount; ++index) {
            auto &entry = entries[index];
            entry.sequenceNumber = index + 1;
            if (index > 0)
                std::copy(std::begin(entries[index - 1].currentHash),
                          std::end(entries[index - 1].currentHash), entry.previousHash);

            std::string previousHash;
            for (uint8_t byte : entry.previousHash) {
                previousHash.push_back(hex[byte >> 4]);
                previousHash.push_back(hex[byte & 0x0F]);
            }
            canonicalData[index] = "{\"previousHash\":\"" + previousHash +
                "\",\"sequenceNumber\":" + std::to_string(entry.sequenceNumber) +
                ",\"value\":\"entry-" + std::to_string(entry.sequenceNumber) + "\"}";
            entry.canonicalData = reinterpret_cast<const uint8_t *>(canonicalData[index].data());
            entry.canonicalDataLength = canonicalData[index].size();
            assert(wrapper_hash_and_sign(entry.canonicalData, entry.canonicalDataLength,
                       KNOWN_ANSWER_PRIVATE_KEY.data(), KNOWN_ANSWER_PRIVATE_KEY.size(),
                       entry.currentHash, sizeof(entry.currentHash),
                       entry.signature, sizeof(entry.signature)) == HASH_AND_SIGN_ALL_OK);
        }
    }

    // Copying the fixture would leave entry pointers referring to the old strings.
    AuditChainFixture(const AuditChainFixture &) = delete;
    AuditChainFixture &operator=(const AuditChainFixture &) = delete;
};

// Keeps chain assertions concise and consistently checks both the result code
// and the exact failing entry index, with a useful message if either is wrong.
void expect_chain_result(
    const std::vector<AuditEntry> &entries,
    VerifyChainResultCode expectedCode,
    uint64_t expectedIndex,
    const std::array<uint8_t, resurs::audit::PKEY_BYTES> &publicKey = KNOWN_ANSWER_PUBLIC_KEY)
{
    const auto result = wrapper_verify_chain(
        entries.data(), entries.size(), publicKey.data(), publicKey.size());
    if (result.result_code != expectedCode || result.index != expectedIndex) {
        throw std::runtime_error(
            "expected chain code " + std::to_string(expectedCode) +
            " at index " + std::to_string(expectedIndex) +
            ", got code " + std::to_string(result.result_code) +
            " at index " + std::to_string(result.index));
    }
}

// Surrounds a native output buffer with recognizable guard bytes. This helps
// detect writes before the buffer, beyond the expected output, or into unused
// capacity without changing the public native API being tested.
template <size_t Capacity>
struct GuardedBuffer
{
    static constexpr size_t guardSize = 8;
    static constexpr uint8_t sentinel = 0xA5;
    std::array<uint8_t, Capacity + 2 * guardSize> storage;

    GuardedBuffer() { storage.fill(sentinel); }
    uint8_t *data() { return storage.data() + guardSize; }

    void expect_boundaries(size_t writtenLength = Capacity) const
    {
        assert(writtenLength <= Capacity);
        assert(std::all_of(storage.begin(), storage.begin() + guardSize,
                          [](uint8_t byte) { return byte == sentinel; }));
        // Include unused output capacity as well as the suffix guard.
        assert(std::all_of(storage.begin() + guardSize + writtenLength, storage.end(),
                          [](uint8_t byte) { return byte == sentinel; }));
    }
};


// ============================================================
// PUBLIC BOUNDARY VALIDATION TESTS
// ============================================================

void test_generate_private_key_validation()
{
    std::array<uint8_t, resurs::audit::PKEY_BYTES> output{};
    std::array<uint8_t, resurs::audit::PKEY_BYTES + 1> largerOutput{};

    assert(
        generate_private_key(nullptr, output.size()) ==
        AUDIT_INVALID_ARGUMENT
    );
    assert(
        generate_private_key(output.data(), output.size() - 1) ==
        AUDIT_PRIVATE_KEY_BUFFER_TOO_SMALL
    );
    assert(
        generate_private_key(output.data(), output.size()) ==
        AUDIT_PRIVATE_KEY_OK
    );
    assert(
        generate_private_key(largerOutput.data(), largerOutput.size()) ==
        AUDIT_PRIVATE_KEY_OK
    );
}


void test_get_public_key_validation()
{
    auto privateKey = KNOWN_ANSWER_PRIVATE_KEY;
    std::array<uint8_t, resurs::audit::PKEY_BYTES> output{};
    std::array<uint8_t, resurs::audit::PKEY_BYTES + 1> largerOutput{};

    assert(
        ::get_public_key(
            nullptr,
            privateKey.size(),
            output.data(),
            output.size()
        ) == AUDIT_KEY_NULL_ARGUMENT
    );
    assert(
        ::get_public_key(
            privateKey.data(),
            privateKey.size(),
            nullptr,
            output.size()
        ) == AUDIT_KEY_NULL_ARGUMENT
    );
    assert(
        ::get_public_key(
            privateKey.data(),
            privateKey.size() - 1,
            output.data(),
            output.size()
        ) == AUDIT_KEY_INVALID_PRIVATE_KEY_SIZE
    );
    assert(
        ::get_public_key(
            privateKey.data(),
            privateKey.size() + 1,
            output.data(),
            output.size()
        ) == AUDIT_KEY_INVALID_PRIVATE_KEY_SIZE
    );
    assert(
        ::get_public_key(
            privateKey.data(),
            privateKey.size(),
            output.data(),
            output.size() - 1
        ) == AUDIT_KEY_OUTPUT_BUFFER_TOO_SMALL
    );
    assert(
        ::get_public_key(
            privateKey.data(),
            privateKey.size(),
            largerOutput.data(),
            largerOutput.size()
        ) == AUDIT_KEY_OK
    );
}


void test_hash_and_sign_validation()
{
    auto privateKey = KNOWN_ANSWER_PRIVATE_KEY;
    const auto* canonicalData = KNOWN_ANSWER_CANONICAL_DATA.data();
    const size_t canonicalDataLength = KNOWN_ANSWER_CANONICAL_DATA.size();
    std::array<uint8_t, resurs::audit::SHA256_HASH_BYTES> hash{};
    std::array<uint8_t, resurs::audit::DIGITAL_SIGNATURE_BYTES> signature{};

    assert(
        wrapper_hash_and_sign(
            nullptr,
            canonicalDataLength,
            privateKey.data(),
            privateKey.size(),
            hash.data(),
            hash.size(),
            signature.data(),
            signature.size()
        ) == HASH_AND_SIGN_INVALID_ARGUMENT
    );
    assert(
        wrapper_hash_and_sign(
            canonicalData,
            canonicalDataLength,
            nullptr,
            privateKey.size(),
            hash.data(),
            hash.size(),
            signature.data(),
            signature.size()
        ) == HASH_AND_SIGN_INVALID_ARGUMENT
    );
    assert(
        wrapper_hash_and_sign(
            canonicalData,
            canonicalDataLength,
            privateKey.data(),
            privateKey.size(),
            nullptr,
            hash.size(),
            signature.data(),
            signature.size()
        ) == HASH_AND_SIGN_INVALID_ARGUMENT
    );
    assert(
        wrapper_hash_and_sign(
            canonicalData,
            canonicalDataLength,
            privateKey.data(),
            privateKey.size(),
            hash.data(),
            hash.size(),
            nullptr,
            signature.size()
        ) == HASH_AND_SIGN_INVALID_ARGUMENT
    );
    assert(
        wrapper_hash_and_sign(
            canonicalData,
            0,
            privateKey.data(),
            privateKey.size(),
            hash.data(),
            hash.size(),
            signature.data(),
            signature.size()
        ) == HASH_AND_SIGN_CANONICAL_DATA_EMPTY
    );

    for (size_t invalidPrivateKeyLength : {
            privateKey.size() - 1,
            privateKey.size() + 1})
    {
        assert(
            wrapper_hash_and_sign(
                canonicalData,
                canonicalDataLength,
                privateKey.data(),
                invalidPrivateKeyLength,
                hash.data(),
                hash.size(),
                signature.data(),
                signature.size()
            ) == HASH_AND_SIGN_INVALID_BUFFER_SIZE
        );
    }

    for (size_t invalidHashLength : {hash.size() - 1, hash.size() + 1})
    {
        assert(
            wrapper_hash_and_sign(
                canonicalData,
                canonicalDataLength,
                privateKey.data(),
                privateKey.size(),
                hash.data(),
                invalidHashLength,
                signature.data(),
                signature.size()
            ) == HASH_AND_SIGN_INVALID_BUFFER_SIZE
        );
    }

    for (size_t invalidSignatureLength : {
            signature.size() - 1,
            signature.size() + 1})
    {
        assert(
            wrapper_hash_and_sign(
                canonicalData,
                canonicalDataLength,
                privateKey.data(),
                privateKey.size(),
                hash.data(),
                hash.size(),
                signature.data(),
                invalidSignatureLength
            ) == HASH_AND_SIGN_INVALID_BUFFER_SIZE
        );
    }
}


void test_verify_chain_argument_validation()
{
    AuditEntry entry = make_known_answer_entry(
        KNOWN_ANSWER_CANONICAL_DATA.data()
    );

    VerifyChainResult result = wrapper_verify_chain(
        nullptr,
        1,
        KNOWN_ANSWER_PUBLIC_KEY.data(),
        KNOWN_ANSWER_PUBLIC_KEY.size()
    );
    assert(result.result_code == VERIFY_CHAIN_INVALID_ARGUMENT);
    assert(result.index == 0);

    result = wrapper_verify_chain(
        &entry,
        1,
        nullptr,
        KNOWN_ANSWER_PUBLIC_KEY.size()
    );
    assert(result.result_code == VERIFY_CHAIN_INVALID_ARGUMENT);
    assert(result.index == 0);

    result = wrapper_verify_chain(
        &entry,
        0,
        KNOWN_ANSWER_PUBLIC_KEY.data(),
        KNOWN_ANSWER_PUBLIC_KEY.size()
    );
    assert(result.result_code == VERIFY_CHAIN_ENTRY_COUNT_EMPTY);
    assert(result.index == 0);

    for (size_t invalidPublicKeyLength : {
            KNOWN_ANSWER_PUBLIC_KEY.size() - 1,
            KNOWN_ANSWER_PUBLIC_KEY.size() + 1})
    {
        result = wrapper_verify_chain(
            &entry,
            1,
            KNOWN_ANSWER_PUBLIC_KEY.data(),
            invalidPublicKeyLength
        );
        assert(result.result_code == VERIFY_CHAIN_INVALID_PUBLIC_KEY_SIZE);
        assert(result.index == 0);
    }
}


void test_verify_chain_entry_validation_reports_index()
{
    AuditEntry entries[2] = {
        make_known_answer_entry(KNOWN_ANSWER_CANONICAL_DATA.data()),
        make_known_answer_entry(KNOWN_ANSWER_CANONICAL_DATA.data())
    };

    entries[1].canonicalData = nullptr;
    VerifyChainResult result = wrapper_verify_chain(
        entries,
        2,
        KNOWN_ANSWER_PUBLIC_KEY.data(),
        KNOWN_ANSWER_PUBLIC_KEY.size()
    );
    assert(result.result_code == VERIFY_CHAIN_CANONICAL_DATA_EMPTY);
    assert(result.index == 1);

    entries[1].canonicalData = KNOWN_ANSWER_CANONICAL_DATA.data();
    entries[1].canonicalDataLength = 0;
    result = wrapper_verify_chain(
        entries,
        2,
        KNOWN_ANSWER_PUBLIC_KEY.data(),
        KNOWN_ANSWER_PUBLIC_KEY.size()
    );
    assert(result.result_code == VERIFY_CHAIN_CANONICAL_DATA_LENGTH_EMPTY);
    assert(result.index == 1);
}


// ============================================================
// TEST 1
// ============================================================

void test_hash_is_deterministic()
{
    auto privateKey = get_private_key();

    const uint8_t data[] = {
        'H', 'e', 'l', 'l', 'o'
    };

    uint8_t hash1[resurs::audit::SHA256_HASH_BYTES]{};
    uint8_t hash2[resurs::audit::SHA256_HASH_BYTES]{};
    uint8_t signature1[resurs::audit::DIGITAL_SIGNATURE_BYTES]{};
    uint8_t signature2[resurs::audit::DIGITAL_SIGNATURE_BYTES]{};

    int result1 = wrapper_hash_and_sign(
        data,
        sizeof(data),
        privateKey.data(),
        privateKey.size(),
        hash1,
        sizeof(hash1),
        signature1,
        sizeof(signature1)
    );

    int result2 = wrapper_hash_and_sign(
        data,
        sizeof(data),
        privateKey.data(),
        privateKey.size(),
        hash2,
        sizeof(hash2),
        signature2,
        sizeof(signature2)
    );

    assert(result1 == 0);
    assert(result2 == 0);

    assert(
        std::memcmp(
            hash1,
            hash2,
            resurs::audit::SHA256_HASH_BYTES
        ) == 0
    );
}


// ============================================================
// TEST 2
// ============================================================

void test_different_data_produces_different_hash()
{
    auto privateKey = get_private_key();

    const uint8_t data1[] = {
        'H', 'e', 'l', 'l', 'o'
    };

    const uint8_t data2[] = {
        'H', 'e', 'l', 'l', 'p'
    };

    uint8_t hash1[resurs::audit::SHA256_HASH_BYTES]{};
    uint8_t hash2[resurs::audit::SHA256_HASH_BYTES]{};
    uint8_t signature1[resurs::audit::DIGITAL_SIGNATURE_BYTES]{};
    uint8_t signature2[resurs::audit::DIGITAL_SIGNATURE_BYTES]{};

    assert(
        wrapper_hash_and_sign(
            data1,
            sizeof(data1),
            privateKey.data(),
            privateKey.size(),
            hash1,
            sizeof(hash1),
            signature1,
            sizeof(signature2)
        ) == 0
    );

    assert(
        wrapper_hash_and_sign(
            data2,
            sizeof(data2),
            privateKey.data(),
            privateKey.size(),
            hash2,
            sizeof(hash2),
            signature2,
            sizeof(signature2)
        ) == 0
    );

    assert(
        std::memcmp(
            hash1,
            hash2,
            resurs::audit::SHA256_HASH_BYTES
        ) != 0
    );
}


// ============================================================
// TEST 3
// ============================================================

void test_hash_and_sign()
{
    auto privateKey = get_private_key();

    const uint8_t data[] = {
        'T', 'e', 's', 't', ' ', 'd', 'a', 't', 'a'
    };

    uint8_t hash[
        resurs::audit::SHA256_HASH_BYTES
    ]{};

    uint8_t signature[
        resurs::audit::DIGITAL_SIGNATURE_BYTES
    ]{};

    int result = wrapper_hash_and_sign(
        data,
        sizeof(data),
        privateKey.data(),
        privateKey.size(),
        hash,
        sizeof(hash),
        signature,
        sizeof(signature)
    );

    assert(result == 0);

    // Kontrollera att hash inte är tom.
    bool hashIsZero = true;

    for (uint8_t byte : hash)
    {
        if (byte != 0)
        {
            hashIsZero = false;
            break;
        }
    }

    assert(!hashIsZero);

    // Controll that the signature is not empty
    bool signatureIsZero = true;

    for (uint8_t byte : signature)
    {
        if (byte != 0)
        {
            signatureIsZero = false;
            break;
        }
    }

    assert(!signatureIsZero);
}


// ============================================================
// TEST 4
// ============================================================

void test_signature_verification()
{
    auto privateKey = get_private_key();
    auto publicKey = get_public_key(privateKey);

    uint8_t data[] = {
        'A', 'u', 'd', 'i', 't'
    };

    AuditEntry entry{};

    assert(
        wrapper_hash_and_sign(
            data,
            sizeof(data),
            privateKey.data(),
            privateKey.size(),
            entry.currentHash,
            sizeof(entry.currentHash),
            entry.signature,
            sizeof(entry.signature)
        ) == 0
    );

    // First entry must have a zero previous hash.
    std::memset(
        entry.previousHash,
        0,
        sizeof(entry.previousHash)
    );

    entry.canonicalData = data;
    entry.canonicalDataLength = sizeof(data);
    entry.sequenceNumber = 1;

    VerifyChainResult result =
        wrapper_verify_chain(
            &entry,
            1,
            publicKey.data(),
            publicKey.size()
        );

    assert(result.result_code == VERIFY_CHAIN_ALL_OK);
}


// ============================================================
// TEST 5
// ============================================================

void test_modified_data_is_detected()
{
    auto privateKey = get_private_key();
    auto publicKey = get_public_key(privateKey);

    uint8_t data[] = {
        'A', 'u', 'd', 'i', 't'
    };

    AuditEntry entry{};

    assert(
        wrapper_hash_and_sign(
            data,
            sizeof(data),
            privateKey.data(),
            privateKey.size(),
            entry.currentHash,
            sizeof(entry.currentHash),
            entry.signature,
            sizeof(entry.signature)
        ) == 0
    );

    std::memset(
        entry.previousHash,
        0,
        sizeof(entry.previousHash)
    );

    entry.canonicalData = data;
    entry.canonicalDataLength = sizeof(data);
    entry.sequenceNumber = 1;

    // ----------------------------------------
    // ATTACK / MANIPULATION
    // ----------------------------------------

    data[0] = 'X';

    VerifyChainResult result =
        wrapper_verify_chain(
            &entry,
            1,
            publicKey.data(),
            publicKey.size()
        );

    assert(
        result.result_code == CURRENT_HASH_MISSMATCH
    );

    assert(result.index == 0);
}


// ============================================================
// TEST 6
// ============================================================

void test_modified_signature_is_detected()
{
    auto privateKey = get_private_key();
    auto publicKey = get_public_key(privateKey);

    uint8_t data[] = {
        'A', 'u', 'd', 'i', 't'
    };

    AuditEntry entry{};

    assert(
        wrapper_hash_and_sign(
            data,
            sizeof(data),
            privateKey.data(),
            privateKey.size(),
            entry.currentHash,
            sizeof(entry.currentHash),
            entry.signature,
            sizeof(entry.signature)
        ) == 0
    );

    std::memset(
        entry.previousHash,
        0,
        sizeof(entry.previousHash)
    );

    entry.canonicalData = data;
    entry.canonicalDataLength = sizeof(data);
    entry.sequenceNumber = 1;

    // ----------------------------------------
    // ATTACK / MANIPULATION
    // ----------------------------------------

    entry.signature[0] ^= 0x01;

    VerifyChainResult result =
        wrapper_verify_chain(
            &entry,
            1,
            publicKey.data(),
            publicKey.size()
        );

    assert(
        result.result_code == EVP_DIGEST_VERIFY_FAILED
    );

    assert(result.index == 0);
}


// ============================================================
// TEST 7
// ============================================================

void test_invalid_previous_hash_is_detected()
{
    auto privateKey = get_private_key();
    auto publicKey = get_public_key(privateKey);

    uint8_t data[] = {
        'T', 'e', 's', 't'
    };

    AuditEntry entry{};

    assert(
        wrapper_hash_and_sign(
            data,
            sizeof(data),
            privateKey.data(),
            privateKey.size(),
            entry.currentHash,
            sizeof(entry.currentHash),
            entry.signature,
            sizeof(entry.signature)
        ) == 0
    );

    entry.canonicalData = data;
    entry.canonicalDataLength = sizeof(data);
    entry.sequenceNumber = 1;

    // First entry should have all-zero previousHash.
    // Instead, deliberately make it invalid.
    std::memset(
        entry.previousHash,
        0xFF,
        sizeof(entry.previousHash)
    );

    VerifyChainResult result =
        wrapper_verify_chain(
            &entry,
            1,
            publicKey.data(),
            publicKey.size()
        );

    assert(
        result.result_code == PREVIOUS_HASH_MISSMATCH
    );

    assert(result.index == 0);
}


// ============================================================
// TEST 8
// ============================================================

void test_valid_two_entry_chain()
{
    auto privateKey = get_private_key();
    auto publicKey = get_public_key(privateKey);

    uint8_t data1[] = {
        'F', 'i', 'r', 's', 't'
    };

    uint8_t data2[] = {
        'S', 'e', 'c', 'o', 'n', 'd'
    };

    AuditEntry entries[2]{};


    // ========================================================
    // ENTRY 1
    // ========================================================

    entries[0].canonicalData = data1;
    entries[0].canonicalDataLength = sizeof(data1);
    entries[0].sequenceNumber = 1;

    std::memset(
        entries[0].previousHash,
        0,
        sizeof(entries[0].previousHash)
    );

    assert(
        wrapper_hash_and_sign(
            data1,
            sizeof(data1),
            privateKey.data(),
            privateKey.size(),
            entries[0].currentHash,
            sizeof(entries[0].currentHash),
            entries[0].signature,
            sizeof(entries[0].signature)
        ) == 0
    );


    // ========================================================
    // ENTRY 2
    // ========================================================

    entries[1].canonicalData = data2;
    entries[1].canonicalDataLength = sizeof(data2);
    entries[1].sequenceNumber = 2;

    // Entry 2 points to Entry 1.
    std::memcpy(
        entries[1].previousHash,
        entries[0].currentHash,
        sizeof(entries[1].previousHash)
    );

    assert(
        wrapper_hash_and_sign(
            data2,
            sizeof(data2),
            privateKey.data(),
            privateKey.size(),
            entries[1].currentHash,
            sizeof(entries[1].currentHash),
            entries[1].signature,
            sizeof(entries[1].signature)
        ) == 0
    );


    // ========================================================
    // VERIFY ENTIRE CHAIN
    // ========================================================

    VerifyChainResult result =
        wrapper_verify_chain(
            entries,
            2,
            publicKey.data(),
            publicKey.size()
        );

    assert(result.result_code == VERIFY_CHAIN_ALL_OK);
}


// ============================================================
// TEST 9
// ============================================================

void test_modified_previous_hash_in_chain()
{
    auto privateKey = get_private_key();
    auto publicKey = get_public_key(privateKey);

    uint8_t data1[] = {
        'F', 'i', 'r', 's', 't'
    };

    uint8_t data2[] = {
        'S', 'e', 'c', 'o', 'n', 'd'
    };

    AuditEntry entries[2]{};


    // ENTRY 1
    entries[0].canonicalData = data1;
    entries[0].canonicalDataLength = sizeof(data1);
    entries[0].sequenceNumber = 1;

    std::memset(
        entries[0].previousHash,
        0,
        sizeof(entries[0].previousHash)
    );

    assert(
        wrapper_hash_and_sign(
            data1,
            sizeof(data1),
            privateKey.data(),
            privateKey.size(),
            entries[0].currentHash,
            sizeof(entries[0].currentHash),
            entries[0].signature,
            sizeof(entries[0].signature)
        ) == 0
    );


    // ENTRY 2
    entries[1].canonicalData = data2;
    entries[1].canonicalDataLength = sizeof(data2);
    entries[1].sequenceNumber = 2;

    std::memcpy(
        entries[1].previousHash,
        entries[0].currentHash,
        sizeof(entries[1].previousHash)
    );

    assert(
        wrapper_hash_and_sign(
            data2,
            sizeof(data2),
            privateKey.data(),
            privateKey.size(),
            entries[1].currentHash,
            sizeof(entries[1].currentHash),
            entries[1].signature,
            sizeof(entries[1].signature)

        ) == 0
    );


    // ----------------------------------------
    // MANIPULATE THE CHAIN
    // ----------------------------------------

    entries[1].previousHash[0] ^= 0x01;


    VerifyChainResult result =
        wrapper_verify_chain(
            entries,
            2,
            publicKey.data(),
            publicKey.size()
        );

    assert(
        result.result_code == PREVIOUS_HASH_MISSMATCH
    );

    assert(result.index == 1);
}


// ============================================================
// KNOWN-ANSWER TESTS
// ============================================================

void test_rfc_8032_public_key_derivation()
{
    auto privateKey = KNOWN_ANSWER_PRIVATE_KEY;
    std::array<uint8_t, resurs::audit::PKEY_BYTES> publicKey{};

    assert(
        ::get_public_key(
            privateKey.data(),
            privateKey.size(),
            publicKey.data(),
            publicKey.size()
        ) == AUDIT_KEY_OK
    );

    assert(publicKey == KNOWN_ANSWER_PUBLIC_KEY);
}


void test_known_answer_hash_and_signature()
{
    auto privateKey = KNOWN_ANSWER_PRIVATE_KEY;
    std::array<uint8_t, resurs::audit::SHA256_HASH_BYTES> hash{};
    std::array<uint8_t, resurs::audit::DIGITAL_SIGNATURE_BYTES> signature{};

    assert(
        wrapper_hash_and_sign(
            KNOWN_ANSWER_CANONICAL_DATA.data(),
            KNOWN_ANSWER_CANONICAL_DATA.size(),
            privateKey.data(),
            privateKey.size(),
            hash.data(),
            sizeof(hash),
            signature.data(),
            sizeof(signature)
        ) == 0
    );

    assert(hash == KNOWN_ANSWER_HASH);
    assert(signature == KNOWN_ANSWER_SIGNATURE);
}


void test_known_answer_entry_verifies()
{
    AuditEntry entry = make_known_answer_entry(
        KNOWN_ANSWER_CANONICAL_DATA.data()
    );

    VerifyChainResult result = wrapper_verify_chain(
        &entry,
        1,
        KNOWN_ANSWER_PUBLIC_KEY.data(),
        KNOWN_ANSWER_PUBLIC_KEY.size()
    );

    assert(result.result_code == VERIFY_CHAIN_ALL_OK);
}


void test_known_answer_modified_data_is_detected()
{
    auto modifiedData = KNOWN_ANSWER_CANONICAL_DATA;
    AuditEntry entry = make_known_answer_entry(modifiedData.data());
    modifiedData[0] ^= 0x01;

    VerifyChainResult result = wrapper_verify_chain(
        &entry,
        1,
        KNOWN_ANSWER_PUBLIC_KEY.data(),
        KNOWN_ANSWER_PUBLIC_KEY.size()
    );

    assert(result.result_code == CURRENT_HASH_MISSMATCH);
    assert(result.index == 0);
}


void test_known_answer_modified_hash_is_detected()
{
    AuditEntry entry = make_known_answer_entry(
        KNOWN_ANSWER_CANONICAL_DATA.data()
    );
    entry.currentHash[0] ^= 0x01;

    VerifyChainResult result = wrapper_verify_chain(
        &entry,
        1,
        KNOWN_ANSWER_PUBLIC_KEY.data(),
        KNOWN_ANSWER_PUBLIC_KEY.size()
    );

    assert(result.result_code == CURRENT_HASH_MISSMATCH);
    assert(result.index == 0);
}


void test_known_answer_modified_signature_is_detected()
{
    AuditEntry entry = make_known_answer_entry(
        KNOWN_ANSWER_CANONICAL_DATA.data()
    );
    entry.signature[0] ^= 0x01;

    VerifyChainResult result = wrapper_verify_chain(
        &entry,
        1,
        KNOWN_ANSWER_PUBLIC_KEY.data(),
        KNOWN_ANSWER_PUBLIC_KEY.size()
    );

    assert(result.result_code == EVP_DIGEST_VERIFY_FAILED);
    assert(result.index == 0);
}


void test_known_answer_modified_previous_hash_is_detected()
{
    AuditEntry entry = make_known_answer_entry(
        KNOWN_ANSWER_CANONICAL_DATA.data()
    );
    entry.previousHash[0] ^= 0x01;

    VerifyChainResult result = wrapper_verify_chain(
        &entry,
        1,
        KNOWN_ANSWER_PUBLIC_KEY.data(),
        KNOWN_ANSWER_PUBLIC_KEY.size()
    );

    assert(result.result_code == PREVIOUS_HASH_MISSMATCH);
    assert(result.index == 0);
}


void test_sequence_number_must_start_at_one()
{
    AuditEntry entry = make_known_answer_entry(
        KNOWN_ANSWER_CANONICAL_DATA.data()
    );
    entry.sequenceNumber = 2;

    VerifyChainResult result = wrapper_verify_chain(
        &entry,
        1,
        KNOWN_ANSWER_PUBLIC_KEY.data(),
        KNOWN_ANSWER_PUBLIC_KEY.size()
    );

    assert(result.result_code == SEQUENCE_NUMBER_MISSMATCH);
    assert(result.index == 0);
}


void test_sequence_number_gap_is_detected()
{
    AuditEntry entries[2] = {
        make_known_answer_entry(KNOWN_ANSWER_CANONICAL_DATA.data()),
        make_known_answer_entry(KNOWN_ANSWER_CANONICAL_DATA.data())
    };

    std::copy(
        KNOWN_ANSWER_HASH.begin(),
        KNOWN_ANSWER_HASH.end(),
        entries[1].previousHash
    );
    entries[1].sequenceNumber = 3;

    VerifyChainResult result = wrapper_verify_chain(
        entries,
        2,
        KNOWN_ANSWER_PUBLIC_KEY.data(),
        KNOWN_ANSWER_PUBLIC_KEY.size()
    );

    assert(result.result_code == SEQUENCE_NUMBER_MISSMATCH);
    assert(result.index == 1);
}


// ============================================================
// EXTENDED CHAIN AND BUFFER TESTS
// ============================================================

void test_long_chain_corruption_reports_exact_index()
{
    AuditChainFixture chain(64);
    expect_chain_result(chain.entries, VERIFY_CHAIN_ALL_OK, 0);

    for (const size_t index : {size_t{0}, chain.entries.size() / 2, chain.entries.size() - 1}) {
        auto entries = chain.entries;
        auto modifiedData = chain.canonicalData[index];
        modifiedData.back() ^= 0x01;
        entries[index].canonicalData = reinterpret_cast<const uint8_t *>(modifiedData.data());
        expect_chain_result(entries, CURRENT_HASH_MISSMATCH, index);

        entries = chain.entries;
        entries[index].currentHash[0] ^= 0x01;
        expect_chain_result(entries, CURRENT_HASH_MISSMATCH, index);

        entries = chain.entries;
        entries[index].signature[0] ^= 0x01;
        expect_chain_result(entries, EVP_DIGEST_VERIFY_FAILED, index);

        entries = chain.entries;
        entries[index].previousHash[0] ^= 0x01;
        expect_chain_result(entries, PREVIOUS_HASH_MISSMATCH, index);

        entries = chain.entries;
        ++entries[index].sequenceNumber;
        expect_chain_result(entries, SEQUENCE_NUMBER_MISSMATCH, index);
    }
}

void test_removed_duplicated_and_reordered_entries_are_detected()
{
    AuditChainFixture chain(8);
    expect_chain_result(chain.entries, VERIFY_CHAIN_ALL_OK, 0);

    auto entries = chain.entries;
    entries.erase(entries.begin());
    expect_chain_result(entries, PREVIOUS_HASH_MISSMATCH, 0);

    entries = chain.entries;
    entries.erase(entries.begin() + 3);
    expect_chain_result(entries, PREVIOUS_HASH_MISSMATCH, 3);

    entries = chain.entries;
    const auto duplicate = entries[2];
    entries.insert(entries.begin() + 3, duplicate);
    expect_chain_result(entries, PREVIOUS_HASH_MISSMATCH, 3);

    entries = chain.entries;
    std::swap(entries[3], entries[4]);
    expect_chain_result(entries, PREVIOUS_HASH_MISSMATCH, 3);
}

void test_wrong_public_key_and_mixed_signers_are_rejected()
{
    AuditChainFixture chain(8);
    expect_chain_result(chain.entries, VERIFY_CHAIN_ALL_OK, 0);

    // A different valid Ed25519 seed tests signer identity, not key-length validation.
    auto otherPrivateKey = KNOWN_ANSWER_PRIVATE_KEY;
    otherPrivateKey[0] ^= 0x01;
    const auto otherPublicKey = get_public_key(otherPrivateKey);
    assert(otherPublicKey != KNOWN_ANSWER_PUBLIC_KEY);
    expect_chain_result(chain.entries, EVP_DIGEST_VERIFY_FAILED, 0, otherPublicKey);

    auto entries = chain.entries;
    auto &entry = entries[4];
    assert(wrapper_hash_and_sign(entry.canonicalData, entry.canonicalDataLength,
               otherPrivateKey.data(), otherPrivateKey.size(),
               entry.currentHash, sizeof(entry.currentHash),
               entry.signature, sizeof(entry.signature)) == HASH_AND_SIGN_ALL_OK);
    // The hash and subsequent links are unchanged: only the signer is different.
    assert(std::equal(std::begin(entry.currentHash), std::end(entry.currentHash),
                      chain.entries[4].currentHash));
    expect_chain_result(entries, EVP_DIGEST_VERIFY_FAILED, 4);
}

void test_valid_prefix_cannot_detect_tail_truncation()
{
    AuditChainFixture chain(8);
    expect_chain_result(chain.entries, VERIFY_CHAIN_ALL_OK, 0);

    // The API receives only the supplied entries, with no trusted expected count
    // or final hash. Removing the tail leaves a valid prefix, not a detectable gap.
    // This documents a limitation, not a claim that the original log is complete.
    auto prefix = chain.entries;
    prefix.pop_back();
    expect_chain_result(prefix, VERIFY_CHAIN_ALL_OK, 0);
    prefix.resize(1);
    expect_chain_result(prefix, VERIFY_CHAIN_ALL_OK, 0);
}

void test_binary_payload_uses_explicit_byte_length()
{
    // Native treats data as opaque bytes, even though production callers pass JSON.
    // The fixed digest was independently calculated with .NET SHA256 over all six
    // bytes, so matching truncation bugs in signing and verification cannot cancel.
    std::array<uint8_t, 6> payload = {0x61, 0x00, 0x62, 0xFF, 0x00, 0x63};
    constexpr std::array<uint8_t, resurs::audit::SHA256_HASH_BYTES> expectedHash = {
        0x21, 0x5E, 0x82, 0xFF, 0xAE, 0x12, 0x17, 0xDF,
        0x8F, 0x64, 0xDC, 0xD7, 0x28, 0xB0, 0xB7, 0xD4,
        0x43, 0xC5, 0x9E, 0x68, 0xE1, 0xEF, 0x3E, 0xE0,
        0x08, 0x21, 0xB2, 0x51, 0xFC, 0x27, 0x0B, 0xCA
    };
    AuditEntry entry{};
    entry.canonicalData = payload.data();
    entry.canonicalDataLength = payload.size();
    entry.sequenceNumber = 1;
    assert(wrapper_hash_and_sign(payload.data(), payload.size(),
               KNOWN_ANSWER_PRIVATE_KEY.data(), KNOWN_ANSWER_PRIVATE_KEY.size(),
               entry.currentHash, sizeof(entry.currentHash),
               entry.signature, sizeof(entry.signature)) == HASH_AND_SIGN_ALL_OK);
    assert(std::equal(expectedHash.begin(), expectedHash.end(), entry.currentHash));
    expect_chain_result({entry}, VERIFY_CHAIN_ALL_OK, 0);

    payload[2] ^= 0x01; // A changed byte after the first null must still be detected.
    expect_chain_result({entry}, CURRENT_HASH_MISSMATCH, 0);
    payload[2] ^= 0x01;
    --entry.canonicalDataLength;
    expect_chain_result({entry}, CURRENT_HASH_MISSMATCH, 0);
}

void test_audit_output_buffers_preserve_boundaries()
{
    GuardedBuffer<resurs::audit::PKEY_BYTES + 8> privateKey;
    GuardedBuffer<resurs::audit::PKEY_BYTES + 8> publicKey;
    GuardedBuffer<resurs::audit::SHA256_HASH_BYTES> hash;
    GuardedBuffer<resurs::audit::DIGITAL_SIGNATURE_BYTES> signature;

    // Key functions accept excess capacity but must write only the 32-byte key.
    assert(generate_private_key(privateKey.data(), resurs::audit::PKEY_BYTES + 8) == AUDIT_PRIVATE_KEY_OK);
    privateKey.expect_boundaries(resurs::audit::PKEY_BYTES);
    assert(::get_public_key(privateKey.data(), resurs::audit::PKEY_BYTES,
               publicKey.data(), resurs::audit::PKEY_BYTES + 8) == AUDIT_KEY_OK);
    publicKey.expect_boundaries(resurs::audit::PKEY_BYTES);

    assert(wrapper_hash_and_sign(KNOWN_ANSWER_CANONICAL_DATA.data(), KNOWN_ANSWER_CANONICAL_DATA.size(),
               privateKey.data(), resurs::audit::PKEY_BYTES,
               hash.data(), resurs::audit::SHA256_HASH_BYTES,
               signature.data(), resurs::audit::DIGITAL_SIGNATURE_BYTES) == HASH_AND_SIGN_ALL_OK);
    hash.expect_boundaries();
    signature.expect_boundaries();

    // Also verify the guarded outputs are usable, not just within their boundaries.
    AuditEntry entry{};
    entry.canonicalData = KNOWN_ANSWER_CANONICAL_DATA.data();
    entry.canonicalDataLength = KNOWN_ANSWER_CANONICAL_DATA.size();
    entry.sequenceNumber = 1;
    std::copy_n(hash.data(), sizeof(entry.currentHash), entry.currentHash);
    std::copy_n(signature.data(), sizeof(entry.signature), entry.signature);
    const auto result = wrapper_verify_chain(&entry, 1, publicKey.data(), resurs::audit::PKEY_BYTES);
    assert(result.result_code == VERIFY_CHAIN_ALL_OK);
    assert(result.index == 0);
    privateKey.expect_boundaries(resurs::audit::PKEY_BYTES);
    publicKey.expect_boundaries(resurs::audit::PKEY_BYTES);
    hash.expect_boundaries();
    signature.expect_boundaries();

    // Rejected short buffers must not be overrun either. They are genuinely short
    // allocations inside the guards, rather than full-size buffers with false lengths.
    GuardedBuffer<resurs::audit::PKEY_BYTES - 1> shortKey;
    GuardedBuffer<resurs::audit::SHA256_HASH_BYTES - 1> shortHash;
    GuardedBuffer<resurs::audit::DIGITAL_SIGNATURE_BYTES - 1> shortSignature;
    assert(generate_private_key(shortKey.data(), resurs::audit::PKEY_BYTES - 1) == AUDIT_PRIVATE_KEY_BUFFER_TOO_SMALL);
    assert(::get_public_key(privateKey.data(), resurs::audit::PKEY_BYTES,
               shortKey.data(), resurs::audit::PKEY_BYTES - 1) == AUDIT_KEY_OUTPUT_BUFFER_TOO_SMALL);
    assert(wrapper_hash_and_sign(KNOWN_ANSWER_CANONICAL_DATA.data(), KNOWN_ANSWER_CANONICAL_DATA.size(),
               privateKey.data(), resurs::audit::PKEY_BYTES,
               shortHash.data(), resurs::audit::SHA256_HASH_BYTES - 1,
               signature.data(), resurs::audit::DIGITAL_SIGNATURE_BYTES) == HASH_AND_SIGN_INVALID_BUFFER_SIZE);
    assert(wrapper_hash_and_sign(KNOWN_ANSWER_CANONICAL_DATA.data(), KNOWN_ANSWER_CANONICAL_DATA.size(),
               privateKey.data(), resurs::audit::PKEY_BYTES,
               hash.data(), resurs::audit::SHA256_HASH_BYTES,
               shortSignature.data(), resurs::audit::DIGITAL_SIGNATURE_BYTES - 1) == HASH_AND_SIGN_INVALID_BUFFER_SIZE);
    shortKey.expect_boundaries();
    shortHash.expect_boundaries();
    shortSignature.expect_boundaries();
    hash.expect_boundaries();
    signature.expect_boundaries();
}

void test_realistic_canonical_entry_known_answer()
{
    const auto *canonicalData =
        reinterpret_cast<const uint8_t *>(REALISTIC_CANONICAL_DATA);
    constexpr size_t canonicalDataLength = sizeof(REALISTIC_CANONICAL_DATA) - 1;
    std::array<uint8_t, resurs::audit::SHA256_HASH_BYTES> hash{};
    std::array<uint8_t, resurs::audit::DIGITAL_SIGNATURE_BYTES> signature{};

    assert(wrapper_hash_and_sign(
               canonicalData, canonicalDataLength,
               KNOWN_ANSWER_PRIVATE_KEY.data(), KNOWN_ANSWER_PRIVATE_KEY.size(),
               hash.data(), hash.size(), signature.data(), signature.size()) ==
           HASH_AND_SIGN_ALL_OK);
    assert(hash == REALISTIC_KNOWN_ANSWER_HASH);
    assert(signature == REALISTIC_KNOWN_ANSWER_SIGNATURE);

    AuditEntry entry{};
    entry.canonicalData = canonicalData;
    entry.canonicalDataLength = canonicalDataLength;
    entry.sequenceNumber = 1;
    std::copy(hash.begin(), hash.end(), entry.currentHash);
    std::copy(signature.begin(), signature.end(), entry.signature);
    expect_chain_result({entry}, VERIFY_CHAIN_ALL_OK, 0);
}

void test_large_realistic_entry_is_processed_completely()
{
    std::string canonicalData =
        "{\"action\":\"COMMENT_ADDED\",\"actor\":\"worker@example.com\","
        "\"data\":{\"actorType\":\"CASE_WORKER\",\"comment\":\"";
    canonicalData.append(64 * 1024, 'x');
    canonicalData +=
        "\",\"workerName\":\"Load Test\"},"
        "\"previousHash\":\"0000000000000000000000000000000000000000000000000000000000000000\","
        "\"schemaVersion\":1,\"sequenceNumber\":1}";
    assert(canonicalData.size() > 64 * 1024);

    AuditEntry entry{};
    entry.canonicalData = reinterpret_cast<const uint8_t *>(canonicalData.data());
    entry.canonicalDataLength = canonicalData.size();
    entry.sequenceNumber = 1;
    assert(wrapper_hash_and_sign(
               entry.canonicalData, entry.canonicalDataLength,
               KNOWN_ANSWER_PRIVATE_KEY.data(), KNOWN_ANSWER_PRIVATE_KEY.size(),
               entry.currentHash, sizeof(entry.currentHash),
               entry.signature, sizeof(entry.signature)) == HASH_AND_SIGN_ALL_OK);
    expect_chain_result({entry}, VERIFY_CHAIN_ALL_OK, 0);

    // A change near the end proves the complete byte range is hashed.
    canonicalData[canonicalData.size() - 2] ^= 0x01;
    expect_chain_result({entry}, CURRENT_HASH_MISSMATCH, 0);
}

void test_public_abi_layout_matches_jna_contract()
{
    // The documented JNA mapping is specifically for a 64-bit process.
    assert(sizeof(void *) == 8);
    assert(sizeof(size_t) == 8);
    assert(std::is_standard_layout<AuditEntry>::value);
    assert(std::is_standard_layout<VerifyChainResult>::value);

    assert(sizeof(GeneratePrivateKeyResultCode) == 4);
    assert(sizeof(GetPublicKeyResultCode) == 4);
    assert(sizeof(HashAndSignResultCode) == 4);
    assert(sizeof(VerifyChainResultCode) == 4);

    assert(sizeof(AuditEntry) == 152);
    assert(alignof(AuditEntry) == 8);
    assert(offsetof(AuditEntry, canonicalData) == 0);
    assert(offsetof(AuditEntry, canonicalDataLength) == 8);
    assert(offsetof(AuditEntry, signature) == 16);
    assert(offsetof(AuditEntry, previousHash) == 80);
    assert(offsetof(AuditEntry, currentHash) == 112);
    assert(offsetof(AuditEntry, sequenceNumber) == 144);

    assert(sizeof(VerifyChainResult) == 16);
    assert(alignof(VerifyChainResult) == 8);
    assert(offsetof(VerifyChainResult, result_code) == 0);
    assert(offsetof(VerifyChainResult, index) == 8);
}


// ============================================================
// TEST RUNNER
// ============================================================

int main()
{
    int passed = 0;
    int failed = 0;

    auto run_test = [&](const char* name, auto test)
    {
        try
        {
            test();

            std::cout
                << "[PASS] "
                << name
                << '\n';

            passed++;
        }
        catch (const std::exception& error)
        {
            std::cout
                << "[FAIL] "
                << name
                << ": "
                << error.what()
                << '\n';

            failed++;
        }
        catch (...)
        {
            std::cout
                << "[FAIL] "
                << name
                << '\n';

            failed++;
        }
    };


    run_test(
        "private-key generation validates arguments",
        test_generate_private_key_validation
    );

    run_test(
        "public-key derivation validates arguments",
        test_get_public_key_validation
    );

    run_test(
        "hash and sign validates arguments",
        test_hash_and_sign_validation
    );

    run_test(
        "chain verification validates arguments",
        test_verify_chain_argument_validation
    );

    run_test(
        "chain entry validation reports its index",
        test_verify_chain_entry_validation_reports_index
    );

    run_test(
        "hash is deterministic",
        test_hash_is_deterministic
    );

    run_test(
        "different data produces different hash",
        test_different_data_produces_different_hash
    );

    run_test(
        "hash and sign",
        test_hash_and_sign
    );

    run_test(
        "signature verification",
        test_signature_verification
    );

    run_test(
        "modified data is detected",
        test_modified_data_is_detected
    );

    run_test(
        "modified signature is detected",
        test_modified_signature_is_detected
    );

    run_test(
        "invalid previous hash is detected",
        test_invalid_previous_hash_is_detected
    );

    run_test(
        "valid two-entry chain",
        test_valid_two_entry_chain
    );

    run_test(
        "modified previous hash in chain",
        test_modified_previous_hash_in_chain
    );

    run_test(
        "RFC 8032 public key derivation",
        test_rfc_8032_public_key_derivation
    );

    run_test(
        "known-answer hash and signature",
        test_known_answer_hash_and_signature
    );

    run_test(
        "known-answer entry verifies",
        test_known_answer_entry_verifies
    );

    run_test(
        "known-answer modified data is detected",
        test_known_answer_modified_data_is_detected
    );

    run_test(
        "known-answer modified hash is detected",
        test_known_answer_modified_hash_is_detected
    );

    run_test(
        "known-answer modified signature is detected",
        test_known_answer_modified_signature_is_detected
    );

    run_test(
        "known-answer modified previous hash is detected",
        test_known_answer_modified_previous_hash_is_detected
    );

    run_test(
        "sequence number must start at one",
        test_sequence_number_must_start_at_one
    );

    run_test(
        "sequence number gap is detected",
        test_sequence_number_gap_is_detected
    );

    run_test(
        "long-chain corruption reports the exact index",
        test_long_chain_corruption_reports_exact_index
    );

    run_test(
        "removed, duplicated, and reordered entries are detected",
        test_removed_duplicated_and_reordered_entries_are_detected
    );

    run_test(
        "wrong public key and mixed signers are rejected",
        test_wrong_public_key_and_mixed_signers_are_rejected
    );

    run_test(
        "valid prefix cannot detect tail truncation",
        test_valid_prefix_cannot_detect_tail_truncation
    );

    run_test(
        "binary payload uses its explicit byte length",
        test_binary_payload_uses_explicit_byte_length
    );

    run_test(
        "audit output buffers preserve boundaries",
        test_audit_output_buffers_preserve_boundaries
    );

    run_test(
        "realistic canonical entry matches independent known answers",
        test_realistic_canonical_entry_known_answer
    );

    run_test(
        "large realistic entry is processed completely",
        test_large_realistic_entry_is_processed_completely
    );

    run_test(
        "public ABI layout matches the JNA contract",
        test_public_abi_layout_matches_jna_contract
    );


    std::cout << "\n";
    std::cout
        << passed
        << "/"
        << (passed + failed)
        << " tests passed\n";


    return failed == 0 ? 0 : 1;
}
