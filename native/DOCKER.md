# Docker commands for native

Run these commands from the project root. Docker Desktop must be running.

## Build the native image

```bash
docker build -t resurs-crypto-demo ./native
```

The build compiles `resurs_crypto`, the demo program, and the tests. The tests
run automatically during the build, so `docker build` fails if a test fails.

Rebuild without using the Docker cache:

```bash
docker build --no-cache -t resurs-crypto-demo ./native
```

## Run the demo program

```bash
docker run --rm resurs-crypto-demo
```

This runs the executable built from `resurs_crypto/main.cpp`.

## Run the tests again

The tests can be run without rebuilding the image:

```bash
docker run --rm resurs-crypto-demo /resurs-build/resurs_crypto_tests
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
