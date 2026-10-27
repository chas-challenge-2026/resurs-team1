package se.comerit.resurs.service;

import org.springframework.stereotype.Service;
import se.comerit.resurs.dto.audit.ChainVerificationResponse;
import se.comerit.resurs.exception.jna.JnaSigningException;
import se.comerit.resurs.jna.JNASignatureComponent;
import se.comerit.resurs.jna.SignatureOutput;
import se.comerit.resurs.jna.VerifyChainResult;
import se.comerit.resurs.persistence.model.AuditEvent;

import java.io.IOException;
import java.util.List;

@Service
public class AuditSigningService {

    private final JNASignatureComponent signatureComponent;

    public AuditSigningService(JNASignatureComponent signatureComponent) {
        this.signatureComponent = signatureComponent;
    }

    public void hashAndSign(AuditEvent event){

        SignatureOutput jnaOutput;

        try{
            jnaOutput = signatureComponent.hashAndSign(event);
        } catch (IOException e) {
            throw new JnaSigningException("Could not create digital signature");
        }

        event.setCurrentHash(jnaOutput.newHash());
        event.setSignature(jnaOutput.newSignature());
    }

    public ChainVerificationResponse verifyChain(List<AuditEvent> chain){
        VerifyChainResult results = signatureComponent.verifyChain(chain);

        String responseText = "";
        Long failedIndex = null;
        switch (results.resultCode){
            case 0 : responseText = "Chain Verified Passed";
            case -1 : responseText = "Invalid Argument, Null parameter";
            case -2 : responseText = "Zero length";
            case -3 : responseText = "Public key length missmatch";
            case -4 : {
                    responseText = "An entry has a null canonical-data reference.";
                    failedIndex = results.index;
            }
            case -5 : {
                responseText = "An entry has a zero canonical-data length.";
                failedIndex = results.index;
            }
            case -6 : {
                responseText = "The previous-hash link is invalid.";
                failedIndex = results.index;
            }
            case -7 : {
                responseText = "The chain does not start at 1 or has a duplicate, gap, or ordering error.";
                failedIndex = results.index;
            }
            case -8 : {
                responseText = "Canonical bytes do not match the stored current hash.";
                failedIndex = results.index;
            }
            case -9 : {
                responseText = "OpenSSL could not initialize verification.";
                failedIndex = results.index;
            }
            case -10 : {
                responseText = "The Ed25519 signature is invalid or verification failed.";
                failedIndex = results.index;
            }
            case -11 : responseText = "The public key could not be converted to an OpenSSL Ed25519 key.";
            case -12 : responseText = "An exception or other internal failure was contained by the native boundary.";
        }


        return new ChainVerificationResponse(results.resultCode, responseText,failedIndex);


    }

}
