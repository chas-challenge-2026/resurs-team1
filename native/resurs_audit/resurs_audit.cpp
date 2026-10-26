#include "resurs_audit.h"
#include <algorithm>

#include <stdio.h>
#include <string.h>
#include <cstring>

/**
 * @file
 * @brief SHA-256 and Ed25519 operations using resources owned by DigitalSign.
 * Method contracts, buffer preconditions, and results are in resurs_audit.h.
 */

// Import raw key bytes into OpenSSL objects with automatic resource cleanup.

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
    // Return ownership of the imported key; the source bytes remain caller-owned.
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

    // OpenSSL writes a newly allocated key; transfer it to the owning member below.
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
    // The context is owned by this instance; sign the supplied hash, not raw JSON.

    // Own the imported signing key locally so every return path releases it.
    DigitalSign::PkeyPtr pKeyPtr = DigitalSign::convert_c_private_key_to_EVP_PKEY_POINTER(privateKey, privateKeyLength);

    if(pKeyPtr == nullptr)
    {
        return SIGN_FAILED_TO_CONVERT_PRIVATE_KEY;
    }

    // Ed25519 signs the 32-byte input directly; no digest algorithm is supplied here.
    if (EVP_DigestSignInit(
            ctx.get(),
            nullptr,
            nullptr,
            nullptr,
            pKeyPtr.get()) != 1)
    {
        return EVP_DIGEST_SIGN_INIT_FAILED;
    }    
    
    // Query the signature size before writing to the caller's 64-byte buffer.
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

    // Write the signature; callers must ignore the output if any step fails.
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
    // The same owned context is used sequentially for hashing and signatures.

    // Reset digest/signature state before starting a new SHA-256 computation.
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
    // Entry errors set a zero-based index; success and key-import failure use zero.
    VerifyChainResult verifyChainResult {VERIFY_CHAIN_ALL_OK, 0};

    // The complete chain starts with an all-zero previous hash.
    std::array<uint8_t, resurs::audit::SHA256_HASH_BYTES> previousEntryCurrentHash{0};
    //memset(previousEntryCurrentHash.begin(), 0, sizeof(previousEntryCurrentHash));
    
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

        // Require a complete chain starting at sequence 1, without gaps or reordering.
        if (entries[i].sequenceNumber != i + 1)
        {
            verifyChainResult.result_code = SEQUENCE_NUMBER_MISSMATCH;
            verifyChainResult.index = i;
            return verifyChainResult;
        }

        // Hash the exact supplied bytes; JSON canonicalization belongs to the caller.
        std::vector<uint8_t> canonicalData(entries[i].canonicalData, entries[i].canonicalData + entries[i].canonicalDataLength);
        std::array<uint8_t, resurs::audit::SHA256_HASH_BYTES> hashOfEntry = hash(canonicalData);
        if (!std::equal(hashOfEntry.begin(), hashOfEntry.end(), entries[i].currentHash))
        {
            verifyChainResult.result_code = CURRENT_HASH_MISSMATCH;
            verifyChainResult.index = i;
            return verifyChainResult;
        }

        // hash() reset the shared context; initialize Ed25519 verification for this entry.
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

        // Verify the stored signature over the stored (and now recomputed) currentHash.
        int result = EVP_DigestVerify(
            ctx.get(),
            entries[i].signature,
            resurs::audit::DIGITAL_SIGNATURE_BYTES,
            entries[i].currentHash,
            sizeof(entries[i].currentHash));

        // Only OpenSSL's explicit success value (1) accepts the signature.
        // Invalid signatures and operation errors both report this entry's index.
        if (result != 1)
        {
            verifyChainResult.result_code = EVP_DIGEST_VERIFY_FAILED;
            verifyChainResult.index = i;
            return verifyChainResult;
        }

        // Carry the verified hash forward as the required link for the next entry.
        std::copy_n(entries[i].currentHash, previousEntryCurrentHash.size(), previousEntryCurrentHash.begin());
    }
    verifyChainResult.result_code = VERIFY_CHAIN_ALL_OK;

    return verifyChainResult;
}
