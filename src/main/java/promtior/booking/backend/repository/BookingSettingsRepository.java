package promtior.booking.backend.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import promtior.booking.backend.entity.BookingSettings;

public interface BookingSettingsRepository extends JpaRepository<BookingSettings, UUID> {
}
