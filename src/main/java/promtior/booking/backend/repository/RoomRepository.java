package promtior.booking.backend.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import promtior.booking.backend.entity.Room;

public interface RoomRepository extends JpaRepository<Room, UUID> {


   List<Room> findByActiveTrueOrderByNameAsc();


   Page<Room> findByNameContainingIgnoreCase(String name, Pageable pageable);


   boolean existsByNameIgnoreCase(String name);


   boolean existsByNameIgnoreCaseAndIdNot(String name, UUID id);


   @Query("""
         SELECT r FROM Room r
         WHERE r.active = true
           AND r.id NOT IN (
               SELECT b.room.id FROM Booking b
               WHERE b.status = promtior.booking.backend.enums.BookingStatus.ACTIVE
                 AND b.startTime < :endTime AND b.endTime > :startTime
            )
         ORDER BY r.name ASC
         """)
   List<Room> findAvailable(@Param("startTime") LocalDateTime startTime,
                             @Param("endTime") LocalDateTime endTime);
}
