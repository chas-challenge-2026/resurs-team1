cmake_minimum_required(VERSION 3.20)
project(resurs_native VERSION 0.1.0 LANGUAGES CXX)

find_package(OpenSSL REQUIRED)

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)
set(CMAKE_CXX_EXTENSIONS OFF)

function(add_resurs_module target source module_directory)
    add_library(${target} SHARED ${source})
    target_include_directories(${target} PUBLIC ${module_directory})
    if(MSVC)
        target_compile_options(${target} PRIVATE /W4)
    else()
        target_compile_options(${target} PRIVATE -Wall -Wextra -Wpedantic)
    endif()
endfunction()

add_resurs_module(resurs_crypto
    resurs_crypto/resurs_crypto.cpp
    ${CMAKE_CURRENT_SOURCE_DIR}/resurs_crypto)
target_sources(resurs_crypto PRIVATE resurs_crypto/wrapper.cpp)
target_link_libraries(resurs_crypto PRIVATE OpenSSL::Crypto)
target_compile_definitions(resurs_crypto PRIVATE RESURS_CRYPTO_BUILDING_DLL)
set_target_properties(resurs_crypto PROPERTIES
    CXX_VISIBILITY_PRESET hidden
    VISIBILITY_INLINES_HIDDEN YES)

option(RESURS_BUILD_CRYPTO_DEMO
    "Build the resurs_crypto demo executable"
    OFF)

if(RESURS_BUILD_CRYPTO_DEMO)
    add_executable(resurs_crypto_demo
        resurs_crypto/main.cpp)
    target_link_libraries(resurs_crypto_demo PRIVATE resurs_crypto)
    target_include_directories(resurs_crypto_demo PRIVATE
        ${CMAKE_CURRENT_SOURCE_DIR}/resurs_crypto)
endif()

add_resurs_module(resurs_audit
    resurs_audit/resurs_audit.cpp
    ${CMAKE_CURRENT_SOURCE_DIR}/resurs_audit)

include(GNUInstallDirs)
install(TARGETS resurs_crypto resurs_audit
    LIBRARY DESTINATION ${CMAKE_INSTALL_LIBDIR}
    RUNTIME DESTINATION ${CMAKE_INSTALL_BINDIR})

include(CTest)
if(BUILD_TESTING)
    add_executable(resurs_crypto_tests
        tests/resurs_crypto_tests.cpp
        resurs_crypto/resurs_crypto.cpp)
    target_link_libraries(resurs_crypto_tests PRIVATE resurs_crypto OpenSSL::Crypto)
    target_include_directories(resurs_crypto_tests PRIVATE
        ${CMAKE_CURRENT_SOURCE_DIR}/resurs_crypto)
    if(MSVC)
        target_compile_options(resurs_crypto_tests PRIVATE /W4)
    else()
        target_compile_options(resurs_crypto_tests PRIVATE -Wall -Wextra -Wpedantic)
    endif()
    add_test(NAME resurs_crypto_tests COMMAND resurs_crypto_tests)

    add_executable(resurs_crypto_symbol_tests
        tests/resurs_crypto_symbol_tests.cpp)
    target_link_libraries(resurs_crypto_symbol_tests PRIVATE ${CMAKE_DL_LIBS})
    if(MSVC)
        target_compile_options(resurs_crypto_symbol_tests PRIVATE /W4)
    else()
        target_compile_options(resurs_crypto_symbol_tests PRIVATE -Wall -Wextra -Wpedantic)
    endif()
    add_test(NAME resurs_crypto_symbol_tests
        COMMAND resurs_crypto_symbol_tests $<TARGET_FILE:resurs_crypto>)
endif()
