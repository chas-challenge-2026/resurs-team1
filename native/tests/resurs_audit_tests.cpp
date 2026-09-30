#include "wrapper.h"
#include "resurs_audit.h"
#include "audit_types.h"

#include <openssl/evp.h>

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

    assert(result.result_code == ALL_OK);
    assert(result.index == 0);
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

    assert(result.result_code == ALL_OK);
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


    std::cout << "\n";
    std::cout
        << passed
        << "/"
        << (passed + failed)
        << " tests passed\n";


    return failed == 0 ? 0 : 1;
}