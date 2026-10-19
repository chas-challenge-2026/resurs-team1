package se.comerit.resurs.jna;

import com.sun.jna.Pointer;
import com.sun.jna.Structure;
import com.sun.jna.platform.win32.BaseTSD.SIZE_T;

@Structure.FieldOrder({
        "canonicalData",
        "canonicalDataLength",
        "signature",
        "previousHash",
        "currentHash",
        "sequenceNumber"
})
public class NativeAuditEntryStruct extends Structure {
    public Pointer canonicalData;
    public SIZE_T canonicalDataLength;
    public byte[] signature = new byte[64];
    public byte[] previousHash = new byte[32];
    public byte[] currentHash = new byte[32];
    public long sequenceNumber;
}
