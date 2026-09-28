#include <iostream>
#include <string>

#if defined(_WIN32)
#include <windows.h>
#else
#include <dlfcn.h>
#endif

namespace {

#if defined(_WIN32)
using LibraryHandle = HMODULE;

LibraryHandle open_library(const char *path)
{
    return LoadLibraryA(path);
}

bool has_symbol(LibraryHandle library, const char *name)
{
    return GetProcAddress(library, name) != nullptr;
}

void close_library(LibraryHandle library)
{
    FreeLibrary(library);
}
#else
using LibraryHandle = void *;

LibraryHandle open_library(const char *path)
{
    return dlopen(path, RTLD_NOW | RTLD_LOCAL);
}

bool has_symbol(LibraryHandle library, const char *name)
{
    dlerror();
    return dlsym(library, name) != nullptr && dlerror() == nullptr;
}

void close_library(LibraryHandle library)
{
    dlclose(library);
}
#endif

} // namespace

int main(int argc, char **argv)
{
    if (argc != 2) {
        std::cerr << "usage: resurs_crypto_symbol_tests <library-path>\n";
        return 1;
    }

    LibraryHandle library = open_library(argv[1]);
    if (library == nullptr) {
        std::cerr << "failed to load native library: " << argv[1] << '\n';
        return 1;
    }

    const char *required_symbols[] = {
        "aes_256_gcm_encrypt",
        "aes_256_gcm_decrypt"
    };

    bool success = true;
    for (const char *symbol : required_symbols) {
        if (!has_symbol(library, symbol)) {
            std::cerr << "missing exported symbol: " << symbol << '\n';
            success = false;
        }
    }

    close_library(library);
    if (success)
        std::cout << "all required resurs_crypto symbols are exported\n";
    return success ? 0 : 1;
}
