package promtior.booking.backend.controller;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.TextStyle;
import java.util.List;
import java.util.Locale;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import lombok.RequiredArgsConstructor;
import promtior.booking.backend.dto.booking.BookingResponse;
import promtior.booking.backend.entity.User;
import promtior.booking.backend.service.BookingService;


@Controller
@RequiredArgsConstructor
public class AdminScheduleController {

   private final BookingService bookingService;
   private final Clock clock;

   private static final Locale ES = Locale.of("es");

   /** Agrupación día por día para la plantilla — no es un DTO de dominio. */
   public record DaySchedule(LocalDate date, String dayName, List<BookingResponse> bookings) {
   }

   @GetMapping("/admin/schedule")
   public String schedule(@AuthenticationPrincipal User currentUser,
                           @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate weekStart,
                           @RequestParam(required = false) String username,
                           Model model) {
      LocalDate referenceDate = weekStart != null ? weekStart : LocalDate.now(clock);
      LocalDate monday = referenceDate.with(DayOfWeek.MONDAY);
      LocalDate sunday = monday.plusDays(6);

      List<BookingResponse> bookings = bookingService.findWeeklySchedule(monday, username);
      List<DaySchedule> days = monday.datesUntil(monday.plusDays(7))
            .map(date -> new DaySchedule(date, capitalize(date.getDayOfWeek().getDisplayName(TextStyle.FULL, ES)),
                  bookings.stream().filter(b -> b.startTime().toLocalDate().equals(date)).toList()))
            .filter(day -> !day.bookings().isEmpty())
            .toList();

      model.addAttribute("username", currentUser.getName());
      model.addAttribute("isAdmin", true);
      model.addAttribute("days", days);
      model.addAttribute("weekStart", monday);
      model.addAttribute("weekEnd", sunday);
      model.addAttribute("previousWeek", monday.minusWeeks(1));
      model.addAttribute("nextWeek", monday.plusWeeks(1));
      model.addAttribute("today", LocalDate.now(clock).with(DayOfWeek.MONDAY));
      model.addAttribute("usernameFilter", username);
      return "admin-schedule";
   }

   private static String capitalize(String value) {
      return value.isEmpty() ? value : Character.toUpperCase(value.charAt(0)) + value.substring(1);
   }
}
