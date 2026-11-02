package se.comerit.resurs.jna;

import com.sun.jna.Structure;

@Structure.FieldOrder({"resultCode", "index"})
public class VerifyChainResult extends Structure
        implements Structure.ByValue {
    public int resultCode;
    public long index;
}
