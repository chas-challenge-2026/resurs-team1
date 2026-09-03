#include "wrapper.h"
#include <iomanip>


#include "resurs_crypto.h"
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

    // Encrypt
    int encryptResult = aes_256_gcm_encrypt(
        reinterpret_cast<const uint8_t *>(text.data()),
        static_cast<int>(text.size()),
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
/*

int main()
{
    std::string text = "aaaaaaaabbbbbbbbbcccccccccccc";

    std::array<uint8_t, 12> iv;
    std::vector<uint8_t> cipher_text(text.size());
    std::array<unsigned char, 16> tag;

    int result = aes_256_gcm_encrypt(
        reinterpret_cast<const uint8_t*>(text.data()),
        static_cast<int>(text.size()),
        iv.data(), 
        static_cast<int>(iv.size()),
        cipher_text.data(),
        static_cast<int>(cipher_text.size()),
        tag.data(),
        static_cast<int>(tag.size())
    );

    if (result != 0)
    {
        std::cerr << "Encryption failed\n";
        return 1;
    }

    std::cout << "Plaintext: " << text << "\n";

    std::cout << "IV: ";
    for (unsigned char byte : iv)
    {
        std::cout << std::hex
                  << std::setw(2)
                  << std::setfill('0')
                  << static_cast<int>(byte);
    }

    std::cout << "\nCiphertext: ";
    for (unsigned char byte : cipher_text)
    {
        std::cout << std::hex
                  << std::setw(2)
                  << std::setfill('0')
                  << static_cast<int>(byte);
    }

    std::cout << "\nTag: ";
    for (unsigned char byte : tag)
    {
        std::cout << std::hex
                  << std::setw(2)
                  << std::setfill('0')
                  << static_cast<int>(byte);
    }

    std::cout << '\n';
}
*/