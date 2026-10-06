#include "resurs_audit.h"
#include <algorithm>

#include <stdio.h>
#include <string.h>
#include <cstring>

// ----------- Internal helper functions to convert details of a private key in C to EVP PkeyPtr. -----------

DigitalSign::PkeyPtr DigitalSign::convert_c_private_key_to_EVP_PKEY_POINTER(const uint8_t *privateKey, size_t privateKeyLength)
{    
    return {EVP_PKEY_new_raw_private_key_ex(
        nullptr,
        "ED25519",
        nullptr,
        privateKey,
        privateKeyLength),
        &EVP_PKEY_free
    };
}

DigitalSign::PkeyPtr DigitalSign::convert_c_public_key_to_EVP_PKEY_POINTER(const uint8_t *publicKey, size_t publicKeyLength)
{
    // We create a new key which the ownership is transfered to, and return that key
    return {
        EVP_PKEY_new_raw_public_key_ex(
            nullptr,
            "ED25519",
            nullptr,
            publicKey,
            publicKeyLength),
        &EVP_PKEY_free
    };
}


// ----------- Key generation methods -----------
std::array<unsigned char, resurs::audit::PKEY_BYTES> DigitalSign::generate_private_key()
{
    pKeyCtxPtr.reset(EVP_PKEY_CTX_new_id(EVP_PKEY_ED25519, nullptr));

    if (pKeyCtxPtr == nullptr)
    {
        throw std::runtime_error("failed to create PKEY context.");
    }

    if (EVP_PKEY_keygen_init(pKeyCtxPtr.get()) <= 0)
    {
        throw std::runtime_error("failed to initialize key generation.");
    }

    EVP_PKEY *pKeyRaw = nullptr;

    if (EVP_PKEY_keygen(pKeyCtxPtr.get(), &pKeyRaw) <= 0)
    {
        throw std::runtime_error("failed to generate key.");
    }

    pKeyPtr.reset(pKeyRaw);

    std::array<unsigned char, resurs::audit::PKEY_BYTES> privateKey{};

    size_t privateKeyLength = privateKey.size();

    if (EVP_PKEY_get_raw_private_key(pKeyPtr.get(), privateKey.data(), &privateKeyLength) <= 0 || privateKeyLength != privateKey.size())
    {
        throw std::runtime_error("failed to extract raw private key.");
    }

    return privateKey;
}


std::array<uint8_t, resurs::audit::PKEY_BYTES> DigitalSign::get_public_key(EVP_PKEY* privateKey)
{
    std::array<uint8_t, resurs::audit::PKEY_BYTES> publicKey;
    size_t length = publicKey.size();

    if (EVP_PKEY_get_raw_public_key(
            privateKey,
            publicKey.data(),
            &length) <= 0)
    {
        throw std::runtime_error("Failed to get public key");
    }
    return publicKey;
}


// ----------- Algorithm and functionality -----------

DigitalSignResultCode DigitalSign::sign(const uint8_t *privateKey, size_t privateKeyLength, uint8_t *input_hash_buffer, uint8_t *output_signature_buffer)
{
    // Context is already set in the constructor

    // convert the uint8_t *pkey pointer and the size_t length to an actualy EVP_PKEY pointer we can use.
    DigitalSign::PkeyPtr pKeyPtr = DigitalSign::convert_c_private_key_to_EVP_PKEY_POINTER(privateKey, privateKeyLength);

    if(pKeyPtr == nullptr)
    {
        return SIGN_FAILED_TO_CONVERT_PRIVATE_KEY;
    }

    // Init
    if (EVP_DigestSignInit(
            ctx.get(),
            nullptr,
            nullptr,
            nullptr,
            pKeyPtr.get()) != 1)
    {
        return EVP_DIGEST_SIGN_INIT_FAILED;
    }    
    
    // Get the length of the signature based on our algorithm.    
    size_t signatureLength = 0;

    if (EVP_DigestSign(
            ctx.get(),
            nullptr,
            &signatureLength,
            input_hash_buffer,
            resurs::audit::SHA256_HASH_BYTES) != 1)
    {
        return EVP_DIGEST_SIGN_SIGNATURE_LENGTH_FAILED;
    }

    // Create the signature
    if (EVP_DigestSign(
            ctx.get(),
            output_signature_buffer,
            &signatureLength,
            input_hash_buffer,
            resurs::audit::SHA256_HASH_BYTES) != 1)
    {
        return EVP_DIGEST_SIGN_SIGNING_FAILED;
    }

    return SIGN_ALL_OK;
}

