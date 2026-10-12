package se.comerit.resurs.persistence.converter;

public interface PiiEncryptionInterface {

    byte[] encode(String plaintext);

    String decode(byte[] ciphertext);

}
