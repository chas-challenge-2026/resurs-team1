#include "wrapper.h"

#include <stdint.h>
#include <stdio.h>
#include <string.h>
#include <cstring>



#define HASH_SIZE 32
#define KEY_SIZE 32
#define SIGNATURE_SIZE 64

// static void print_hex(const uint8_t *data, size_t length)
// {
//     for (size_t i = 0; i < length; ++i)
//     {
//         printf("%02x ", data[i]);
//     }
//     putchar('\n');
// }

int main(void)
{
    /* Fixed RFC 8032 Ed25519 test keys. Do not use these in production. */
    const uint8_t private_key[KEY_SIZE] = {
        0x9d, 0x61, 0xb1, 0x9d, 0xef, 0xfd, 0x5a, 0x60,
        0xba, 0x84, 0x4a, 0xf4, 0x92, 0xec, 0x2c, 0xc4,
        0x44, 0x49, 0xc5, 0x69, 0x7b, 0x32, 0x69, 0x19,
        0x70, 0x3b, 0xac, 0x03, 0x1c, 0xae, 0x7f, 0x60
    };
    const uint8_t public_key[KEY_SIZE] = {
        0xd7, 0x5a, 0x98, 0x01, 0x82, 0xb1, 0x0a, 0xb7,
        0xd5, 0x4b, 0xfe, 0xd3, 0xc9, 0x64, 0x07, 0x3a,
        0x0e, 0xe1, 0x72, 0xf3, 0xda, 0xa6, 0x23, 0x25,
        0xaf, 0x02, 0x1a, 0x68, 0xf7, 0x07, 0x51, 0x1a
    };
    const uint8_t canonical_data[] =
        "{\"event\":\"application-created\",\"sequence\":1}";
    printf("canonical data at start: %s\n", canonical_data);

    uint8_t output_hash_buffer[HASH_SIZE] = {};

    uint8_t output_signature_buffer[SIGNATURE_SIZE] = {};
    
    AuditEntry entry;
    entry.canonicalData = canonical_data;
    entry.canonicalDataLength = sizeof(canonical_data) - 1;
    entry.sequenceNumber = 1;

    //entry.previousHash[HASH_SIZE] = {0};
    memset(entry.previousHash, 0, sizeof(entry.previousHash));
    //printf("entry.previousHash after define: %d\n", entry.previousHash);
    std::cout << "entry.previousHash after define: " << std::endl;
    print_hex(entry.previousHash, sizeof(entry.previousHash));





    if (wrapper_hash_and_sign(
        entry.canonicalData,
        entry.canonicalDataLength,
        private_key,
        KEY_SIZE,
        output_hash_buffer,
        output_signature_buffer
    ) != 0)
    {
        //fprint("wrapper_hash_and_sign error");
        fprintf(stderr, "wrapper_hash_and_sign error");
        return 1;
    }

    //entry.currentHash = output_hash_buffer;
    std::memcpy(entry.currentHash, output_hash_buffer, sizeof(output_hash_buffer));

    std::memcpy(entry.signature, output_signature_buffer, sizeof(output_signature_buffer));
    // if (wrapper_hash(entry.canonicalData,
    //                  entry.canonicalDataLength,
    //                  entry.currentHash) != 0)
    // {
    //     fprintf(stderr, "Hashing failed.\n");
    //     return 1;
    // }

    // if (wrapper_sign(entry.canonicalData,
    //                  entry.canonicalDataLength,
    //                  private_key,
    //                  sizeof(private_key),
    //                  entry.signature) != 0)
    // {
    //     fprintf(stderr, "Signing failed.\n");
    //     return 1;
    // }

    printf("Hash:      ");
    print_hex(entry.currentHash, HASH_SIZE);
    printf("Signature: ");
    print_hex(entry.signature, SIGNATURE_SIZE);

    VerifyChainResult result = wrapper_verify_chain(&entry, 1, public_key, sizeof(public_key));
    if (result.result_code != 0)
    {
        //fprintf(stderr, "Chain verification failed.\n");
        printf("%s\n", "chain verification failed.");
        printf("result code: %d\n", result.result_code);
        return 1;
    }

    puts("Audit test completed successfully.");
    return 0;
}
