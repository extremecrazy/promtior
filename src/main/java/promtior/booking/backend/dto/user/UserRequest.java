package promtior.booking.backend.dto.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record UserRequest(
      @NotBlank @Pattern(regexp = "^[a-zA-Z0-9]+$", message = "El campo solo puede contener letras y números, sin espacios ni caracteres especiales")String username,
      @NotBlank String password,
      @NotBlank @Pattern(regexp = "^[a-zA-Z0-9áéíóúÁÉÍÓÚñÑ ]+$", message = "El texto contiene caracteres no permitidos")String name
) {
}
