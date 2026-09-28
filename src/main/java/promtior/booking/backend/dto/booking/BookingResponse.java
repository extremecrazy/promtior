package promtior.booking.backend.dto.booking;

import java.time.LocalDateTime;
import java.util.UUID;

import promtior.booking.backend.enums.BookingStatus;

public record BookingResponse(
      UUID bookingId,
      String name,
      UUID roomId,
      String roomName,
      UUID userId,
      String userName,
      LocalDateTime startTime,
      LocalDateTime endTime,
      Integer attendeeCount,
      BookingStatus status
) {
}
