package se.comerit.resurs.util;

import org.springframework.stereotype.Component;
import se.comerit.resurs.config.EncryptionKey;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.Locale;

@Component
public class HMACHasher {

    private final EncryptionKey key;

    private static final String ALGORITHM = "HmacSHA256";

    public HMACHasher(EncryptionKey key) {
        this.key = key;
    }

    public byte[] encode(String string) {
        if (string == null) {
            return null;
        }


        try {
            Mac mac = Mac.getInstance(ALGORITHM);
            mac.init(new SecretKeySpec(key.hashKeyBytes(), ALGORITHM));

            return mac.doFinal(
                    string.getBytes(StandardCharsets.UTF_8)
            );
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException(
                    "Failed to hash", e);
        }
    }
}
