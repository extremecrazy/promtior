package promtior.booking.backend.dto.user;

import java.util.UUID;


public record UserResponse(
      UUID id,
      String username,
      String name
) {
}
