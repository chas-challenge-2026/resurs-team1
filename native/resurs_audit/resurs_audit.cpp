#include "resurs_audit.h"
#include <algorithm>

#include <stdio.h>
#include <string.h>
#include <cstring>

// VÅr initiala test.
// std::array<std::size_t, resurs::audit::DIGITAL_SIGNATURE_BYTES> DigitalSign::sign(std::array<std::size_t, resurs::audit::SHA256_HASH_BYTES> &currentHash, EVP_PKEY *pkey)

// Modified: Vi tar in data, sköter vi just ed25519 signtering i den här funktionen.
// std::array<std::size_t, resurs::audit::DIGITAL_SIGNATURE_BYTES> DigitalSign::sign(const std::vector<uint8_t>& data, EVP_PKEY *pkey)

// vector direkt utan konstant?
// std::vector<uint8_t> DigitalSign::sign(const std::vector<uint8_t>& data, EVP_PKEY *pkey)


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


DigitalSignResultCode DigitalSign::sign(const uint8_t *privateKey, size_t privateKeyLength, uint8_t *input_hash_buffer, uint8_t *output_signature_buffer)
//std::vector<uint8_t> DigitalSign::sign(const uint8_t *privateKey, size_t privateKeyLength, uint8_t *input_hash_buffer, uint8_t *output_signature_buffer)
{
    // Context är redan satt i konstruktor.

    // convert the uint8_t *pkey pointer and the size_t length to an actualy EVP_PKEY pointer we can use.
    DigitalSign::PkeyPtr pKeyPtr = DigitalSign::convert_c_private_key_to_EVP_PKEY_POINTER(privateKey, privateKeyLength);
    // todo - proper validation

    // INit grej.
    if (EVP_DigestSignInit(
            ctx.get(),
            nullptr,
            nullptr,
            nullptr,
            pKeyPtr.get()) != 1)
    {
        // EVP_MD_CTX_free(ctx);
        //throw std::runtime_error("EVP_DigestSignInit failed.");
        return EVP_DIGEST_SIGN_INIT_FAILED;
    }

    // Räkna ut signaturlängden baserad på algoritm.

    size_t signatureLength = 0;

    if (EVP_DigestSign(
            ctx.get(),
            nullptr,
            &signatureLength,
            input_hash_buffer,
            resurs::audit::SHA256_HASH_BYTES) != 1)
    {
        // EVP_MD_CTX_free(ctx);
        //throw std::runtime_error("failed to get signature length");
        return EVP_DIGEST_SIGN_SIGNATURE_LENGTH_FAILED;
    }

    //std::vector<uint8_t> signature(signatureLength);

    // Skapa faktiska signaturen
    if (EVP_DigestSign(
            ctx.get(),
            output_signature_buffer,
            &signatureLength,
            input_hash_buffer,
            resurs::audit::SHA256_HASH_BYTES) != 1)
    {
        // throw std::runtime_error("Signing failed.");
        return EVP_DIGEST_SIGN_SIGNING_FAILED;
    }

    //signature.resize(signatureLength);

    return SIGN_ALL_OK;

    // Vad vi behöver för att signera:

    // Referenser - dokumentaiton från openssl
}

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

