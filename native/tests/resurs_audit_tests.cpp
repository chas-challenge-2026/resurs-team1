#include "wrapper.h"
#include "resurs_audit.h"
#include "audit_types.h"

#include <openssl/evp.h>

#include <algorithm>
#include <array>
#include <cassert>
#include <cstring>
#include <iostream>


// ============================================================
// TEST HELPERS
// ============================================================

std::array<uint8_t, resurs::audit::PKEY_BYTES>
get_private_key()
{
    DigitalSign digSign;
    return digSign.generate_private_key();
}


std::array<uint8_t, resurs::audit::PKEY_BYTES>
get_public_key(
    const std::array<uint8_t, resurs::audit::PKEY_BYTES>& privateKey)
{
    std::array<uint8_t, resurs::audit::PKEY_BYTES> publicKey{};

    EVP_PKEY* pkey = EVP_PKEY_new_raw_private_key_ex(
        nullptr,
        "ED25519",
        nullptr,
        privateKey.data(),
        privateKey.size()
    );

    assert(pkey != nullptr);

    size_t publicKeyLength = publicKey.size();

    int result = EVP_PKEY_get_raw_public_key(
        pkey,
        publicKey.data(),
        &publicKeyLength
    );

    assert(result == 1);
    assert(publicKeyLength == publicKey.size());

    EVP_PKEY_free(pkey);

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


// ============================================================
// TEST 1
// ============================================================

void test_hash_is_deterministic()
{
    const uint8_t data[] = {
        'H', 'e', 'l', 'l', 'o'
    };

    uint8_t hash1[resurs::audit::SHA256_HASH_BYTES]{};
    uint8_t hash2[resurs::audit::SHA256_HASH_BYTES]{};

    int result1 = wrapper_hash(
        data,
        sizeof(data),
        hash1
    );

    int result2 = wrapper_hash(
        data,
        sizeof(data),
        hash2
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
    const uint8_t data1[] = {
        'H', 'e', 'l', 'l', 'o'
    };

    const uint8_t data2[] = {
        'H', 'e', 'l', 'l', 'p'
    };

    uint8_t hash1[resurs::audit::SHA256_HASH_BYTES]{};
    uint8_t hash2[resurs::audit::SHA256_HASH_BYTES]{};

    assert(
        wrapper_hash(
            data1,
            sizeof(data1),
            hash1
        ) == 0
    );

    assert(
        wrapper_hash(
            data2,
            sizeof(data2),
            hash2
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
    DigitalSign digSign;

    auto privateKey = digSign.generate_private_key();

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
        signature
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

    // Kontrollera att signature inte är tom.
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
    DigitalSign digSign;

    auto privateKey = digSign.generate_private_key();
    auto publicKey = get_public_key(privateKey);

    uint8_t data[] = {
        'A', 'u', 'd', 'i', 't'
    };

    AuditEntry entry{};

    // Hash canonical data
    assert(
        wrapper_hash(
            data,
            sizeof(data),
            entry.currentHash
        ) == 0
    );

    // Sign hash
    assert(
        wrapper_sign(
            privateKey.data(),
            privateKey.size(),
            entry.currentHash,
            entry.signature
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
    DigitalSign digSign;

    auto privateKey = digSign.generate_private_key();
    auto publicKey = get_public_key(privateKey);

    uint8_t data[] = {
        'A', 'u', 'd', 'i', 't'
    };

    AuditEntry entry{};

    assert(
        wrapper_hash(
            data,
            sizeof(data),
            entry.currentHash
        ) == 0
    );

    assert(
        wrapper_sign(
            privateKey.data(),
            privateKey.size(),
            entry.currentHash,
            entry.signature
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
    DigitalSign digSign;

    auto privateKey = digSign.generate_private_key();
    auto publicKey = get_public_key(privateKey);

    uint8_t data[] = {
        'A', 'u', 'd', 'i', 't'
    };

    AuditEntry entry{};

    assert(
        wrapper_hash(
            data,
            sizeof(data),
            entry.currentHash
        ) == 0
    );

    assert(
        wrapper_sign(
            privateKey.data(),
            privateKey.size(),
            entry.currentHash,
            entry.signature
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
    DigitalSign digSign;

    auto privateKey = digSign.generate_private_key();
    auto publicKey = get_public_key(privateKey);

    uint8_t data[] = {
        'T', 'e', 's', 't'
    };

    AuditEntry entry{};

    assert(
        wrapper_hash(
            data,
            sizeof(data),
            entry.currentHash
        ) == 0
    );

    assert(
        wrapper_sign(
            privateKey.data(),
            privateKey.size(),
            entry.currentHash,
            entry.signature
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
    DigitalSign digSign;

    auto privateKey = digSign.generate_private_key();
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
        wrapper_hash(
            data1,
            sizeof(data1),
            entries[0].currentHash
        ) == 0
    );

    assert(
        wrapper_sign(
            privateKey.data(),
            privateKey.size(),
            entries[0].currentHash,
            entries[0].signature
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
        wrapper_hash(
            data2,
            sizeof(data2),
            entries[1].currentHash
        ) == 0
    );

    assert(
        wrapper_sign(
            privateKey.data(),
            privateKey.size(),
            entries[1].currentHash,
            entries[1].signature
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
    DigitalSign digSign;

    auto privateKey = digSign.generate_private_key();
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
        wrapper_hash(
            data1,
            sizeof(data1),
            entries[0].currentHash
        ) == 0
    );

    assert(
        wrapper_sign(
            privateKey.data(),
            privateKey.size(),
            entries[0].currentHash,
            entries[0].signature
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
        wrapper_hash(
            data2,
            sizeof(data2),
            entries[1].currentHash
        ) == 0
    );

    assert(
        wrapper_sign(
            privateKey.data(),
            privateKey.size(),
            entries[1].currentHash,
            entries[1].signature
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
            publicKey.data()
        ) == 0
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
            signature.data()
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


    std::cout << "\n";
    std::cout
        << passed
        << "/"
        << (passed + failed)
        << " tests passed\n";


    return failed == 0 ? 0 : 1;
}
