# native/ – C/C++ Moduler för v2

Denna katalog innehåller (i v2) C/C++ nativmodulerna som anropas från Java via JNA (Java Native Access).

## Planerade moduler

### 1. PDF-parser: `libresurs_pdf.so`

Parsar K2- och K3-årsredovisningar samt F-skatteintyg i PDF-format.

**Syfte:** I v1 sparas PDF-filer men parsas aldrig (`DocumentController.java`, kommentar: `// TODO: implement PDF parsing in v2`). Scoring baseras på manuellt inmatade nyckeltal utan verifiering mot faktisk årsredovisning.

**Funktioner:**
```c
// Extrahera finansiella nyckeltal från K2/K3-årsredovisning
int resurs_parse_arsredovisning(
    const char* pdf_path,
    double* eget_kapital,
    double* totalt_kapital,
    double* omsattningstillgangar,
    double* kortfristiga_skulder,
    double* totala_skulder,
    double* rorelseresultat,
    double* nettoomsattning
);

// Verifiera F-skatteintyg
int resurs_verify_fskatt(
    const char* pdf_path,
    const char* org_number,
    char* status_out,  // "ACTIVE" | "INACTIVE" | "INVALID"
    size_t status_len
);
```

**JNA Bridge (Java):**
```java
public interface ResursPdfLibrary extends Library {
    ResursPdfLibrary INSTANCE = Native.load("resurs_pdf", ResursPdfLibrary.class);

    int resurs_parse_arsredovisning(
        String pdfPath,
        DoubleByReference egetKapital,
        DoubleByReference totaltKapital,
        // ... övriga parametrar
    );
}
```

### 2. PII-kryptering: `libresurs_crypto.so`

AES-256-GCM kryptering av PII-fält på hot path.

**Syfte:** I v1 lagras firmanamn, organisationsnummer och firmatecknare i klartext (`ApplicationController.java`, kommentar: `// TODO: encrypt PII before go-live`).

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

**Nyckellagring:**
- Nyckel lagras separat från databasen (HashiCorp Vault eller AWS KMS)
- Nonce genereras per krypteringsoperation och lagras tillsammans med ciphertext

## Kompilering

```bash
# PDF-parser (kräver libpoppler-dev)
gcc -shared -fPIC -o libresurs_pdf.so resurs_pdf.c -lpoppler -lpoppler-glib

# PII-kryptering (kräver libssl-dev)
gcc -shared -fPIC -o libresurs_crypto.so resurs_crypto.c -lssl -lcrypto
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

4. Anropa via `Native.load("resurs_pdf", ResursPdfLibrary.class)`

## Status

- [ ] libresurs_pdf.so — ej implementerad (v2)
- [ ] libresurs_crypto.so — ej implementerad (v2)
- [ ] JNA bridge — ej implementerad (v2)
