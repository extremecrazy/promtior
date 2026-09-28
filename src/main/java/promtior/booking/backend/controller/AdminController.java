package promtior.booking.backend.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import promtior.booking.backend.dto.settings.BookingSettingsRequest;
import promtior.booking.backend.entity.User;
import promtior.booking.backend.exception.BusinessException;
import promtior.booking.backend.service.BookingSettingsService;

/** Pantalla de administración (solo {@code ROLE_ADMIN}, ver SecurityConfig) para setear la configuración de reservas. */
@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminController {

   private final BookingSettingsService bookingSettingsService;

   @GetMapping("/settings")
   public String settings(@AuthenticationPrincipal User currentUser, Model model) {
      model.addAttribute("username", currentUser.getName());
      model.addAttribute("isAdmin", true);
      model.addAttribute("settings", bookingSettingsService.getSettings());
      return "admin-settings";
   }

   @PostMapping("/settings")
   public String updateSettings(@AuthenticationPrincipal User currentUser,
                                 @Valid @ModelAttribute("request") BookingSettingsRequest request,
                                 BindingResult bindingResult,
                                 Model model) {
      model.addAttribute("username", currentUser.getName());
      model.addAttribute("isAdmin", true);

      if (!bindingResult.hasErrors()) {
         try {
            model.addAttribute("settings", bookingSettingsService.updateSettings(request));
            model.addAttribute("success", true);
            return "admin-settings";
         } catch (BusinessException e) {
            model.addAttribute("error", e.getMessage());
         }
      } else {
         model.addAttribute("error", "slotMinutes y maxSlots son obligatorios y tienen que ser mayores a 0");
      }

      model.addAttribute("settings", bookingSettingsService.getSettings());
      return "admin-settings";
   }
}
