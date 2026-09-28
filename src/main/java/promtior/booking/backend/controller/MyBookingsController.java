package promtior.booking.backend.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import lombok.RequiredArgsConstructor;
import promtior.booking.backend.entity.User;
import promtior.booking.backend.service.BookingService;

@Controller
@RequiredArgsConstructor
public class MyBookingsController {

   private final BookingService bookingService;

   @GetMapping("/my-bookings")
   public String myBookings(@AuthenticationPrincipal User currentUser, Model model) {
      model.addAttribute("username", currentUser.getName());
      model.addAttribute("isAdmin", currentUser.isAdmin());
      model.addAttribute("bookings", bookingService.findAllByUser(currentUser.getId()));
      return "my-bookings";
   }
}