std::array<uint8_t, resurs::audit::SHA256_HASH_BYTES> DigitalSign::hash(const std::vector<uint8_t> &data)
{

    std::array<uint8_t, resurs::audit::SHA256_HASH_BYTES> result{};
    // Context hanteras i konstruktor

    // Resuse existing context, but reset/clear previous encryption state.
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

/* I en kreditansökan:
steg 1 - Jag skickar in.
previousHash: 32-nollbytes:
hash: 111 ----
digitalSignatur aaa ---
sequencenumber 1

steg 2 - scoring modul.
previousHash: 111
hash: 222 ---
digitalSignature: aaa ---
sequenceNumber 2

steg 3 - rejected.
previousHash: 222
hash: 333 ---
idigitalSignature: aaa ---
sequenceNumber 3:



*/

/*
std::array<uint8_t, resurs::audit::SHA256_HASH_BYTES> DigitalSign::hash_chain(const std::string& data, const std::array<uint8_t, resurs::audit::SHA256_HASH_BYTES>& prev_hash)
{


}
*/




// int DigitalSign::verify_chain(const AuditEntryChain* chain, size_t entryCount, EVP_PKEY* publicKey)
VerifyChainResult DigitalSign::verify_chain(const AuditEntry *entries, size_t entryCount, const uint8_t *publicKey, size_t publicKeyLength)
{
    VerifyChainResult verifyChainResult;

    // std::vector<uint8_t> previousEntryCurrentHash{};
    std::array<uint8_t, resurs::audit::SHA256_HASH_BYTES> previousEntryCurrentHash{0};
    memset(previousEntryCurrentHash.data(), 0, sizeof(previousEntryCurrentHash.data()));
    
    // Convert the uint8_t publicKey pointer and the size_t publicKeyPointer length to an actual pkey
    DigitalSign::PkeyPtr pKeyPtr = DigitalSign::convert_c_public_key_to_EVP_PKEY_POINTER(publicKey, publicKeyLength);

    for(size_t i = 0; i < entryCount; i++)
    {
            //if (*entries[i].previousHash != *previousEntryCurrentHash.data())
            if (!std::equal(previousEntryCurrentHash.begin(), previousEntryCurrentHash.end(), entries[i].previousHash))
            {
                //printf("previoushEntryCurrentHash: %d\n", previousEntryCurrentHash.data());
                //printf("entry.previousHash: %d\n", entries[i].previousHash);
                std::cout << "previousEntryCurrentHash: " << std::endl;
                //print_hex(previousEntryCurrentHash.data(), sizeof(previousEntryCurrentHash.data()));
                print_hex(previousEntryCurrentHash.data(), previousEntryCurrentHash.size());
                printf("previousEntryCurrentHash.size():  %d\n", previousEntryCurrentHash.size());
                
                std::cout << "entry.previousHash"<< std::endl;
                print_hex(entries[i].previousHash, sizeof(entries[i].previousHash));
                printf("sizeof(entries[i].previousHash):  %d\n", sizeof(entries[i].previousHash));
                verifyChainResult.result_code = PREVIOUS_HASH_MISSMATCH;
                verifyChainResult.index = i;

                return verifyChainResult;
            }

            // verifiera att hashen i sig ärgiltig
            std::vector<uint8_t> canonicalData(entries[i].canonicalData, entries[i].canonicalData + entries[i].canonicalDataLength);

            //printf("%scanonicalData: \n", canonicalData.data());
            // std::cout << "canonicalData: " << std::endl;
            // print_hex(canonicalData.data(), sizeof(canonicalData.data()));
            
            std::array<uint8_t, resurs::audit::SHA256_HASH_BYTES> hashOfEntry = hash(canonicalData);
            //printf("%sHashed Data: \n", hashOfEntry.data());
            std::cout << "hashOfEntry: " << std::endl;
            //print_hex(hashOfEntry.data(), sizeof(hashOfEntry.data()));
            print_hex(hashOfEntry.data(), hashOfEntry.size());

            std::cout << "entries[i].currentHash: " << std::endl;
            print_hex(entries[i].currentHash, sizeof(entries[i].currentHash));


            //if (reinterpret_cast<uint8_t>(*hashOfEntry.data()) != *entries[i].currentHash)
            if (!std::equal(hashOfEntry.begin(), hashOfEntry.end(), entries[i].currentHash))
            {
                //throw std::runtime_error("Entry hash is not the same as current hash.");
                verifyChainResult.result_code = CURRENT_HASH_MISSMATCH;
                verifyChainResult.index = i;
                //printf("\nhasOfEntry: %d\ncurrentHash: %d\n", hashOfEntry.data(), entries[i].currentHash);
                printf("hashOfEntry: ");
                print_hex(hashOfEntry.data(), hashOfEntry.size());
                printf("\ncurrentHash: ");
                print_hex(entries[i].currentHash, sizeof(entries[i].currentHash));
                printf("\n");

                return verifyChainResult;
            }

            // Använd public key för att verifiera signatur
            if (EVP_DigestVerifyInit(
                    ctx.get(),
                    nullptr,
                    nullptr,
                    nullptr,
                    pKeyPtr.get()) != 1)
            {
                //throw std::runtime_error("EVP_DigestVerifyInit failed.");
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

            // EVP library function returns 1 is successfull and 0 if any error occured. If so, we return the index of the audit entry we got error from.
            if (result == 0)
            {
                verifyChainResult.result_code = EVP_DIGEST_VERIFY_FAILED;
                verifyChainResult.index = i;

                return verifyChainResult;
            }

            //throw std::runtime_error("EVP_DigestVerify failed.");

            // Copy source, of destination.size, to destionation
            std::copy_n(entries[i].currentHash, previousEntryCurrentHash.size(), previousEntryCurrentHash.begin());

            //previousEntryCurrentHash[0] = *entries[i].previousHash;
    }

    verifyChainResult.result_code = ALL_OK;

    return verifyChainResult;
    //return 0; // but in c/c++ 0 is success, and negative numbers if error codes

    /* Dum-kod eller dum-flöde
    1. Spara värde lokalt
    previousEntrysCurrenthash = Spara nuvarande entry' current hash



    2. Loopa igenom all entries
    for (...)
    {
        // Verifiera att previousHash är giltig(tex 32 nollbytes om sequenceNumber är 1, annars jämför med previousEntryCurrentHash)

        // Verifiera att hashen i sig i giltig
        // När vi hashar första gånger skickar vi in en AuditEntry och får tex hash abc123.
        När vi verfierar hash, kan vi bara göra sammma sak?
        Skicka in AUditEntry och om vi inte får tillbaka ahsh abc123, så är det något som har ändrats.

        // Är sequenceNumber 1 mer än förra?

        // Använd hashen och public key för att verifiera signatur

        // Sätta previousEntryCurrentHash av vår nuvarande entries.currentHash, så att den är redo för nästa loop
    }

    // Om inget fel, returnera bra kod, annars felkod/-1

    */
}

