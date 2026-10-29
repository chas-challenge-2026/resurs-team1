#ifndef RESURS_AUDIT_H
#define RESURS_AUDIT_H
/**
 * @file
 * @brief Internal C++ SHA-256 and Ed25519 implementation.
 * Java/JNA callers use the validated C ABI declared in wrapper.h.
 */

#include <iostream>
#include <openssl/evp.h>
#include <openssl/err.h>
#include <array>
#include <vector>
#include <memory>

#include "audit_types.h"

/**
 * Keeps the OpenSSL state used for hashing, signing, and verification under
 * automatic ownership, so callers never need to release these resources manually.
 *
 * Methods share mutable state: reuse an instance sequentially, or synchronize
 * access externally. Separate instances own separate contexts. Borrowed input
 * pointers remain caller-owned and must stay valid throughout the call.
 * The C wrappers validate arguments and translate exceptions into result codes;
 * direct C++ callers must satisfy each method's preconditions themselves.
 */
class DigitalSign
{
    private:
        /// Gives the key-generation context one owner.
        /// EVP_PKEY_CTX_free is called automatically.
        using PkeyCtxPtr = std::unique_ptr<EVP_PKEY_CTX, decltype(&EVP_PKEY_CTX_free)>;
        /// Gives an OpenSSL key one owner.
        /// EVP_PKEY_free is called automatically.
        using PkeyPtr = std::unique_ptr<EVP_PKEY, decltype(&EVP_PKEY_free)>;
        /// Gives the shared context one owner.
        /// EVP_MD_CTX_free is called automatically.
        using DigestSignCtxPtr = std::unique_ptr<EVP_MD_CTX, decltype(&EVP_MD_CTX_free)>;
        /// Shared working state means one instance must be used sequentially.
        DigestSignCtxPtr ctx;
        /// Key-generation context, replaced on each generate_private_key call.
        PkeyCtxPtr pKeyCtxPtr;
        /// Most recently generated key pair, retained until replacement or destruction.
        PkeyPtr pKeyPtr;
    
    public:
        /**
         * Allocate the working digest context; key resources start empty.
         * All owned OpenSSL resources are released automatically.
         * @throws std::runtime_error if OpenSSL cannot allocate the context.
         */
        DigitalSign() : ctx(EVP_MD_CTX_new(), EVP_MD_CTX_free), pKeyCtxPtr(nullptr, &EVP_PKEY_CTX_free), pKeyPtr(nullptr, &EVP_PKEY_free)
        {
            if (ctx == nullptr)
            {
                throw std::runtime_error("failed to create context.");
            }
        };
        /**
         * Generate an Ed25519 key pair and return its raw 32-byte private key.
         * The pair remains owned by this instance until replaced or destroyed;
         * the returned array is an independent copy owned by the caller.
         * The caller is responsible for secure storage and lifecycle of that copy.
         * @throws std::runtime_error if generation or raw-key extraction fails.
         */
        std::array<unsigned char, resurs::audit::PKEY_BYTES> generate_private_key();
        /**
         * Extract the raw 32-byte public key from a borrowed Ed25519 key object.
         * @param privateKey Non-null EVP_PKEY containing an Ed25519 private key.
         * Ownership is not transferred and the object is not freed here.
         * @return An owned copy of the public-key bytes.
         * @throws std::runtime_error if OpenSSL cannot extract the public key.
         */
        std::array<uint8_t, 32> get_public_key(EVP_PKEY* privateKey);
        
        /**
         * Reset the working context and compute SHA-256 over the supplied bytes.
         * Empty input is accepted internally. No JSON parsing or canonicalization
         * is performed; callers supply the exact representation to protect.
         * @return The owned 32-byte digest.
         * @throws std::runtime_error if a digest operation or length check fails.
         */
        std::array<uint8_t, resurs::audit::SHA256_HASH_BYTES> hash(const std::vector<uint8_t> &data);
        /**
         * Sign an existing SHA-256 digest as the Ed25519 message.
         *
         * Contract: privateKey points to privateKeyLength readable bytes (32 for
         * Ed25519), input_hash_buffer provides 32 readable bytes, and
         * output_signature_buffer provides 64 writable bytes. Pointers must be
         * non-null; this internal method does not validate buffer capacities.
         * @return SIGN_ALL_OK or a conversion, initialization, size-query, or
         * signing error from DigitalSignResultCode. Use output only on success.
         */
        DigitalSignResultCode sign(const uint8_t *privateKey, size_t privateKeyLength, uint8_t *input_hash_buffer, uint8_t *output_signature_buffer);
        /**
         * Verify an ordered complete audit chain with one Ed25519 public key.
         *
         * Contract: entries points to entryCount valid contiguous AuditEntry
         * objects; each canonicalData pointer provides canonicalDataLength readable
         * bytes. publicKey provides publicKeyLength readable bytes (32 for Ed25519).
         * The C wrapper additionally requires a non-empty chain and entry data.
         * Checks each previous-hash link, one-based sequence number, SHA-256 digest,
         * and signature in that order, stopping at the first failure.
         * Canonical bytes are opaque; the caller must include sequenceNumber and
         * previousHash in the signed JSON and keep them consistent with the fields.
         * @return VERIFY_CHAIN_ALL_OK or the first verification error, with a
         * zero-based index for entry-specific errors. Other results use index 0.
         * @throws std::runtime_error if hashing fails; allocation failures may also
         * propagate. The C wrapper maps exceptions to an internal-error result.
         * A valid prefix alone cannot detect entries removed from the chain's end.
         */
        VerifyChainResult verify_chain(const AuditEntry *entries, size_t entryCount, const uint8_t *publicKey, size_t publicKeyLength);

        /**
         * Import borrowed raw Ed25519 private-key bytes into an owned OpenSSL key.
         * @param privateKey Non-null pointer to privateKeyLength readable bytes.
         * @param privateKeyLength Raw key length; Ed25519 requires 32 bytes.
         * @return A PkeyPtr that frees the imported key, or null if import fails.
         * The input buffer stays caller-owned and is not retained by this helper.
         */
        PkeyPtr convert_c_private_key_to_EVP_PKEY_POINTER(const uint8_t *privateKey, size_t privateKeyLength);
        /**
         * Import borrowed raw Ed25519 public-key bytes into an owned OpenSSL key.
         * @param publicKey Non-null pointer to publicKeyLength readable bytes.
         * @param publicKeyLength Raw key length; Ed25519 requires 32 bytes.
         * @return A PkeyPtr that frees the imported key, or null if import fails.
         * The input buffer stays caller-owned and is not retained by this helper.
         */
        PkeyPtr convert_c_public_key_to_EVP_PKEY_POINTER(const uint8_t *publicKey, size_t publicKeyLength);
};


#endif
