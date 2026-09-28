package promtior.booking.backend.dto.room;

import jakarta.validation.constraints.NotNull;

public record RoomActiveRequest(
      @NotNull Boolean active
) {
}
