#include "wrapper.h"
#include <iomanip>


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
        cipher_text.data(),
        static_cast<int>(cipher_text.size()),
        tag.data()
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
