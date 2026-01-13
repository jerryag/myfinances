package br.com.infotech.myfinances.util;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class CryptUtilsTest {

    @Test
    void testEncrypt() {
        String input = "testPassword";
        // SHA-256 hash de "testPassword"
        String expectedHash = "fd5cb51bafd60f6fdbedde6e62c473da6f247db271633e15919bab78a02ee9eb";

        String result = CryptUtils.encrypt(input);

        assertNotNull(result);
        assertEquals(expectedHash, result);
    }

    @Test
    void testEncryptNull() {
        assertNull(CryptUtils.encrypt(null));
    }

    @Test
    void testEncryptSameInputProduceSameOutput() {
        String input = "sameInput";
        String run1 = CryptUtils.encrypt(input);
        String run2 = CryptUtils.encrypt(input);

        assertEquals(run1, run2);
    }
}
