package se.comerit.resurs.jna;

import com.sun.jna.Library;
import com.sun.jna.platform.win32.BaseTSD.SIZE_T;

public interface NativeSignatureInterface extends Library {


    void generate_private_key(byte[] outputPrivateKey);

    int get_public_key(
            byte[] privateKey,
            byte[] outputPublicKey);

    int wrapper_hash_and_sign(
            byte[] canonicalData,
            SIZE_T  canonicalDataLength,
            byte[] privateKey,
            SIZE_T  privateKeyLength,
            byte[] outputHash,
            byte[] outputSignature);

    VerifyChainResult wrapper_verify_chain(
            NativeAuditEntryStruct[] entries,
            SIZE_T  entryCount,
            byte[] publicKey,
            SIZE_T  publicKeyLength);
}