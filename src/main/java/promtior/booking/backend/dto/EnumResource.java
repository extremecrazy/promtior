package promtior.booking.backend.dto;

/**
 * Representación traducida de un valor de enum, expuesta por el endpoint
 * genérico {@code GET /enums/{enumName}} (ver {@link promtior.booking.backend.controller.EnumController}).
 */
public record EnumResource(String id, String description) {
}
