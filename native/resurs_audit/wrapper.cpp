#include "wrapper.h"
#include "resurs_audit.h"







int wrapper_hash(const uint8_t *canonicalData, size_t canonicalDateLength, uint8_t* output_buffer)
{
    DigitalSign digSign;

    std::vector<uint8_t> cData(canonicalData, canonicalData + canonicalDateLength);

    std::array<uint8_t, resurs::audit::SHA256_HASH_BYTES> result = digSign.hash(cData);

    if(result.size() != resurs::audit::SHA256_HASH_BYTES)
    {
        return -1;
    }

    std::copy(result.begin(), result.end(), output_buffer);

    return 0;
}

int wrapper_sign(const uint8_t *canonicalData, size_t canonicalDateLength, const uint8_t *privateKey, size_t privateKeyLength, uint8_t* output_buffer)
{
    DigitalSign digSign;

    std::vector<uint8_t> cData(canonicalData, canonicalData + canonicalDateLength);


    std::vector<uint8_t> result = digSign.sign(cData, privateKey, privateKeyLength);

    if(result.size() != resurs::audit::DIGITAL_SIGNATURE_BYTES)
    {
        return -1;
    }

    std::copy(result.begin(), result.end(), output_buffer);

    return 0;
}

int wrapper_verify_chain(const AuditEntry *entries, size_t entryCount, const uint8_t *publicKey, size_t PublicKeyLength)
{
    // Läsa igenom entries

    DigitalSign digSign;

    //AuditEntryChain chain;
    //chain.entries.resize(entryCount);

    int result = digSign.verify_chain(entries, entryCount, publicKey, PublicKeyLength);

    return result;
    


}

unsigned char* generate_private_key()
{

}