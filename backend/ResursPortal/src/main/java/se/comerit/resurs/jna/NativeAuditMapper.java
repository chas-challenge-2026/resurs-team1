package se.comerit.resurs.jna;


import com.sun.jna.Memory;
import com.sun.jna.platform.win32.BaseTSD;
import org.erdtman.jcs.JsonCanonicalizer;
import org.springframework.stereotype.Component;
import se.comerit.resurs.dto.audit.AuditEventSignaturePayloadDTO;
import se.comerit.resurs.persistence.model.AuditEvent;
import tools.jackson.databind.ObjectMapper;


import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HexFormat;

@Component
public class NativeAuditMapper {

    private final ObjectMapper objectMapper;

    public NativeAuditMapper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public NativeAuditEntryStruct toStruct(AuditEvent event) {


        try {
            AuditEventSignaturePayloadDTO payload = new AuditEventSignaturePayloadDTO(event);

            String json = objectMapper.writeValueAsString(payload);
            String canonicalJson =
                    new JsonCanonicalizer(json).getEncodedString();

            byte[] data = canonicalJson.getBytes(StandardCharsets.UTF_8);

            NativeAuditEntryStruct struct = new NativeAuditEntryStruct();

            Memory memory = new Memory(data.length);
            memory.write(0, data, 0, data.length);

            struct.setCanonicalDataMemory(memory);
            struct.canonicalData = memory;
            struct.canonicalDataLength =
                    new BaseTSD.SIZE_T(data.length);

            struct.sequenceNumber = event.getSequenceNumber();

            if (event.getPreviousHash() != null) {
                struct.previousHash =
                        event.getPreviousHash();
            }

            if (event.getCurrentHash() != null) {
                struct.currentHash =
                        event.getCurrentHash();
            }

            if (event.getSignature() != null) {
                byte[] signature =
                        event.getSignature();

                if (signature.length != 64) {
                    throw new IllegalArgumentException(
                            "Signature must be exactly 64 bytes");
                }

                struct.signature = signature;
            }

            struct.write();
            return struct;

        } catch (Exception e) {
            throw new IllegalStateException(
                    "Failed to map audit event " + event.getEventId(), e);
        }
    }

    public void toTargetStruct(AuditEvent event, NativeAuditEntryStruct targetStruct){
        NativeAuditEntryStruct mapped = this.toStruct(event);

        targetStruct.canonicalData = mapped.canonicalData;
        targetStruct.currentHash = mapped.currentHash;
        targetStruct.previousHash = mapped.previousHash;
        targetStruct.signature = mapped.signature;
        targetStruct.sequenceNumber = mapped.sequenceNumber;
        targetStruct.canonicalDataLength = mapped.canonicalDataLength;
        targetStruct.setCanonicalDataMemory(mapped.getCanonicalDataMemory());

        targetStruct.write();

    }



}