package promtior.booking.backend.service;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import promtior.booking.backend.dto.booking.BookingRequest;
import promtior.booking.backend.dto.booking.BookingResponse;
import promtior.booking.backend.dto.settings.BookingSettingsResponse;
import promtior.booking.backend.entity.Booking;
import promtior.booking.backend.entity.Room;
import promtior.booking.backend.entity.User;
import promtior.booking.backend.enums.BookingStatus;
import promtior.booking.backend.exception.BusinessException;
import promtior.booking.backend.exception.ResourceNotFoundException;
import promtior.booking.backend.mapper.BookingMapper;
import promtior.booking.backend.repository.BookingRepository;
import promtior.booking.backend.repository.RoomRepository;
import promtior.booking.backend.repository.UserRepository;

@Service
@RequiredArgsConstructor
public class BookingService {

   private static final int UNIFY_GAP_MINUTES = 0;

   private final BookingRepository bookingRepository;
   private final RoomRepository roomRepository;
   private final UserRepository userRepository;
   private final BookingMapper bookingMapper;
   private final BookingSettingsService bookingSettingsService;
   private final Clock clock;

   public List<BookingResponse> findAll() {
      return bookingRepository.findAll().stream().map(bookingMapper::toResponse).toList();
   }

   public BookingResponse findById(UUID id) {
      return bookingMapper.toResponse(getBookingOrThrow(id));
   }

   public List<BookingResponse> findAllByUser(UUID userId) {
      return bookingRepository.findByUser_IdOrderByStartTimeAsc(userId).stream().map(bookingMapper::toResponse).toList();
   }


   public List<BookingResponse> findWeeklySchedule(LocalDate weekStart, String username) {
      LocalDate monday = weekStart.with(DayOfWeek.MONDAY);
      LocalDateTime rangeStart = monday.atStartOfDay();
      LocalDateTime rangeEnd = monday.plusDays(7).atStartOfDay();
      String usernamePattern = (username == null || username.isBlank())
            ? null
            : "%" + username.trim().toLowerCase() + "%";
      return bookingRepository.findForWeeklySchedule(rangeStart, rangeEnd, usernamePattern).stream()
            .map(bookingMapper::toResponse).toList();
   }


   public List<LocalDateTime> findAvailableSlots(UUID roomId, LocalDate day) {
      getRoomOrThrow(roomId);
      int slotMinutes = bookingSettingsService.getSettings().slotMinutes();
      LocalDateTime dayStart = day.atStartOfDay();
      LocalDateTime dayEnd = day.plusDays(1).atStartOfDay();
      List<Booking> dayBookings = bookingRepository.findActiveByRoomAndRange(roomId, dayStart, dayEnd);

      List<LocalDateTime> available = new ArrayList<>();
      for (LocalDateTime cursor = dayStart; cursor.isBefore(dayEnd); cursor = cursor.plusMinutes(slotMinutes)) {
         final LocalDateTime slotStart = cursor;
         final LocalDateTime slotEnd = cursor.plusMinutes(slotMinutes);
         boolean overlaps = dayBookings.stream()
               .anyMatch(b -> b.getStartTime().isBefore(slotEnd) && b.getEndTime().isAfter(slotStart));
         if (!overlaps) {
            available.add(slotStart);
         }
      }
      return available;
   }

   /**
    * Reserva entre 1 y {@code maxSlots} slots de {@code slotMinutes} minutos
    * (ver {@link BookingSettingsService}). No unifica con reservas vecinas:
    * eso es {@link #unify}.
    */
   @Transactional(rollbackFor = Exception.class)
   public BookingResponse create(@Valid BookingRequest request, UUID userId) {
      validateSlotCount(request.startTime(), request.endTime());
      Room room = getRoomOrThrow(request.roomId());
      User user = getUserOrThrow(userId);
      validateRoomActive(room);
      validateAttendeeCount(request.attendeeCount(), room);
      validateNoOverlap(room.getId(), request.startTime(), request.endTime(), null);
      validateCurrentDate(request.startTime());
      Booking booking = bookingMapper.toEntity(request);
      booking.setRoom(room);
      booking.setUser(user);
      booking.setStatus(BookingStatus.ACTIVE);
      return bookingMapper.toResponse(bookingRepository.save(booking));
   }

