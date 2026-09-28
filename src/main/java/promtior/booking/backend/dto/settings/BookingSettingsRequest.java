package promtior.booking.backend.dto.settings;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record BookingSettingsRequest(
      @NotNull @Min(1) Integer slotMinutes,
      @NotNull @Min(1) Integer maxSlots
) {
}
