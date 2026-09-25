package ro.micronikgrupm.mcp.client.security;

import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/** Auto-generated documentation. */
@Service
public class JwtService {

  private final SecretKey key;
  private final long expirationMinutes;

  public JwtService(
      @Value("${jwt.secret}") String secret,
      @Value("${jwt.expiration-minutes}") long expirationMinutes) {
    this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    this.expirationMinutes = expirationMinutes;
  }

  /** Handles generateToken operation. */
  public String generateToken(String username, String role) {
    Instant now = Instant.now();
    Instant expiresAt = now.plus(Duration.ofMinutes(expirationMinutes));
    return Jwts.builder()
        .subject(username)
        .claim("role", role)
        .claim("iat", now.getEpochSecond())
        .claim("exp", expiresAt.getEpochSecond())
        .signWith(key)
        .compact();
  }

  public String extractUsername(String token) {
    return parse(token).getPayload().getSubject();
  }

  public String extractRole(String token) {
    return parse(token).getPayload().get("role", String.class);
  }

  /** Handles isTokenValid operation. */
  public boolean isTokenValid(String token) {
    try {
      parse(token);
      return true;
    } catch (JwtException | IllegalArgumentException e) {
      return false;
    }
  }

  private io.jsonwebtoken.Jws<io.jsonwebtoken.Claims> parse(String token) {
    return Jwts.parser().verifyWith(key).build().parseSignedClaims(token);
  }
}
