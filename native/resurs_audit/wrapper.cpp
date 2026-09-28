#include "wrapper.h"
#include "resurs_audit.h"
#include "audit_types.h"







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

int wrapper_sign(const uint8_t *privateKey, size_t privateKeyLength, uint8_t* input_buffer, uint8_t* output_buffer)
{
    DigitalSign digSign;

    //std::vector<uint8_t> cData(canonicalData, canonicalData + canonicalDateLength);


    DigitalSignResultCode result = digSign.sign(privateKey, privateKeyLength, input_buffer, output_buffer);

    if(result != SIGN_ALL_OK)
    {
        return -1;
    }

    //std::copy(result.begin(), result.end(), output_buffer);

    return 0;
}


int wrapper_hash_and_sign(const uint8_t* canonicalData, size_t canonicalDataLength, const uint8_t* privateKey, size_t privateKeyLength, uint8_t* output_hash_buffer, uint8_t* output_signature_buffer)
{
    int hash_result = wrapper_hash(canonicalData, canonicalDataLength, output_hash_buffer);

    if (hash_result != 0)
    {
        printf("hash fail");
    }

    int signature_result = wrapper_sign(privateKey, privateKeyLength, output_hash_buffer, output_signature_buffer);

    if (signature_result != 0)
    {
        printf("signature fail");
    }

    return 0;
}

VerifyChainResult wrapper_verify_chain(const AuditEntry *entries, size_t entryCount, const uint8_t *publicKey, size_t PublicKeyLength)
{
    // Läsa igenom entries

    DigitalSign digSign;

    //AuditEntryChain chain;
    //chain.entries.resize(entryCount);

    VerifyChainResult result = digSign.verify_chain(entries, entryCount, publicKey, PublicKeyLength);

    return result;
    


}

unsigned char* generate_private_key()
{

}