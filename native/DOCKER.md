# Docker commands for native

Run these commands from the project root. Docker Desktop must be running.

## Build and run the audit demo

```bash
docker build -t resurs-native ./native && \
docker run --rm resurs-native
```

## Build the native image

```bash
docker build -t resurs-native ./native
```

The build compiles both native libraries (`resurs_crypto` and `resurs_audit`),
both demos, and both test executables. CTest runs automatically during the
image build, so the build fails if a test fails.

Rebuild without using the Docker cache:

```bash
docker build --no-cache -t resurs-native ./native
```

## Run the audit demo

```bash
docker run --rm resurs-native
```

This runs the executable built from `resurs_audit/Main.cpp`, which hashes, signs,
and verifies an audit entry.

## Run the crypto demo

```bash
docker run --rm resurs-native /resurs-build/resurs_crypto_demo
```

## Run the tests again

The tests can be run without rebuilding the image:

```bash
docker run --rm resurs-native /resurs-build/resurs_crypto_tests
```

## Run the audit tests

```bash
docker run --rm resurs-native /resurs-build/resurs_audit_tests
```

Alternatively, run them through CTest inside the container:

```bash
docker run --rm resurs-native \
  ctest --test-dir /resurs-build --output-on-failure
```

## Open a shell in the image

```bash
docker run --rm -it --entrypoint /bin/bash resurs-native
```

The build artifacts are located in `/resurs-build` inside the container.
