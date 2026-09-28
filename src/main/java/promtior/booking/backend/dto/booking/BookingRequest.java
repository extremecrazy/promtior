package promtior.booking.backend.dto.booking;

import java.time.LocalDateTime;
import java.util.UUID;

import jakarta.validation.constraints.*;
import org.springframework.format.annotation.DateTimeFormat;

public record BookingRequest(
      @NotBlank @Size(min=1, max=255) @Pattern(regexp = "^[a-zA-Z0-9áéíóúÁÉÍÓÚñÑ ]+$", message = "El texto contiene caracteres no permitidos")String name,
      @NotNull UUID roomId,
      @NotNull LocalDateTime startTime,
      @NotNull LocalDateTime endTime,
      @NotNull @Min(1) @Max(200) Integer attendeeCount
) {
}
