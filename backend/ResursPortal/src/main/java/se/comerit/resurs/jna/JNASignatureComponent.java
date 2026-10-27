package se.comerit.resurs.jna;

import com.sun.jna.platform.win32.BaseTSD;
import org.erdtman.jcs.JsonCanonicalizer;
import org.springframework.stereotype.Component;
import se.comerit.resurs.config.EncryptionKey;
import se.comerit.resurs.dto.audit.AuditEventSignaturePayloadDTO;
import se.comerit.resurs.exception.jna.JnaSigningException;
import se.comerit.resurs.persistence.model.AuditEvent;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.List;

@Component
public class JNASignatureComponent  {

        private final EncryptionKey encryptionKey;
        private final NativeSignatureInterface signatureLibrary;
        private final ObjectMapper jsonMapper;
        private final NativeAuditMapper nativeAuditMapper;

        public JNASignatureComponent(EncryptionKey encryptionKey, NativeSignatureInterface signatureLibrary, ObjectMapper jsonMapper, NativeAuditMapper nativeAuditMapper) {
            this.encryptionKey = encryptionKey;
            this.signatureLibrary = signatureLibrary;
            this.jsonMapper = jsonMapper;
            this.nativeAuditMapper = nativeAuditMapper;
        }

        public byte[] generatePrivateKey() {

            byte[] privateKey = new byte[32];
            signatureLibrary.generate_private_key(privateKey);
            return privateKey;
        }

        public byte[] getPublicKey(byte[] privateKey){
            byte[] publicKey = new byte[32];
            signatureLibrary.get_public_key(privateKey,publicKey);
            return publicKey;
        }

        public SignatureOutput hashAndSign(AuditEvent event) throws IOException {

            byte[] newHash = new byte[32];
            byte[] newSignature = new byte[64];

            String json = jsonMapper.writeValueAsString(new AuditEventSignaturePayloadDTO(event));

            String payload = new JsonCanonicalizer(json).getEncodedString();
            byte[] canonicalData = payload.getBytes();

            //Status codes for exceptions

            int statusCode = signatureLibrary.wrapper_hash_and_sign(
                    canonicalData,new BaseTSD.SIZE_T(canonicalData.length),
                    encryptionKey.hashKeyBytes(),new BaseTSD.SIZE_T(encryptionKey.hashKeyBytes().length),
                    newHash,newSignature
                    );

            if(statusCode != 0){
                throw new JnaSigningException("a JNA Signature component Failed");
            }


            return new SignatureOutput(newHash,newSignature);
        }

    public VerifyChainResult verifyChain(List<AuditEvent> auditEvents) {
        NativeAuditEntryStruct[] entries = auditEvents.stream().map(nativeAuditMapper::toStruct)
                .toArray(NativeAuditEntryStruct[]::new);

        try {
            byte[] publicKey = getPublicKey(encryptionKey.hashKeyBytes());

            return signatureLibrary.wrapper_verify_chain(
                    entries,
                    new BaseTSD.SIZE_T(entries.length),
                    publicKey,
                    new BaseTSD.SIZE_T(publicKey.length)
            );
        } finally {
            for (NativeAuditEntryStruct entry : entries) {
                //Close the structs Memory field
                entry.close();
            }
        }
    }

}




