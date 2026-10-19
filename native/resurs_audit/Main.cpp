#include "wrapper.h"

#include <stdint.h>
#include <stdio.h>
#include <string.h>
#include <cstring>

#define HASH_SIZE 32
#define KEY_SIZE 32
#define SIGNATURE_SIZE 64

int main(void)
{
    unsigned char private_key[KEY_SIZE] = {0};

    unsigned char public_key[KEY_SIZE] = {0};

    generate_private_key(private_key);

    // Old version
    //get_public_key(private_key, public_key);
    get_public_key(private_key, sizeof(private_key), public_key, sizeof(public_key));

    std::cout << "Private key: " << std::endl;
    print_hex(private_key, KEY_SIZE);
    std::cout << "Public key: " << std::endl;
    print_hex(public_key, KEY_SIZE);

    const uint8_t canonical_data[] =
        "{\"event\":\"application-created\",\"sequence\":1}";
    printf("canonical data at start: %s\n", canonical_data);

    uint8_t output_hash_buffer[HASH_SIZE] = {};

    uint8_t output_signature_buffer[SIGNATURE_SIZE] = {};

    AuditEntry entry;
    entry.canonicalData = canonical_data;
    entry.canonicalDataLength = sizeof(canonical_data) - 1;
    entry.sequenceNumber = 1;

    memset(entry.previousHash, 0, sizeof(entry.previousHash));
    std::cout << "entry.previousHash after define: " << std::endl;
    print_hex(entry.previousHash, sizeof(entry.previousHash));

    if (wrapper_hash_and_sign(
            entry.canonicalData,
            entry.canonicalDataLength,
            private_key,
            KEY_SIZE,
            output_hash_buffer,
            output_signature_buffer) != 0)
    {
        fprintf(stderr, "wrapper_hash_and_sign error");
        return 1;
    }

    std::memcpy(entry.currentHash, output_hash_buffer, sizeof(output_hash_buffer));
    std::memcpy(entry.signature, output_signature_buffer, sizeof(output_signature_buffer));

    printf("Hash:      ");
    print_hex(entry.currentHash, HASH_SIZE);
    printf("Signature: ");
    print_hex(entry.signature, SIGNATURE_SIZE);

    VerifyChainResult result = wrapper_verify_chain(&entry, 1, public_key, sizeof(public_key));
    if (result.result_code != 0)
    {
        printf("%s\n", "chain verification failed.");
        printf("result code: %d\n", result.result_code);
        return 1;
    }

    puts("Audit test completed successfully.");
    return 0;
}
