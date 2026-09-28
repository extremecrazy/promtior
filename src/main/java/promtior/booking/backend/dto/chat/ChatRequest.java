package promtior.booking.backend.dto.chat;

import jakarta.validation.constraints.NotBlank;

public record ChatRequest(
      @NotBlank String message
) {
}
