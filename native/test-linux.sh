#!/usr/bin/env bash

set -uo pipefail

script_dir="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
ubuntu_versions=("24.04" "22.04")
failed_versions=()

if ! command -v docker >/dev/null 2>&1; then
    echo "Docker was not found. Install or start Docker Desktop with WSL integration enabled."
    exit 1
fi

if ! docker info >/dev/null 2>&1; then
    echo "Docker is not running. Start Docker Desktop and try again."
    exit 1
fi

for version in "${ubuntu_versions[@]}"; do
    tag="resurs-native-test:ubuntu-${version//./-}"

    echo
    echo "============================================================"
    echo "Building and testing native modules on Ubuntu ${version}"
    echo "============================================================"

    if docker build \
        --pull \
        --no-cache \
        --build-arg "UBUNTU_VERSION=${version}" \
        --build-arg "CMAKE_BUILD_TYPE=Debug" \
        --tag "${tag}" \
        "${script_dir}"; then
        echo "PASS: Ubuntu ${version}"
    else
        echo "FAIL: Ubuntu ${version}"
        failed_versions+=("${version}")
    fi
done

echo
if ((${#failed_versions[@]} > 0)); then
    echo "Native tests failed on: ${failed_versions[*]}"
    exit 1
fi

echo "All native tests passed on Ubuntu 24.04 and Ubuntu 22.04."