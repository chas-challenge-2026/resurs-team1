#include "wrapper.h"
#include <iomanip>


#include "resurs_crypto.h"
#include "crypto_types.h"
#include <iostream>



#include "wrapper.h"

#include <array>
#include <iostream>
#include <string>
#include <vector>

int main()
{
    std::string text = "aaaaaaaabbbbbbbbbcccccccccccc";

    std::array<uint8_t, 12> iv{};
    std::array<unsigned char, 16> tag{};

    std::vector<uint8_t> ciphertext(text.size());
    std::vector<uint8_t> decrypted(text.size());
    std::array<unsigned char, resurs::crypto::AES_256_KEY_SIZE_BYTES> key =
        {0x52, 0x86, 0x5A, 0x9C, 0x22, 0xEE, 0x88, 0xE5,
         0x03, 0x25, 0x6B, 0x6D, 0x04, 0x01, 0x21, 0x6B,
         0xDE, 0xD4, 0x06, 0xA1, 0xFD, 0x88, 0x61, 0x6C,
         0x1A, 0x7A, 0x77, 0x92, 0x18, 0x76, 0xCF, 0x9C};

    // Encrypt
    int encryptResult = aes_256_gcm_encrypt(
        reinterpret_cast<const uint8_t *>(text.data()),
        static_cast<int>(text.size()),
        key.data(),
        static_cast<int>(key.size()),
        iv.data(),
        static_cast<int>(iv.size()),
        ciphertext.data(),
        static_cast<int>(ciphertext.size()),
        tag.data(),
        static_cast<int>(tag.size())
    );

    if (encryptResult != 0)
    {
        std::cerr
            << "Encryption failed: "
            << encryptResult
            << '\n';

        return 1;
    }

    std::cout << "Encryption successful\n";

    // Decrypt
    int decryptResult = aes_256_gcm_decrypt(
        ciphertext.data(),
        static_cast<int>(ciphertext.size()),
        key.data(),
        static_cast<int>(key.size()),
        iv.data(),
        static_cast<int>(iv.size()),
        decrypted.data(),
        static_cast<int>(decrypted.size()),
        tag.data(),
        static_cast<int>(tag.size())
    );

    if (decryptResult != 0)
    {
        std::cerr
            << "Decryption failed: "
            << decryptResult
            << '\n';

        return 1;
    }

    std::string decryptedText(
        reinterpret_cast<const char *>(decrypted.data()),
        decrypted.size()
    );

    std::cout << "Original:  " << text << '\n';
    std::cout << "Decrypted: " << decryptedText << '\n';

    if (text == decryptedText)
    {
        std::cout << "SUCCESS: texts match\n";
    }
    else
    {
        std::cout << "FAILED: texts do not match\n";
    }

    return 0;
}   