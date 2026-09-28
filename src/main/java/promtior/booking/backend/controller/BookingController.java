package promtior.booking.backend.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
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
import promtior.booking.backend.dto.booking.BookingRequest;
import promtior.booking.backend.dto.booking.BookingResponse;
import promtior.booking.backend.dto.booking.UnifyBookingRequest;
import promtior.booking.backend.entity.User;
import promtior.booking.backend.service.BookingService;

@RestController
@RequestMapping("/bookings")
@RequiredArgsConstructor
public class BookingController {

   private final BookingService bookingService;

   @GetMapping
   public List<BookingResponse> getAll() {
      return bookingService.findAll();
   }

   @GetMapping("/{id}")
   public BookingResponse getById(@PathVariable UUID id) {
      return bookingService.findById(id);
   }

   @PostMapping
   public BookingResponse create(@Valid @RequestBody BookingRequest request, @AuthenticationPrincipal User currentUser) {
      return bookingService.create(request, currentUser.getId());
   }

   @PutMapping("/{id}")
   public BookingResponse update(@PathVariable UUID id, @Valid @RequestBody BookingRequest request, @AuthenticationPrincipal User currentUser) {
      return bookingService.update(id, request, currentUser.getId());
   }


   @DeleteMapping("/{id}")
   public void cancel(@PathVariable UUID id, @AuthenticationPrincipal User currentUser) {
      bookingService.cancel(id, currentUser.getId());
   }

   @PutMapping("/{id}/unify")
   public BookingResponse unify(@PathVariable UUID id, @Valid @RequestBody UnifyBookingRequest request, @AuthenticationPrincipal User currentUser) {
      return bookingService.unify(id, request.otherBookingId(), currentUser.getId());
   }
}
