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
        --target resurs_crypto_demo resurs_crypto_tests resurs_audit \
        --parallel \
    && ctest --test-dir /resurs-build --output-on-failure

RUN c++ -std=c++17 -Wall -Wextra -Wpedantic \
        -I resurs_audit \
        resurs_audit/Main.cpp \
        -L/resurs-build \
        -Wl,-rpath,/resurs-build \
        -lresurs_audit \
        -lcrypto \
        -o /resurs-build/resurs_audit_demo

CMD ["/resurs-build/resurs_audit_demo"]
