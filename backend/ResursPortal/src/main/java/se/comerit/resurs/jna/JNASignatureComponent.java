package se.comerit.resurs.jna;

import org.springframework.stereotype.Component;
import se.comerit.resurs.config.EncryptionKey;

@Component
public class JNASignatureComponent  {

        private final EncryptionKey encryptionKey;
        private final NativeSignatureInterface signatureLibrary;

        public JNASignatureComponent(EncryptionKey encryptionKey, NativeSignatureInterface signatureLibrary) {
            this.encryptionKey = encryptionKey;
            this.signatureLibrary = signatureLibrary;
        }




}
