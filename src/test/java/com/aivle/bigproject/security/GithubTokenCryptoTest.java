package com.aivle.bigproject.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import java.util.Base64;
import org.junit.jupiter.api.Test;

class GithubTokenCryptoTest {

    @Test
    void encryptsAndDecryptsToken() {
        String key = Base64.getEncoder().encodeToString(new byte[32]);
        GithubTokenCrypto crypto = new GithubTokenCrypto(key);

        String first = crypto.encrypt("github-token");
        String second = crypto.encrypt("github-token");

        assertNotEquals("github-token", first);
        assertNotEquals(first, second);
        assertEquals("github-token", crypto.decrypt(first));
    }
}
