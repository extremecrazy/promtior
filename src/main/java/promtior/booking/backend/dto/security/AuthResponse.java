package promtior.booking.backend.dto.security;

public record AuthResponse(String token, String tokenType, String username) {

   public AuthResponse(String token, String username) {
      this(token, "Bearer", username);
   }
}
