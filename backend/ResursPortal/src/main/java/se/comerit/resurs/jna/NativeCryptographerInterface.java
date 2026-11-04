package se.comerit.resurs.jna;

import com.sun.jna.Library;
import com.sun.jna.Native;

public interface NativeCryptographerInterface extends Library {

    int aes_256_gcm_encrypt(
            byte[] plainText,
            int plaintext_len,
            byte[] aes_key,
            int aes_key_len,
            byte[] iv,
            int iv_len,
            byte[] ciphertext,
            int ciphertext_len,
            byte[] tag,
            int tag_len);

    int aes_256_gcm_decrypt(
            byte[] ciphertext,
            int ciphertext_len,
            byte[] aes_key,
            int aes_key_len,
            byte[] iv,
            int iv_len,
            byte[] plaintext,
            int plaintext_len,
            byte[] tag,
            int tag_len);
}

