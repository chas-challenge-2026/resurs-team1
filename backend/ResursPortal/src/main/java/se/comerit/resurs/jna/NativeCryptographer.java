package se.comerit.resurs.jna;

import com.sun.jna.Library;
import com.sun.jna.Native;

public interface NativeCryptographer extends Library {

    NativeCryptographer INSTANCE =
            Native.load("mylibrary", NativeCryptographer.class);

    String encode(String data, String key);

}
