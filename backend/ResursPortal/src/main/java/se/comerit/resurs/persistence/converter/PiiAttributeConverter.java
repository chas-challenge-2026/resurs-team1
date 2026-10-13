package se.comerit.resurs.persistence.converter;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import se.comerit.resurs.jna.JNAEncryptionComponent;

@Converter
public class PiiAttributeConverter
        implements AttributeConverter<String, byte[]> {

    private final JNAEncryptionComponent encryption;

    public PiiAttributeConverter(JNAEncryptionComponent encryption) {
        this.encryption = encryption;
    }

    @Override
    public byte[] convertToDatabaseColumn(String attribute) {
        if (attribute == null) {
            return null;
        }
        return encryption.encode(attribute);
    }

    @Override
    public String convertToEntityAttribute(byte[] dbData) {
        if (dbData == null) {
            return null;
        }
        return encryption.decode(dbData);
    }
}