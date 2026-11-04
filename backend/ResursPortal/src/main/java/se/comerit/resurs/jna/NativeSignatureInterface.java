package se.comerit.resurs.jna;

import com.sun.jna.Library;
import com.sun.jna.platform.win32.BaseTSD.SIZE_T;

public interface NativeSignatureInterface extends Library {


    int generate_private_key(byte[] outputPrivateKey, SIZE_T  privateKeyLength);

    int get_public_key(
            byte[] privateKey,
            SIZE_T  privateKeyLength,
            byte[] outputPublicKey,
            SIZE_T  publicKeyLength);

    int wrapper_hash_and_sign(
            byte[] canonicalData,
            SIZE_T  canonicalDataLength,
            byte[] privateKey,
            SIZE_T  privateKeyLength,
            byte[] outputHash,
            SIZE_T  outputHashLength,
            byte[] outputSignature,
            SIZE_T outputSignatureLength);

    VerifyChainResult wrapper_verify_chain(
            NativeAuditEntryStruct[] entries,
            SIZE_T  entryCount,
            byte[] publicKey,
            SIZE_T  publicKeyLength);
}