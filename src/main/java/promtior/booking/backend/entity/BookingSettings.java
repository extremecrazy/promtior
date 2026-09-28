package promtior.booking.backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import promtior.booking.backend.shared.Auditable;

import java.util.UUID;


@Getter
@Setter
@Entity
@Table(name = "booking_settings")
public class BookingSettings extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @NotNull
    @Min(1)
    @Column(name = "slot_minutes", nullable = false)
    private Integer slotMinutes;

    @NotNull
    @Min(1)
    @Column(name = "max_slots", nullable = false)
    private Integer maxSlots;
}
