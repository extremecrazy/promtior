package promtior.booking.backend.tool;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import promtior.booking.backend.dto.booking.BookingRequest;
import promtior.booking.backend.dto.booking.BookingResponse;
import promtior.booking.backend.dto.room.RoomResponse;
import promtior.booking.backend.dto.settings.BookingSettingsResponse;
import promtior.booking.backend.entity.User;
import promtior.booking.backend.exception.BusinessException;
import promtior.booking.backend.exception.ResourceNotFoundException;
import promtior.booking.backend.service.BookingService;
import promtior.booking.backend.service.BookingSettingsService;
import promtior.booking.backend.service.RoomService;


@Service
@RequiredArgsConstructor
public class ChatToolsService {


   public static final String BOOKING_CREATED_KEY = "bookingCreatedInThisRequest";

   private static final DateTimeFormatter ISO_MINUTES = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm");

   private final BookingService bookingService;
   private final RoomService roomService;
   private final BookingSettingsService bookingSettingsService;
   private final Clock clock;

   @Tool(description = "Devuelve la fecha y hora actual, en formato ISO (yyyy-MM-ddTHH:mm), seguida del día de la semana entre paréntesis. Usar para responder qué hora o qué día es, y para resolver referencias relativas como 'mañana', 'el viernes' o 'en una hora'.")
   public String getCurrentDateTime() {
      LocalDateTime now = LocalDateTime.now(clock);
      return now.format(ISO_MINUTES) + " (" + now.getDayOfWeek().getDisplayName(TextStyle.FULL, Locale.of("es")) + ")";
   }

   @Tool(description = "Lista las salas habilitadas para reservar, con su nombre y capacidad máxima. Las salas deshabilitadas por un admin no aparecen acá.")
   public List<RoomResponse> listRooms() {
      return roomService.findAllActive();
   }


   public record BookingDurationRules(int gridMinutes, int maxDurationMinutes) {
   }

   @Tool(description = "Devuelve las reglas de duración vigentes para reservas, en minutos: gridMinutes (cada cuántos minutos, desde las 00:00, puede empezar una reserva) y maxDurationMinutes (duración total máxima permitida de una reserva). Usar antes de crear o explicar una reserva para saber estos valores reales en vez de asumirlos — pueden cambiar. Nunca le menciones al usuario que existe un conteo de \"slots\": comunicale únicamente estos minutos (o convertidos a horas si es más claro).")
   public BookingDurationRules getBookingDurationRules() {
      BookingSettingsResponse settings = bookingSettingsService.getSettings();
      return new BookingDurationRules(settings.slotMinutes(), settings.slotMinutes() * settings.maxSlots());
   }

   @Tool(description = "Lista las reservas activas del usuario actualmente logueado, incluido su bookingId. El bookingId es solo para uso interno (pasarlo a cancelBooking/rescheduleBooking/unifyBookings) — nunca se lo muestres al usuario, referite a cada reserva por sala/horario/motivo.")
   public List<BookingResponse> listMyBookings() {
      return bookingService.findAllByUser(currentUser().getId());
   }

   @Tool(description = "Lista las salas disponibles (sin ninguna reserva ACTIVA que se solape) para un horario puntual. Usar cuando el usuario pregunta qué sala está libre en un día/horario dado, antes de sugerirle una sala para crear la reserva.")
   public List<RoomResponse> listAvailableRooms(
         @ToolParam(description = "Inicio del horario a consultar, formato yyyy-MM-ddTHH:mm") LocalDateTime startTime,
         @ToolParam(description = "Fin del horario a consultar, formato yyyy-MM-ddTHH:mm") LocalDateTime endTime) {
      return roomService.findAvailableRooms(startTime, endTime);
   }

   @Tool(description = "Devuelve los horarios de inicio disponibles (que no se solapan con ninguna reserva ACTIVA existente) de una sala puntual para un día determinado, respetando la grilla vigente (ver getBookingDurationRules). Usar cuando el usuario pregunta qué horarios tiene libre una sala específica en un día dado.")
   public List<String> listAvailableSlotsForRoom(
         @ToolParam(description = "Nombre de la sala, tal cual figura en listRooms") String roomName,
         @ToolParam(description = "Día a consultar, formato yyyy-MM-dd") LocalDate day) {
      UUID roomId = findRoomIdByName(roomName);
      return bookingService.findAvailableSlots(roomId, day).stream()
            .map(dt -> dt.format(ISO_MINUTES))
            .toList();
   }

