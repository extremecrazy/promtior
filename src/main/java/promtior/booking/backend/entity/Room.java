package promtior.booking.backend.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import promtior.booking.backend.shared.Auditable;


import java.util.List;
import java.util.UUID;


/** Validaciones en espejo de {@code dto.room.RoomRequest} — ver CLAUDE.md. */
@Getter
@Setter
@Entity
@Table(name = "rooms")
public class Room extends Auditable {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @NotBlank
    @Column(nullable = false)
    private String name;

    @NotNull
    @Min(1)
    @Column(nullable = false)
    private Integer maxCapacity;

    /** Una sala deshabilitada no admite reservas nuevas (ver {@code BookingService}), pero conserva su historial. */
    @NotNull
    @Column(nullable = false)
    private Boolean active = true;

    @OneToMany(mappedBy = "room", cascade = CascadeType.ALL)
    private List<Booking> bookings;

}
