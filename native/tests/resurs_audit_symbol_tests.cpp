#include <cstddef>
#include <iostream>

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
        std::cerr << "usage: resurs_audit_symbol_tests <library-path>\n";
        return 1;
    }

    LibraryHandle library = open_library(argv[1]);
    if (library == nullptr) {
        std::cerr << "failed to load native library: " << argv[1] << '\n';
        return 1;
    }

    const char *required_symbols[] = {
        "generate_private_key",
        "get_public_key",
        "wrapper_hash_and_sign",
        "wrapper_verify_chain"
    };

    std::size_t passed = 0;
    for (const char *symbol : required_symbols) {
        if (!has_symbol(library, symbol)) {
            std::cout << "[FAIL] exported symbol " << symbol << '\n';
        } else {
            std::cout << "[PASS] exported symbol " << symbol << '\n';
            ++passed;
        }
    }

    close_library(library);
    const auto total = sizeof(required_symbols) / sizeof(required_symbols[0]);
    std::cout << passed << "/" << total << " symbol checks passed\n";
    return passed == total ? 0 : 1;
}
