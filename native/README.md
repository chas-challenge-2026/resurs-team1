# Native modules

The native part of Resurs contains two C++17 shared libraries called from Java
through JNA: `resurs_crypto` for AES-256-GCM encryption and `resurs_audit` for
SHA-256 hashing, Ed25519 signing and audit-chain verification. Both require
OpenSSL 3.x.

This README is the starting point for native development. Use the module guides
for API details and the build/test guides for configuration, CI and troubleshooting.

## Documentation

| Area | Module documentation | Java integration |
|---|---|---|
| Encryption: `resurs_crypto` | [Crypto README](resurs_crypto/README.md) | [Crypto JNA integration](resurs_crypto/JNA_INTEGRATION.md) |
| Audit: `resurs_audit` | [Audit README](resurs_audit/README.md) | [Audit JNA integration](resurs_audit/JNA_INTEGRATION.md) |

| Build and test guide | What it covers |
|---|---|
| [Native developer guide](../docs/README.md) | Configure, build and test stages, settings, test coverage, local commands, troubleshooting and adding tests. |
| [Native CI](../docs/native-ci.md) | GitHub Actions triggers, change detection, Ubuntu jobs, reports and the `Native CI result` check. |
| [Docker guide](DOCKER.md) | Local Linux builds on Ubuntu 22.04 and 24.04, demos, test commands and optional audit sanitizers. |

## Encryption

`resurs_crypto` encrypts and decrypts binary data with AES-256-GCM. Encryption
generates an IV and authentication tag; decryption checks authenticity before
the caller can use the result. The application stores the IV and tag alongside
the ciphertext and manages keys separately.

The supported native entry points are `aes_256_gcm_encrypt` and
`aes_256_gcm_decrypt`. See the [Crypto README](resurs_crypto/README.md) for
behavior and result codes, and the [JNA guide](resurs_crypto/JNA_INTEGRATION.md)
for Java declarations, binary sizes and buffer handling.

## Audit

`resurs_audit` hashes canonical bytes, signs the hash with Ed25519 and verifies
ordered audit chains. Verification checks previous-hash links, sequence numbers,
hashes and signatures, stopping at the first failure.

Java constructs and canonicalizes the unsigned audit JSON according to RFC 8785
and passes its UTF-8 bytes to native. It also owns persistence, chain ordering
and key management. Native treats the data as opaque bytes.

The supported native entry points are `generate_private_key`, `get_public_key`,
`wrapper_hash_and_sign` and `wrapper_verify_chain`. See the
[Audit README](resurs_audit/README.md) for the native contract, and the
[JNA guide](resurs_audit/JNA_INTEGRATION.md) for structures, memory handling,
result codes and complete-chain requirements. The guides also explain the
additional trusted information needed to detect removal of the chain's tail.

## Build and test

[CMakeLists.txt](CMakeLists.txt) defines both libraries, optional demos and the
CTest test executables. A direct native build requires a C++17 compiler,
CMake 3.20 or later and OpenSSL 3.x development files. The CI commands that produce
JUnit XML require CMake 3.21 or later.

For local Linux verification through Docker, run from the repository root with
Docker available in Git Bash, WSL or Bash:

```bash
bash native/test-linux.sh
```

The script builds and tests both Ubuntu 22.04 and 24.04 in Debug mode. On Windows,
start Docker Desktop first. For individual Docker commands that also work in
PowerShell, see the [Docker guide](DOCKER.md). For direct CMake/CTest commands,
see the [developer guide](../docs/README.md#run-locally).
For native Windows builds, see [Windows with Visual Studio](../docs/README.md#windows-with-visual-studio).

The four CTest registrations cover crypto behavior, audit behavior and required
exported functions in each built library. Native CI configures, compiles and
runs them on both Ubuntu versions. Both demos are compiled but are not run as
CI tests. Read [Native CI](../docs/native-ci.md) for when checks run and how to
read their results.

Native CI checks newly built Linux libraries. Java/JNA integration, Windows
DLLs, packaged prebuilt files and deployment need separate verification; a
passing native run does not validate those parts.

## Source layout

```text
native/
  README.md                    Native overview and documentation links
  CMakeLists.txt               Libraries, demos and test registrations
  Dockerfile                   Linux build and test image
  DOCKER.md                    Docker commands and runtime compatibility
  test-linux.sh                Local verification on both Ubuntu versions
  resurs_crypto/               Encryption source, API, JNA guide and prebuilts
  resurs_audit/                Audit source, API, JNA guide and prebuilts
  tests/                       Behavior and exported-symbol tests
```

The calling application owns native-resource packaging and deployment. Use the
module's JNA guide to select and load the appropriate library for its runtime.

## Original documentation

The [original native README](../docs/old%20native%20documentation/README.md) and
[original case documents](../docs/old%20documentation/) are preserved as
background and requirements material. They describe the original case and its
targets rather than the current native API or implementation status.

Return to the [project README](../README.md).
