package promtior.booking.backend.dto.room;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record RoomRequest(
      @NotBlank String name,
      @NotNull @Min(1) Integer maxCapacity
) {
}
