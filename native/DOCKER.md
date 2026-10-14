# Docker commands for native

Run these commands from the project root. Docker Desktop must be running.

## Supported Ubuntu build images

The Dockerfile accepts `UBUNTU_VERSION` as a build argument. Ubuntu 24.04 is
the default used by the native team. Ubuntu 22.04 (Jammy) is also supported so
the shared library can be verified against a runtime based on Jammy.

Docker downloads the selected Ubuntu image automatically. Neither Ubuntu
version needs to be installed on the host computer.

## Build with Ubuntu 24.04

```bash
docker build --build-arg UBUNTU_VERSION=24.04 -t resurs-crypto-ubuntu-24 ./native
```

The argument can be omitted because 24.04 is the default:

```bash
docker build -t resurs-crypto-ubuntu-24 ./native
```

## Build with Ubuntu 22.04 (Jammy)

```bash
docker build --build-arg UBUNTU_VERSION=22.04 -t resurs-crypto-jammy ./native
```

These commands are intentionally shown on one line so they work in PowerShell,
Command Prompt, Bash, Git Bash, and WSL without shell-specific continuation
characters.

Both builds compile `libresurs_crypto.so`, the demo program, the crypto test
suite, and the exported-symbol test. CTest runs automatically during the image
build, so `docker build` fails if either test fails.

Rebuild without using the Docker cache:

```bash
docker build --no-cache --build-arg UBUNTU_VERSION=24.04 -t resurs-crypto-ubuntu-24 ./native
docker build --no-cache --build-arg UBUNTU_VERSION=22.04 -t resurs-crypto-jammy ./native
```

## Verify the selected Ubuntu version

```bash
docker run --rm --entrypoint /bin/bash resurs-crypto-ubuntu-24 -c "cat /etc/os-release"
docker run --rm --entrypoint /bin/bash resurs-crypto-jammy -c "cat /etc/os-release"
```

The first image should report `VERSION_ID="24.04"`. The second should report
`VERSION_ID="22.04"` and `VERSION_CODENAME=jammy`.

## Run the demo programs

```bash
docker run --rm resurs-crypto-ubuntu-24
docker run --rm resurs-crypto-jammy
```

This runs the executable built from `resurs_crypto/main.cpp`.

## Run all tests again

```bash
docker run --rm resurs-crypto-ubuntu-24 ctest --test-dir /resurs-build --output-on-failure
docker run --rm resurs-crypto-jammy ctest --test-dir /resurs-build --output-on-failure
```

The expected result for each image is:

```text
resurs_crypto_tests          Passed
resurs_crypto_symbol_tests   Passed
100% tests passed
```

`resurs_crypto_symbol_tests` dynamically loads the completed `.so` file and
resolves `aes_256_gcm_encrypt` and `aes_256_gcm_decrypt` by name. This verifies
the same exported-symbol boundary that JNA uses.

To run only the crypto behavior tests:

```bash
docker run --rm resurs-crypto-ubuntu-24 /resurs-build/resurs_crypto_tests
docker run --rm resurs-crypto-jammy /resurs-build/resurs_crypto_tests
```

## Open a shell in the image

```bash
docker run --rm -it --entrypoint /bin/bash resurs-crypto-ubuntu-24
```

The build artifacts are located in `/resurs-build` inside the container. The
Linux shared library is `/resurs-build/libresurs_crypto.so`.

## Runtime compatibility

A Linux shared library is not independent of the environment in which it was
built. It depends on compatible versions of glibc, libstdc++, and OpenSSL.

Build the `.so` distributed to another application with the same Ubuntu
release as that application's runtime, or with an older compatible release.
For example, an application running on Ubuntu 22.04/Jammy should receive the
library built with `UBUNTU_VERSION=22.04`, not the artifact built on Ubuntu
24.04.

The native team's local operating system does not affect this rule because the
compilation takes place inside Docker. The calling application remains
responsible for placing the correct `.so` in its runtime environment and making
it discoverable by JNA.
