package promtior.booking.backend.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import promtior.booking.backend.dto.room.RoomActiveRequest;
import promtior.booking.backend.dto.room.RoomRequest;
import promtior.booking.backend.dto.room.RoomResponse;
import promtior.booking.backend.service.RoomService;

@RestController
@RequestMapping("/rooms")
@RequiredArgsConstructor
public class RoomController {

   private final RoomService roomService;

   @GetMapping
   public List<RoomResponse> getAll() {
      return roomService.findAll();
   }

   @GetMapping("/{id}")
   public RoomResponse getById(@PathVariable UUID id) {
      return roomService.findById(id);
   }

   @PostMapping
   public RoomResponse create(@Valid @RequestBody RoomRequest request) {
      return roomService.create(request);
   }

   @PutMapping("/{id}")
   public RoomResponse update(@PathVariable UUID id, @Valid @RequestBody RoomRequest request) {
      return roomService.update(id, request);
   }

   @DeleteMapping("/{id}")
   public void delete(@PathVariable UUID id) {
      roomService.delete(id);
   }

   /** Habilita/deshabilita la sala (una deshabilitada no admite reservas nuevas, ver BookingService). */
   @PutMapping("/{id}/active")
   public RoomResponse setActive(@PathVariable UUID id, @Valid @RequestBody RoomActiveRequest request) {
      return roomService.setActive(id, request.active());
   }
}
