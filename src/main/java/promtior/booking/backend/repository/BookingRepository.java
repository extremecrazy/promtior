package promtior.booking.backend.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import promtior.booking.backend.entity.Booking;

public interface BookingRepository extends JpaRepository<Booking, UUID> {

   List<Booking> findByUser_IdOrderByStartTimeAsc(UUID userId);


   @Query("""
         SELECT b FROM Booking b
         WHERE b.startTime >= :rangeStart AND b.startTime < :rangeEnd
           AND (:usernamePattern IS NULL OR LOWER(b.user.username) LIKE :usernamePattern)
         ORDER BY b.startTime ASC
         """)
   List<Booking> findForWeeklySchedule(@Param("rangeStart") LocalDateTime rangeStart,
                                        @Param("rangeEnd") LocalDateTime rangeEnd,
                                        @Param("usernamePattern") String usernamePattern);


   @Query("""
         SELECT COUNT(b) > 0 FROM Booking b
         WHERE b.room.id = :roomId
           AND b.status = promtior.booking.backend.enums.BookingStatus.ACTIVE
           AND (:excludeBookingId IS NULL OR b.bookingId <> :excludeBookingId)
           AND b.startTime < :endTime
           AND b.endTime > :startTime
         """)
   boolean existsOverlapping(@Param("roomId") UUID roomId,
                              @Param("startTime") LocalDateTime startTime,
                              @Param("endTime") LocalDateTime endTime,
                              @Param("excludeBookingId") UUID excludeBookingId);


   @Query("""
         SELECT b FROM Booking b
         WHERE b.room.id = :roomId
           AND b.status = promtior.booking.backend.enums.BookingStatus.ACTIVE
           AND b.startTime < :rangeEnd AND b.endTime > :rangeStart
         ORDER BY b.startTime ASC
         """)
   List<Booking> findActiveByRoomAndRange(@Param("roomId") UUID roomId,
                                           @Param("rangeStart") LocalDateTime rangeStart,
                                           @Param("rangeEnd") LocalDateTime rangeEnd);


   @Query("""
         SELECT COUNT(b) > 0 FROM Booking b
         WHERE b.room.id = :roomId
           AND b.status = promtior.booking.backend.enums.BookingStatus.ACTIVE
           AND b.endTime > :now
         """)
   boolean existsActiveFutureByRoom(@Param("roomId") UUID roomId, @Param("now") LocalDateTime now);
}
