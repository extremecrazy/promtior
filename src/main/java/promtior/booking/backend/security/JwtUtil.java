package promtior.booking.backend.security;

import java.util.Date;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import promtior.booking.backend.entity.User;


@Component
public class JwtUtil {

   @Value("${jwt.secret}")
   private String secret;

   @Value("${jwt.expiration-ms}")
   private long expirationMs;

   private SecretKey key() {
      return Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret));
   }

   public String generateToken(User user) {
      Date now = new Date();
      return Jwts.builder()
            .subject(user.getUsername())
            .claim("userId", user.getId().toString())
            .issuedAt(now)
            .expiration(new Date(now.getTime() + expirationMs))
            .signWith(key())
            .compact();
   }

   public String extractUsername(String token) {
      return getClaims(token).getSubject();
   }

   public boolean isTokenValid(String token) {
      try {
         getClaims(token);
         return true;
      } catch (JwtException | IllegalArgumentException e) {
         return false;
      }
   }

   private Claims getClaims(String token) {
      return Jwts.parser().verifyWith(key()).build().parseSignedClaims(token).getPayload();
   }
}