   @Tool(description = "Crea una reserva de sala para el usuario logueado, con una duración que respeta las reglas de getBookingDurationRules (múltiplo de gridMinutes, sin superar maxDurationMinutes en total). Los horarios posibles son una grilla fija que arranca a las 00:00 y avanza de gridMinutes en gridMinutes (ej. con una grilla de 30 minutos: 00:00, 00:30, 01:00, ...); startTime tiene que caer justo en uno de esos horarios, nunca en un minuto intermedio (ej. 00:01 se rechaza). Puede empezar justo donde termina otra reserva ACTIVA de la misma sala (aunque sea propia) sin dejar ningún minuto libre (ej: si hay una reserva hasta las 11:00, la próxima puede empezar a las 11:00 en punto); solo se rechaza si de verdad se solapan los horarios, si el inicio no cae en la grilla, o si se supera la duración máxima. Solo se puede crear UNA reserva por mensaje del usuario: una segunda llamada en el mismo mensaje se rechaza.")
   public BookingResponse createBooking(
         @ToolParam(description = "Nombre de la sala, tal cual figura en listRooms") String roomName,
         @ToolParam(description = "Nombre/motivo de la reserva, tal cual lo indicó el usuario. Nunca inventar un valor por defecto: si el usuario no lo dio, hay que preguntárselo antes de llamar a esta herramienta.") String bookingName,
         @ToolParam(description = "Inicio, formato yyyy-MM-ddTHH:mm. Tiene que caer en la grilla horaria vigente (múltiplo exacto de gridMinutes contado desde las 00:00), no en cualquier minuto.") LocalDateTime startTime,
         @ToolParam(description = "Fin, formato yyyy-MM-ddTHH:mm. La duración (endTime - startTime) tiene que ser un múltiplo de gridMinutes, sin superar maxDurationMinutes (ver getBookingDurationRules).") LocalDateTime endTime,
         @ToolParam(description = "Cantidad de personas que asisten a la reserva. No puede superar la capacidad máxima de la sala (ver listRooms). Nunca inventar un valor: si el usuario no lo dio, preguntárselo antes de llamar a esta herramienta.") Integer attendeeCount,
         ToolContext toolContext) {
      AtomicBoolean bookingCreated = toolContext == null ? null
            : (AtomicBoolean) toolContext.getContext().get(BOOKING_CREATED_KEY);
      if (bookingCreated != null && bookingCreated.get()) {
         throw new BusinessException("Solo se puede crear una reserva por pedido: ya se creó una en este mensaje. "
               + "No intentes crear más; avisale al usuario que puede pedir la siguiente en otro mensaje.");
      }
      UUID roomId = findRoomIdByName(roomName);
      UUID userId = currentUser().getId();
      BookingRequest request = new BookingRequest(bookingName, roomId, startTime, endTime, attendeeCount);
      BookingResponse response = bookingService.create(request, userId);
      if (bookingCreated != null) {
         bookingCreated.set(true);
      }
      return response;
   }

   @Tool(description = "Cancela (borra) una reserva existente del usuario logueado, a partir de su bookingId. Usar listMyBookings para obtener el id. Solo funciona sobre reservas propias del usuario logueado — nunca sobre las de otro usuario, aunque se le pase un bookingId ajeno.")
   public String cancelBooking(@ToolParam(description = "Id de la reserva a cancelar") UUID bookingId) {
      bookingService.cancel(bookingId, currentUser().getId());
      return "Reserva cancelada";
   }

   @Tool(description = "Reprograma una reserva existente del usuario logueado a un nuevo horario (ISO yyyy-MM-ddTHH:mm), manteniendo la misma sala y nombre. El nuevo inicio también tiene que caer en la grilla horaria vigente (ver createBooking). Solo funciona sobre reservas propias del usuario logueado — nunca sobre las de otro usuario, aunque se le pase un bookingId ajeno.")
   public BookingResponse rescheduleBooking(
         @ToolParam(description = "Id de la reserva a reprogramar") UUID bookingId,
         @ToolParam(description = "Nuevo inicio, formato yyyy-MM-ddTHH:mm") LocalDateTime newStartTime,
         @ToolParam(description = "Nuevo fin, formato yyyy-MM-ddTHH:mm") LocalDateTime newEndTime) {
      BookingResponse current = bookingService.findById(bookingId);
      BookingRequest request = new BookingRequest(current.name(), current.roomId(), newStartTime, newEndTime, current.attendeeCount());
      return bookingService.update(bookingId, request, currentUser().getId());
   }

   @Tool(description = "Unifica en una sola reserva dos reservas ACTIVAS propias de la misma sala, cuando son exactamente adyacentes: el fin de una coincide con el inicio de la otra (ej: una termina a las 11:00 y la otra empieza a las 11:00). Solo llamar si el usuario lo pidió explícitamente, nunca por iniciativa propia. Usar listMyBookings para obtener los bookingId.")
   public BookingResponse unifyBookings(
         @ToolParam(description = "Id de una de las dos reservas a unificar") UUID firstBookingId,
         @ToolParam(description = "Id de la otra reserva a unificar") UUID secondBookingId) {
      return bookingService.unify(firstBookingId, secondBookingId, currentUser().getId());
   }


   private UUID findRoomIdByName(String roomName) {
      return roomService.findAllActive().stream()
            .filter(r -> r.name().equalsIgnoreCase(roomName))
            .map(RoomResponse::id)
            .findFirst()
            .orElseThrow(() -> new ResourceNotFoundException("Room not found or disabled: " + roomName));
   }

   private User currentUser() {
      return (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
   }
}
