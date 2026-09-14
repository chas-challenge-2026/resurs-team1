FROM ubuntu:24.04

RUN apt-get update \
    && apt-get install -y --no-install-recommends build-essential cmake libssl-dev \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /src
COPY . .

RUN cmake -S . -B /resurs-build \
        -DCMAKE_BUILD_TYPE=Release \
        -DBUILD_TESTING=ON \
        -DRESURS_BUILD_CRYPTO_DEMO=ON \
    && cmake --build /resurs-build \
        --target resurs_crypto_demo resurs_crypto_tests \
        --parallel \
    && ctest --test-dir /resurs-build --output-on-failure

CMD ["/resurs-build/resurs_crypto_demo"]
