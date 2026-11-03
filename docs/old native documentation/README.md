# native/, C/C++ Moduler för v2

Denna katalog innehåller (i v2) C/C++ nativmodulerna som anropas från Java via JNA (Java Native Access).

## Planerade moduler

### 1. PII-kryptering: `libresurs_crypto.so`

AES-256-GCM kryptering av känsliga uppgifter (org.nr, personuppgifter, finansiell info) på hot path.

**Syfte:** I v1 lagras firmanamn, organisationsnummer och firmatecknare i klartext (`ApplicationController.java`, kommentar: `// TODO: encrypt PII before go-live`). Nyckeln ska lagras separat från databasen, inte i samma förvaringsutrymme som ciphertext.

**Funktioner:**
```c
// Kryptera PII-sträng
int resurs_encrypt_pii(
    const char* plaintext,
    const unsigned char* key,       // 32 bytes (AES-256)
    const unsigned char* nonce,     // 12 bytes (GCM)
    unsigned char* ciphertext_out,
    size_t* ciphertext_len
);

// Dekryptera PII-sträng
int resurs_decrypt_pii(
    const unsigned char* ciphertext,
    size_t ciphertext_len,
    const unsigned char* key,
    const unsigned char* nonce,
    char* plaintext_out,
    size_t* plaintext_len
);
```

**JNA Bridge (Java):**
```java
public interface ResursCryptoLibrary extends Library {
    ResursCryptoLibrary INSTANCE = Native.load("resurs_crypto", ResursCryptoLibrary.class);

    int resurs_encrypt_pii(
        String plaintext,
        byte[] key,
        byte[] nonce,
        byte[] ciphertextOut,
        IntByReference ciphertextLen
    );
}
```

**Nyckellagring:**
- Nyckel lagras separat från databasen (HashiCorp Vault eller AWS KMS)
- Nonce genereras per krypteringsoperation och lagras tillsammans med ciphertext

### 2. Audit-signering: `libresurs_audit.so`

Säker signering av audit-loggen med hashkedjor, för att upptäcka manipulation i efterhand.

**Syfte:** I v1 är audit-loggen osignerad (JSON-blob i en kolumn utan index). En rad kan ändras eller raderas i efterhand utan att det syns. I v2 ska varje audit-post hashas ihop med föregående posts hash (hashkedja) och signeras, så att manipulation av en enskild post eller av kedjans ordning går att upptäcka vid verifiering.

**Funktioner:**
```c
// Beräkna hash för en audit-post och kedja den till föregående post
int resurs_audit_chain_entry(
    const unsigned char* prev_hash,     // 32 bytes, SHA-256 av föregående post (NULL för första posten i kedjan)
    const char* entry_json,             // audit-postens innehåll: tidsstämpel, regel-ID, indata, utfall
    size_t entry_len,
    unsigned char* hash_out,            // 32 bytes, SHA-256(prev_hash || entry_json)
    unsigned char* signature_out,       // digital signatur av hash_out
    size_t* signature_len
);

// Verifiera en kedja av audit-poster, hittar första manipulerade posten om någon
int resurs_audit_verify_chain(
    const unsigned char* hashes,        // entry_count * 32 bytes, hashkedjan i ordning
    const unsigned char* signatures,    // signaturer i samma ordning
    const size_t* signature_lens,
    size_t entry_count,
    const unsigned char* public_key,
    int* first_invalid_index            // -1 om kedjan är giltig, annars index på första manipulerade posten
);
```

**JNA Bridge (Java):**
```java
public interface ResursAuditLibrary extends Library {
    ResursAuditLibrary INSTANCE = Native.load("resurs_audit", ResursAuditLibrary.class);

    int resurs_audit_chain_entry(
        byte[] prevHash,
        String entryJson,
        int entryLen,
        byte[] hashOut,
        byte[] signatureOut,
        IntByReference signatureLen
    );

    int resurs_audit_verify_chain(
        byte[] hashes,
        byte[] signatures,
        int[] signatureLens,
        int entryCount,
        byte[] publicKey,
        IntByReference firstInvalidIndex
    );
}
```

**Nyckellagring:**
- Signeringsnyckeln (privat nyckel) lagras separat från databasen, samma princip som för PII-kryptering
- Publik nyckel kan distribueras fritt för verifiering, t.ex. till revisor eller tillsynsmyndighet

## Kompilering

```bash
# PII-kryptering (kräver libssl-dev)
gcc -shared -fPIC -o libresurs_crypto.so resurs_crypto.c -lssl -lcrypto

# Audit-signering (kräver libssl-dev)
gcc -shared -fPIC -o libresurs_audit.so resurs_audit.c -lssl -lcrypto
```

## JNA Integration Guide

1. Lägg till JNA i pom.xml:
```xml
<dependency>
    <groupId>net.java.dev.jna</groupId>
    <artifactId>jna</artifactId>
    <version>5.13.0</version>
</dependency>
```

2. Placera `.so`-filer i `/usr/local/lib/` eller ange sökväg via `-Djna.library.path`

3. Definiera Java-interface som extends `Library`

4. Anropa via `Native.load("resurs_crypto", ResursCryptoLibrary.class)` respektive `Native.load("resurs_audit", ResursAuditLibrary.class)`

## Status

- [ ] libresurs_crypto.so, ej implementerad (v2)
- [ ] libresurs_audit.so, ej implementerad (v2)
- [ ] JNA bridge, ej implementerad (v2)
