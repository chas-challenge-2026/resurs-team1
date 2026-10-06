package se.comerit.resurs.jna;

import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.Platform;

public class JnaEncrypter {

    public interface CLibrary extends Library {
        CLibrary INSTANCE = Native.load(
                "msvcrt",
                CLibrary.class
        );

        long strlen(String str);
    }

    public static long hello(String str) {
        return CLibrary.INSTANCE.strlen(str);
    }

}
