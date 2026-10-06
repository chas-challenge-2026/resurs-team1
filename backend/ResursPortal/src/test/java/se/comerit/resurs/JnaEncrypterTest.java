package se.comerit.resurs;

import org.junit.jupiter.api.Test;
import se.comerit.resurs.jna.JnaEncrypter;

import static org.junit.jupiter.api.Assertions.assertEquals;

class JnaEncrypterTest {

    @Test
    void helloReturnsStringLength() {
        assertEquals(13, JnaEncrypter.hello("I'm Different"));
    }
}

