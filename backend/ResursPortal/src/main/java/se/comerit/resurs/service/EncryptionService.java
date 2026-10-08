package se.comerit.resurs.service;

import org.springframework.stereotype.Service;
import se.comerit.resurs.exception.jna.EncryptionException;
import se.comerit.resurs.jna.EncryptionKey;
import se.comerit.resurs.jna.NativeCryptographerInterface;

import java.nio.ByteBuffer;

@Service
public class EncryptionService {
    private final NativeCryptographerInterface cryptoLibrary;
    private final EncryptionKey aesKey;

    private int ivLength = 12;
    private int tagLength = 16;

    public EncryptionService(NativeCryptographerInterface cryptoLibrary, EncryptionKey aesKey) {
        this.cryptoLibrary = cryptoLibrary;
        this.aesKey = aesKey;
    }


    public byte[] encode(String plainText){
       byte[] plainTextBytes =  plainText.getBytes();
       return cLibraryEncode(plainTextBytes,aesKey);
    }

    public String decode(byte[] encryptedData){
        return new String(cLibrarydecode(encryptedData,aesKey));
    }


    private byte[] cLibraryEncode(
            byte[] plainText,
            EncryptionKey aesKey
    ) {
        byte[] initVector = new byte[ivLength];
        byte[] gcmTag = new byte[tagLength];
        byte[] cipherText = new byte[plainText.length];
        int statusCode;

        statusCode = cryptoLibrary.aes_256_gcm_encrypt(
                plainText,
                plainText.length,
                aesKey.keyBytes(),
                aesKey.keyBytes().length,
                initVector,
                initVector.length,
                cipherText,
                cipherText.length,
                gcmTag,
                gcmTag.length
        );

        byte[] encryptedPayload = ByteBuffer.allocate(
                gcmTag.length+initVector.length+cipherText.length)
                .put(initVector)
                .put(cipherText)
                .put(gcmTag)
                .array();


        return switch (statusCode) {
            case 0 ->
                // Success
                    encryptedPayload;
            case -1 -> throw new EncryptionException("Crypto: Invalid Argument");
            case -2 -> throw new EncryptionException("Invalid Buffer Size");
            case -3 -> throw new EncryptionException("Crypto Authentication Failed");
            default -> throw new EncryptionException(
                    "Unknown encryption error: " + statusCode
            );
        };
    }

    private byte[] cLibrarydecode(
            byte[] encryptedPayload,
            EncryptionKey aesKey
    ) {
        byte[] initVector = new byte[ivLength];
        byte[] gcmTag = new byte[tagLength];
        byte[] plainText = new byte[encryptedPayload.length-ivLength-tagLength];
        byte[] cipherText = new byte[plainText.length];
        int statusCode;

        ByteBuffer buffer = ByteBuffer.wrap(encryptedPayload);
        buffer.get(initVector);
        buffer.get(cipherText);
        buffer.get(gcmTag);

        statusCode = cryptoLibrary.aes_256_gcm_decrypt(
                cipherText,
                cipherText.length,
                aesKey.keyBytes(),
                aesKey.keyBytes().length,
                initVector,
                initVector.length,
                plainText,
                plainText.length,
                gcmTag,
                gcmTag.length
        );

        return switch (statusCode) {
            case 0 ->
                // Success
                    plainText;
            case -1 -> throw new EncryptionException("Crypto: Invalid Argument");
            case -2 -> throw new EncryptionException("Invalid Buffer Size");
            case -3 -> throw new EncryptionException("Crypto: Authentication Failed");
            default -> throw new EncryptionException(
                    "Unknown encryption error: " + statusCode
            );
        };



    }

}
