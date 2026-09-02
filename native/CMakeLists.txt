cmake_minimum_required(VERSION 3.20)
project(resurs_native VERSION 0.1.0 LANGUAGES CXX)

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
add_resurs_module(resurs_audit
    resurs_audit/resurs_audit.cpp
    ${CMAKE_CURRENT_SOURCE_DIR}/resurs_audit)

include(GNUInstallDirs)
install(TARGETS resurs_crypto resurs_audit
    LIBRARY DESTINATION ${CMAKE_INSTALL_LIBDIR}
    RUNTIME DESTINATION ${CMAKE_INSTALL_BINDIR})
