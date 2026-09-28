package promtior.booking.backend.dto.settings;

import java.util.UUID;

public record BookingSettingsResponse(
      UUID id,
      Integer slotMinutes,
      Integer maxSlots
) {
}
