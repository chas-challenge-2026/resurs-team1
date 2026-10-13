package se.comerit.resurs.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Base64;

@ConfigurationProperties(prefix = "encryption")
public record EncryptionKey(String cipherKey, String hashKey) {

    public byte[] cipherKeyBytes() {
        byte[] bytes = Base64.getDecoder().decode(cipherKey);

        if (bytes.length != 32) {
            throw new IllegalStateException(
                    "Encryption key must be exactly 32 bytes for AES-256"
            );
        }

        return bytes;
    }

    public byte[] hashKeyBytes() {
        byte[] bytes = Base64.getDecoder().decode(hashKey);

        if (bytes.length != 32) {
            throw new IllegalStateException(
                    "Hash key must be exactly 32 bytes for HmacSHA256"
            );
        }

        return bytes;
    }
}

