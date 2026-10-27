package se.comerit.resurs.jna;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.jna.Memory;
import com.sun.jna.platform.win32.BaseTSD;
import org.erdtman.jcs.JsonCanonicalizer;
import org.springframework.stereotype.Component;
import se.comerit.resurs.dto.audit.AuditEventSignaturePayloadDTO;
import se.comerit.resurs.persistence.model.AuditEvent;


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
            var payload = new AuditEventSignaturePayloadDTO(event);

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
                        Base64.getDecoder().decode(event.getSignature());

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
}