   /**
    * Reposiciona/redimensiona una reserva existente (posiblemente ya
    * unificada), con la misma regla de slots que {@link #create}. Solo el
    * usuario dueño de la reserva puede modificarla ({@link #validateOwner}) —
    * el dueño no cambia con {@code update}, a diferencia de antes.
    */
   @Transactional(rollbackFor = Exception.class)
   public BookingResponse update(UUID id, BookingRequest request, UUID userId) {
      validateSlotCount(request.startTime(), request.endTime());
      Booking booking = getBookingOrThrow(id);
      validateOwner(booking, userId);
      Room room = getRoomOrThrow(request.roomId());
      validateRoomActive(room);
      validateAttendeeCount(request.attendeeCount(), room);
      validateNoOverlap(room.getId(), request.startTime(), request.endTime(), booking.getBookingId());
      validateCurrentDate(request.startTime());

      bookingMapper.updateEntityFromRequest(request, booking);
      booking.setRoom(room);
      return bookingMapper.toResponse(bookingRepository.save(booking));
   }

   /**
    * Unifica dos reservas ACTIVAS del mismo usuario, en la misma sala, en una
    * sola: exige que sean exactamente adyacentes, es decir que el
    * {@code startTime} de la que empieza después coincida con el
    * {@code endTime} de la que termina antes (ej: 10:30–11:00 y
    * 11:00–11:30). Extiende el {@code endTime} de la reserva anterior hasta
    * el {@code endTime} de la siguiente y **cancela** esta última (no se
    * borra de la base — ver {@link #cancel}).
    */
   @Transactional(rollbackFor = Exception.class)
   public BookingResponse unify(UUID firstBookingId, UUID secondBookingId, UUID userId) {
      if (firstBookingId.equals(secondBookingId)) {
         throw new BusinessException("No se puede unificar una reserva consigo misma");
      }

      Booking a = getBookingOrThrow(firstBookingId);
      Booking b = getBookingOrThrow(secondBookingId);
      Booking earlier = a.getStartTime().isBefore(b.getStartTime()) ? a : b;
      Booking later = earlier == a ? b : a;

      validateOwnedAndActive(earlier, userId);
      validateOwnedAndActive(later, userId);
      if (!earlier.getRoom().getId().equals(later.getRoom().getId())) {
         throw new BusinessException("Las reservas a unificar deben ser de la misma sala");
      }

      LocalDateTime expectedNextStart = earlier.getEndTime().plusMinutes(UNIFY_GAP_MINUTES);
      if (!later.getStartTime().equals(expectedNextStart)) {
         throw new BusinessException("Solo se pueden unificar reservas exactamente adyacentes "
               + "(el fin de una debe coincidir con el inicio de la otra)");
      }

      checkTotalDuration(earlier.getStartTime(), later.getEndTime(), bookingSettingsService.getSettings());

      earlier.setEndTime(later.getEndTime());
      later.setStatus(BookingStatus.CANCELLED);
      bookingRepository.save(later);
      return bookingMapper.toResponse(bookingRepository.save(earlier));
   }

   /**
    * Cancela una reserva: nunca se borra de la base, pasa de {@code ACTIVE} a
    * {@code CANCELLED} (queda como historial). Solo el dueño puede cancelarla
    * ({@link #validateOwner}); cancelar una reserva ya cancelada es un error.
    */
   @Transactional(rollbackFor = Exception.class)
   public void cancel(UUID id, UUID userId) {
      Booking booking = getBookingOrThrow(id);
      validateOwner(booking, userId);
      if (booking.getStatus() != BookingStatus.ACTIVE) {
         throw new BusinessException("La reserva ya está cancelada");
      }
      booking.setStatus(BookingStatus.CANCELLED);
      bookingRepository.save(booking);
   }

   private void validateOwnedAndActive(Booking booking, UUID userId) {
      validateOwner(booking, userId);
      if (booking.getStatus() != BookingStatus.ACTIVE) {
         throw new BusinessException("Solo se pueden unificar reservas activas");
      }
   }

   /** Valida que {@code userId} sea el dueño de la reserva — nunca otro usuario puede modificarla/cancelarla. */
   private void validateOwner(Booking booking, UUID userId) {
      if (!booking.getUser().getId().equals(userId)) {
         throw new BusinessException("Solo se pueden modificar o cancelar reservas propias");
      }
   }

   private void checkTotalDuration(LocalDateTime start, LocalDateTime end, BookingSettingsResponse settings) {
      long totalMinutes = Duration.between(start, end).toMinutes();
      long maxMinutes = (long) settings.maxSlots() * settings.slotMinutes();
      if (totalMinutes > maxMinutes) {
         throw new BusinessException("La reserva combinada no puede superar los " + maxMinutes + " minutos");
      }
   }

