package promtior.booking.backend.dto.room;

import java.util.UUID;

public record RoomResponse(
      UUID id,
      String name,
      Integer maxCapacity,
      Boolean active
) {
}
