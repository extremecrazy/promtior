package promtior.booking.backend.dto.booking;

import java.util.UUID;

import jakarta.validation.constraints.NotNull;

public record UnifyBookingRequest(
      @NotNull UUID otherBookingId
) {
}