std::array<uint8_t, resurs::audit::SHA256_HASH_BYTES> DigitalSign::hash(const std::vector<uint8_t> &data)
{

    std::array<uint8_t, resurs::audit::SHA256_HASH_BYTES> result{};
    // Context is handled in the constructor

    // Re-use existing context, but reset/clear previous encryption state.
    if (EVP_MD_CTX_reset(ctx.get()) != 1)
    {
        throw std::runtime_error("failed to rset cipher context");
    }

    if (EVP_DigestInit_ex(
            ctx.get(),
            EVP_sha256(),
            nullptr) != 1)
    {
        throw std::runtime_error("failed to init sha-256.");
    }

    if (EVP_DigestUpdate(
            ctx.get(),
            data.data(),
            data.size()) != 1)
    {
        throw std::runtime_error("Failed to hash data");
    }

    unsigned int hashLength = 0;

    if (EVP_DigestFinal_ex(
            ctx.get(),
            result.data(),
            &hashLength) != 1)
    {
        throw std::runtime_error("failed to finalize sha256.");
    }

    if (hashLength != resurs::audit::SHA256_HASH_BYTES)
    {
        throw std::runtime_error("Unexpected sha256 hash-size.");
    }

    return result;
}


VerifyChainResult DigitalSign::verify_chain(const AuditEntry *entries, size_t entryCount, const uint8_t *publicKey, size_t publicKeyLength)
{
    VerifyChainResult verifyChainResult;

    std::array<uint8_t, resurs::audit::SHA256_HASH_BYTES> previousEntryCurrentHash{0};
    memset(previousEntryCurrentHash.data(), 0, sizeof(previousEntryCurrentHash.data()));
    
    DigitalSign::PkeyPtr pKeyPtr = DigitalSign::convert_c_public_key_to_EVP_PKEY_POINTER(publicKey, publicKeyLength);
    if(pKeyPtr == nullptr)
    {
        verifyChainResult.result_code = VERIFY_CHAIN_FAILED_TO_CONVERT_PUBLIC_KEY;
        return verifyChainResult;
    }

    for(size_t i = 0; i < entryCount; i++)
    {
        if (!std::equal(previousEntryCurrentHash.begin(), previousEntryCurrentHash.end(), entries[i].previousHash))
        {
            verifyChainResult.result_code = PREVIOUS_HASH_MISSMATCH;
            verifyChainResult.index = i;

            return verifyChainResult;
        }

        // Verify that the hash itself is valid
        std::vector<uint8_t> canonicalData(entries[i].canonicalData, entries[i].canonicalData + entries[i].canonicalDataLength);
        std::array<uint8_t, resurs::audit::SHA256_HASH_BYTES> hashOfEntry = hash(canonicalData);
        if (!std::equal(hashOfEntry.begin(), hashOfEntry.end(), entries[i].currentHash))
        {
            verifyChainResult.result_code = CURRENT_HASH_MISSMATCH;
            verifyChainResult.index = i;
            return verifyChainResult;
        }

        // Use public key to verify signature
        if (EVP_DigestVerifyInit(
                ctx.get(),
                nullptr,
                nullptr,
                nullptr,
                pKeyPtr.get()) != 1)
        {
            verifyChainResult.result_code = EVP_DIGEST_VERIFY_INIT_FAILED;
            verifyChainResult.index = i;
            return verifyChainResult;
        }

        // Verify
        int result = EVP_DigestVerify(
            ctx.get(),
            entries[i].signature,
            resurs::audit::DIGITAL_SIGNATURE_BYTES,
            entries[i].currentHash,
            sizeof(entries[i].currentHash));

        // EVP library function returns 1 is successfull and 0 if any error occured. 
        // on error, return the index of the audit entry we got error from.
        if (result == 0)
        {
            verifyChainResult.result_code = EVP_DIGEST_VERIFY_FAILED;
            verifyChainResult.index = i;
            return verifyChainResult;
        }

        // Copy source, of destination.size, to destionation
        std::copy_n(entries[i].currentHash, previousEntryCurrentHash.size(), previousEntryCurrentHash.begin());
    }
    verifyChainResult.result_code = VERIFY_CHAIN_ALL_OK;

    return verifyChainResult;
}