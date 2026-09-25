package ro.micronikgrupm.mcp.client.security;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class JwtServiceTest {

    private final JwtService jwtService = new JwtService(
            "d0a3f2b6c9e1487fa4c3e9b1728d4f6a0c5e8b3d7f2a9c4e6b1d8f3a5c7e9b2d", 60);

    @Test
    void generatesAndParsesToken() {
        String token = jwtService.generateToken("admin", "ADMIN");

        assertThat(jwtService.isTokenValid(token)).isTrue();
        assertThat(jwtService.extractUsername(token)).isEqualTo("admin");
        assertThat(jwtService.extractRole(token)).isEqualTo("ADMIN");
    }

    @Test
    void rejectsGarbageToken() {
        assertThat(jwtService.isTokenValid("not-a-real-token")).isFalse();
    }

    @Test
    void rejectsTokenFromDifferentSecret() {
        JwtService otherService = new JwtService(
                "0000000000000000000000000000000000000000000000000000000000000000", 60);
        String token = otherService.generateToken("admin", "ADMIN");

        assertThat(jwtService.isTokenValid(token)).isFalse();
    }
}
