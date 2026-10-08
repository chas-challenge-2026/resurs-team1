package se.comerit.resurs.jna;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Base64;

@ConfigurationProperties(prefix = "encryption")
public record EncryptionKey(String key) {

    public byte[] keyBytes() {
        byte[] bytes = Base64.getDecoder().decode(key);

        if (bytes.length != 32) {
            throw new IllegalStateException(
                    "Encryption key must be exactly 32 bytes for AES-256"
            );
        }

        return bytes;
    }
}