   /**
    * Duración múltiplo de {@code slotMinutes}, ocupando entre 1 y
    * {@code maxSlots} slots, y alineada a la grilla de horarios posibles
    * ({@code startTime} tiene que caer justo en un múltiplo de
    * {@code slotMinutes} contado desde las 00:00 del día, ver
    * {@link #validateSlotAlignment}) — ver {@link BookingSettingsService}.
    */
   private void validateSlotCount(LocalDateTime start, LocalDateTime end) {
      if (start == null || end == null || !end.isAfter(start)) {
         throw new BusinessException("endTime debe ser posterior a startTime");
      }
      BookingSettingsResponse settings = bookingSettingsService.getSettings();
      validateSlotAlignment(start, settings);
      long durationMinutes = Duration.between(start, end).toMinutes();
      if (durationMinutes % settings.slotMinutes() != 0) {
         throw new BusinessException("La duración de la reserva debe ser múltiplo de " + settings.slotMinutes() + " minutos");
      }
      long slotCount = durationMinutes / settings.slotMinutes();
      if (slotCount > settings.maxSlots()) {
         throw new BusinessException("La reserva no puede superar los " + settings.maxSlots() + " slots ("
               + (settings.maxSlots() * settings.slotMinutes()) + " minutos)");
      }
   }

   /**
    * Los horarios posibles de reserva son una grilla fija que arranca a las
    * 00:00 y avanza de {@code slotMinutes} en {@code slotMinutes} (ej. con
    * slots de 30 minutos: 00:00, 00:30, 01:00, ...); {@code startTime} tiene
    * que caer exactamente en uno de esos horarios, nunca en un minuto
    * intermedio (ej. 00:01). Si la duración ya es múltiplo de
    * {@code slotMinutes} (ver {@link #validateSlotCount}), alinear el inicio
    * alcanza para que el fin también caiga en la grilla.
    */
   private void validateSlotAlignment(LocalDateTime start, BookingSettingsResponse settings) {
      long minutesSinceMidnight = start.getHour() * 60L + start.getMinute();
      if (start.getSecond() != 0 || start.getNano() != 0 || minutesSinceMidnight % settings.slotMinutes() != 0) {
         throw new BusinessException("El horario de inicio debe caer en la grilla de slots de "
               + settings.slotMinutes() + " minutos contados desde las 00:00 (múltiplos exactos de "
               + settings.slotMinutes() + " minutos desde la medianoche)");
      }
   }

   /** Una sala deshabilitada por un admin no admite reservas nuevas (ver entity.Room.active). */
   private void validateRoomActive(Room room) {
      if (!Boolean.TRUE.equals(room.getActive())) {
         throw new BusinessException("La sala \"" + room.getName() + "\" está deshabilitada y no admite reservas");
      }
   }

   /** La cantidad de asistentes no puede superar la capacidad máxima de la sala. */
   private void validateAttendeeCount(Integer attendeeCount, Room room) {
      if (attendeeCount > room.getMaxCapacity()) {
         throw new BusinessException("La cantidad de asistentes (" + attendeeCount
               + ") supera la capacidad máxima de la sala (" + room.getMaxCapacity() + ")");
      }
   }

   /**
    * Valida que no exista otra reserva ACTIVA de la misma sala cuyo horario se
    * solape con el solicitado (ver {@link BookingRepository#existsOverlapping}).
    */
   private void validateNoOverlap(UUID roomId, LocalDateTime start, LocalDateTime end, UUID excludeBookingId) {
      if (bookingRepository.existsOverlapping(roomId, start, end, excludeBookingId)) {
         throw new BusinessException("La sala ya tiene una reserva que se solapa con el horario solicitado");
      }
   }

   private Booking getBookingOrThrow(UUID id) {
      return bookingRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Booking not found: " + id));
   }

   private Room getRoomOrThrow(UUID id) {
      return roomRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Room not found: " + id));
   }

   private User getUserOrThrow(UUID id) {
      return userRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("User not found: " + id));
   }

   private void validateCurrentDate(LocalDateTime dateTime) {
      if (dateTime == null) {
         throw new BusinessException("La fecha no debe ser vacia");
      }

      if (dateTime.isBefore(LocalDateTime.now(clock))) {
         throw new BusinessException("Le fecha y hora deben ser posteriores a la actual");
      }
   }
}
