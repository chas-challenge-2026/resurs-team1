#include "resurs_audit.h"

// VÅr initiala test.
//std::array<std::size_t, resurs::audit::DIGITAL_SIGNATURE_BYTES> DigitalSign::sign(std::array<std::size_t, resurs::audit::SHA256_HASH_BYTES> &currentHash, EVP_PKEY *pkey)

// Modified: Vi tar in data, sköter vi just ed25519 signtering i den här funktionen.
//std::array<std::size_t, resurs::audit::DIGITAL_SIGNATURE_BYTES> DigitalSign::sign(const std::vector<uint8_t>& data, EVP_PKEY *pkey)

// vector direkt utan konstant?
//std::vector<uint8_t> DigitalSign::sign(const std::vector<uint8_t>& data, EVP_PKEY *pkey)
std::vector<uint8_t> DigitalSign::sign(const std::vector<uint8_t>& data, EVP_PKEY *pkey)
{
    // Context är redan satt i konstruktor.

    //INit grej.
    if (EVP_DigestSignInit(
        ctx.get(),
        nullptr,
        nullptr,
        nullptr,
        pkey) != 1)
    {
        // EVP_MD_CTX_free(ctx);
        throw std::runtime_error("EVP_DigestSignInit failed.");
    }


    // Räkna ut signaturlängden baserad på algoritm.

    size_t signatureLength = 0;

    if (EVP_DigestSign(
        ctx.get(),
        nullptr,
        &signatureLength,
        data.data(),
        data.size()) != 1)
    {
        //EVP_MD_CTX_free(ctx);
        throw std::runtime_error("failed to get signature length");
    }
    

    std::vector<uint8_t> signature(signatureLength);

    // Skapa faktiska signaturen
    if (EVP_DigestSign(
        ctx.get(),
        signature.data(),
        &signatureLength,
        data.data(),
        data.size()) != 1)
    {
        throw std::runtime_error("Signing failed.");
    }

    signature.resize(signatureLength);

    return signature;
    

    // Vad vi behöver för att signera:
    
    // Referenser - dokumentaiton från openssl

}

std::array<unsigned char, resurs::audit::PKEY_BYTES> DigitalSign::generate_private_key()
{
    pKeyCtxPtr.reset(EVP_PKEY_CTX_new_id(EVP_PKEY_ED25519, nullptr));

    if(pKeyCtxPtr == nullptr)
    {
        throw std::runtime_error("failed to create PKEY context.");
    }

    if(EVP_PKEY_keygen_init(pKeyCtxPtr.get()) <= 0)
    {
        throw std::runtime_error("failed to initialize key generation.");
    }

    EVP_PKEY* pKeyRaw = nullptr;
    
    if(EVP_PKEY_keygen(pKeyCtxPtr.get(), &pKeyRaw) <= 0)
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


std::array<uint8_t, resurs::audit::SHA256_HASH_BYTES> DigitalSign::hash(const std::vector<uint8_t> &data)
{

    std::array<uint8_t, resurs::audit::SHA256_HASH_BYTES> result{};
    //Context hanteras i konstruktor


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


int DigitalSign::verify_chain(const AuditEntryChain* chain, size_t entryCount, EVP_PKEY* publicKey)
{
    //std::vector<uint8_t> previousEntryCurrentHash{};
    std::array<uint8_t, resurs::audit::SHA256_HASH_BYTES> previousEntryCurrentHash {};


    size_t index = 0;

    for(auto& entry : chain->entries)
    {
        // Reset MD context for every entry, so we dont have any data from old entry?

        if (entry.previousHash != previousEntryCurrentHash)
        {
            return index;
        }
               
        // verifiera att hashen i sig ärgiltig
        std::vector<uint8_t> canonicalData(entry.canonicalData, entry.canonicalData + entry.canonicalDataLength);

        std::array<uint8_t, resurs::audit::SHA256_HASH_BYTES> hashOfEntry = hash(canonicalData);
        if (hashOfEntry != entry.currentHash)
        {
            throw std::runtime_error("Entry hash is not the same as current hash.");
            return index;
        }

        // Använd public key för att verifiera signatur
        if (EVP_DigestVerifyInit(
            ctx.get(),
            nullptr,
            nullptr,
            nullptr,
            publicKey) != 1)
        {
            throw std::runtime_error("EVP_DigestVerifyInit failed.");
        }

        // Verify 
        int result = EVP_DigestVerify(
            ctx.get(),
            entry.signature,
            resurs::audit::DIGITAL_SIGNATURE_BYTES,
            entry.canonicalData,
            entry.canonicalDataLength
        );

        // EVP library function returns 1 is successfull and 0 if any error occured. If so, we return the index of the audit entry we got error from.
        if (result == 0)
        {
            return index;
        }

        throw std::runtime_error("EVP_DigestVerify failed.");


        previousEntryCurrentHash = entry.previousHash;
        // // Sätta previousEntryCurrentHash av vår nuvarande entries.currentHash, så att den är redo för nästa loop
        // std::vector<uint8_t> prevHash(entry.previousHash, entry.previousHash + resurs::audit::SHA256_HASH_BYTES);
        // previousEntryCurrentHash = prevHash;
        // previousEntryCurrentHash = {entry.previousHash};
        // previousEntryCurrentHash = std::vector<uint8_t>(entry.previousHash, entry.previousHash + resurs::audit::SHA256_HASH_BYTES);
        ++index;
    }

    return 0; // but in c/c++ 0 is success, and negative numbers if error codes    
    
    PkeyPtr convert_c_private_key_to_EVP_PKEY_POINTER(const uint8_t privateKey, size_t privateKeyLength) 
    {
        PkeyPtr pKey = EVP_PKEY_new_raw_private_key_ex(
            nullptr,
            "ED25519",
            nullptr,
            privateKey,
            privateKeyLength
        );

        return pKey;
    } 

    
    PkeyPtr convert_c_public_key_to_EVP_PKEY_POINTER(const uint8_t publicKey, size_t publicKeyLength) 
    {
        PkeyPtr pKey = EVP_PKEY_new_raw_public_key_ex(
            nullptr,
            "ED25519",
            nullptr,
            publicKey,
            publicKeyLength
        );

        return pKey;
    }

    
    
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