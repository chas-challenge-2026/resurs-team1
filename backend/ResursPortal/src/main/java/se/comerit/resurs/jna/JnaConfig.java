package se.comerit.resurs.jna;

import com.sun.jna.Native;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Base64;

@Configuration
public class JnaConfig {

    @Bean
    NativeCryptographerInterface cryptoLibraryInstance(){
        return Native.load(
                "resurs_crypto",
                NativeCryptographerInterface.class);
    }
    @Bean
    NativeSignatureInterface signatureLibraryInstance(){
        return Native.load(
                "return_audit",
                NativeSignatureInterface.class);
    }

}
