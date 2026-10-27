package se.comerit.resurs.jna;

import com.sun.jna.Memory;
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
public class NativeAuditEntryStruct extends Structure implements AutoCloseable {


    public Pointer canonicalData;
    public SIZE_T canonicalDataLength;
    public byte[] signature = new byte[64];
    public byte[] previousHash = new byte[32];
    public byte[] currentHash = new byte[32];
    public long sequenceNumber;

    private Memory canonicalDataMemory;

    public void setCanonicalDataMemory(Memory memory) {
        this.canonicalDataMemory = memory;
        this.canonicalData = memory;
    }

    @Override
    public void close() {
        if (canonicalDataMemory != null) {
            canonicalDataMemory.close();
            canonicalDataMemory = null;
            canonicalData = null;
        }
    }

}
