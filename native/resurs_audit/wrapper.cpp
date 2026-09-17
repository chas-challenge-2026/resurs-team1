#include "wrapper.h"
#include "resurs_audit.h"







int hash(const uint8_t *canonicalData, size_t canonicalDateLength, uint8_t* output_buffer)
{
    std::vector<uint8_t> cData(canonicalData, canonicalData + canonicalDateLength);

    std::array<uint8_t, resurs::audit::SHA256_HASH_BYTES> result = DigitalSign::hash(cData);

    if(result.size() != resurs::audit::SHA256_HASH_BYTES)
    {
        return -1;
    }

    std::copy(result.begin(), result.end(), output_buffer);

    return 0;
}

int sign(const uint8_t *canonicalData, size_t canonicalDateLength, const uint8_t *privateKey, size_t privateKeyLength, uint8_t* output_buffer)
{
    std::vector<uint8_t> cData(canonicalData, canonicalData + canonicalDateLength);

    EVP_PKEY* DigitalSign::convert_c_private_key_to_EVP_PKEY_POINTER(privateKey, privateKeyLength);

    std::vector<uint8_t> result = DigitalSign::sign(cData, )

    if(result.size() != resurs::audit::DIGITAL_SIGNATURE_BYTES)
    {
        return -1;
    }

    std::copy(result.begin(), result.end(), output_buffer);

    return 0;
}

int verify_chain(const AuditEntry *entries, size_t entryCount, const uint8_t *publicKey, size_t PublicKeyLength)
{

}

unsigned char* generate_private_key()
{

}