# Docker commands for native

Run these commands from the project root. Docker Desktop must be running.

## Build and run the audit demo

```bash
docker build -t resurs-crypto-demo ./native && \
docker run --rm resurs-crypto-demo
```

## Build the native image

```bash
docker build -t resurs-crypto-demo ./native
```

The build compiles both native libraries (`resurs_crypto` and `resurs_audit`),
the crypto demo, the crypto tests, and `resurs_audit/Main.cpp`. The audit demo
is linked against the audit library and OpenSSL. The existing CTest tests run
automatically during the image build.

Rebuild without using the Docker cache:

```bash
docker build --no-cache -t resurs-crypto-demo ./native
```

## Run the audit demo

```bash
docker run --rm resurs-crypto-demo
```

This runs the executable built from `resurs_audit/Main.cpp`, which hashes, signs,
and verifies an audit entry.

## Run the crypto demo

```bash
docker run --rm resurs-crypto-demo /resurs-build/resurs_crypto_demo
```

## Run the tests again

The tests can be run without rebuilding the image:

```bash
docker run --rm resurs-crypto-demo /resurs-build/resurs_crypto_tests
```

## Run Audit tests
```bash
docker run --rm resurs-crypto-demo /resurs-build/resurs_audit_tests
```

Alternatively, run them through CTest inside the container:

```bash
docker run --rm resurs-crypto-demo \
  ctest --test-dir /resurs-build --output-on-failure
```

## Open a shell in the image

```bash
docker run --rm -it --entrypoint /bin/bash resurs-crypto-demo
```

The build artifacts are located in `/resurs-build` inside the container.

## Refresh C++ IntelliSense

The recommended VS Code setup is the development container in `.devcontainer`.
It contains the compiler, OpenSSL headers, CMake, and Ninja used by the native
project. Open the repository in VS Code and run **Dev Containers: Reopen in
Container**.

CMake Tools configures `native` when the workspace opens and provides the
compiler flags and include paths to the C/C++ extension. CMake also writes a
compilation database to:

```text
native/build-devcontainer/compile_commands.json
```

After adding a source file, changing `CMakeLists.txt`, or seeing stale
diagnostics:

1. Run **CMake: Delete Cache and Reconfigure** from the command palette.
2. Wait for CMake configuration to finish.
3. Run **C/C++: Reset IntelliSense Database** if diagnostics are still stale.
4. Use **Developer: Reload Window** if the language server still shows an old
   result.

The audit target is part of the CMake project, so `resurs_audit.cpp` and
`wrapper.cpp` should appear in the CMake project outline. IntelliSense errors in
those files are expected while `resurs_audit` does not compile; compare them
with the authoritative build output from:

```bash
docker build --no-cache -t resurs-crypto-demo ./native
```
