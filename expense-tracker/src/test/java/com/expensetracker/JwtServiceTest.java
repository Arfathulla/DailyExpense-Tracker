package com.expensetracker;

import com.expensetracker.security.JwtService;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class JwtServiceTest {
    private final JwtService jwt = new JwtService("a-test-secret-that-is-at-least-32-bytes-long", 60_000);

    @Test
    void roundTripReturnsUserId() {
        String token = jwt.generate(42L, "a@b.com");
        assertEquals(42L, jwt.parseUserId(token));
    }

    @Test
    void tamperedTokenIsRejected() {
        String token = jwt.generate(42L, "a@b.com");
        int i = token.lastIndexOf('.') + 5;                       // a character inside the signature
        char swapped = token.charAt(i) == 'A' ? 'B' : 'A';
        String tampered = token.substring(0, i) + swapped + token.substring(i + 1);
        assertThrows(JwtException.class, () -> jwt.parseUserId(tampered));
    }

    @Test
    void expiredTokenIsRejected() {
        JwtService shortLived = new JwtService("a-test-secret-that-is-at-least-32-bytes-long", -1000);
        assertThrows(JwtException.class, () -> shortLived.parseUserId(shortLived.generate(1L, "a@b.com")));
    }
}
