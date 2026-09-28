package promtior.booking.backend.controller;

import java.util.UUID;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import promtior.booking.backend.dto.room.RoomRequest;
import promtior.booking.backend.dto.room.RoomResponse;
import promtior.booking.backend.entity.User;
import promtior.booking.backend.exception.BusinessException;
import promtior.booking.backend.exception.ResourceNotFoundException;
import promtior.booking.backend.service.RoomService;

/**
 * Pantalla de administración (solo {@code ROLE_ADMIN}, ver SecurityConfig)
 * para editar nombre/capacidad de las salas y habilitarlas/deshabilitarlas.
 * Una sala deshabilitada no admite reservas nuevas (ver {@code BookingService}).
 *
 * <p>Los `POST` redirigen a `GET /admin/rooms` en vez de renderizar la vista
 * directamente (patrón Post/Redirect/Get, con {@code success}/{@code error}
 * como flash attributes) — si no, un reload del navegador reenvía el último
 * `POST`. Para el form de editar nombre/capacidad eso es inofensivo (reenvía
 * los mismos valores), pero para "toggle-active" es destructivo: como invierte
 * el estado actual en vez de fijar uno, reenviarlo por accidente deshace el
 * cambio que se acababa de guardar — daba la falsa impresión de que
 * deshabilitar "no guardaba" cuando en realidad el reload lo revertía.
 * {@code success}/{@code error} disparan un modal de resultado en
 * {@code templates/admin-rooms.html} (`#result-modal`) que requiere click en
 * "Aceptar" para cerrarse — no hay mensaje inline en la card ni auto-cierre.
 */
@Controller
@RequestMapping("/admin/rooms")
@RequiredArgsConstructor
public class AdminRoomController {

   private final RoomService roomService;

   private static final int PAGE_SIZE = 10;

   @GetMapping
   public String rooms(@AuthenticationPrincipal User currentUser,
                        @RequestParam(required = false) String name,
                        @RequestParam(defaultValue = "0") int page,
                        Model model) {
      Pageable pageable = PageRequest.of(page, PAGE_SIZE, Sort.by(Sort.Direction.ASC, "name"));
      var roomsPage = roomService.findAll(name, pageable);
      model.addAttribute("username", currentUser.getName());
      model.addAttribute("isAdmin", true);
      model.addAttribute("rooms", roomsPage.getContent());
      model.addAttribute("pageInfo", roomsPage);
      model.addAttribute("nameFilter", name);
      return "admin-rooms";
   }

   @PostMapping
   public String createRoom(@Valid @ModelAttribute("newRoom") RoomRequest request,
                             BindingResult bindingResult,
                             @RequestParam(defaultValue = "false") boolean active,
                             RedirectAttributes redirectAttributes) {
      if (!bindingResult.hasErrors()) {
         try {
            roomService.create(request, active);
            redirectAttributes.addFlashAttribute("success", true);
         } catch (BusinessException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
         }
      } else {
         redirectAttributes.addFlashAttribute("error", "Nombre y capacidad son obligatorios y la capacidad tiene que ser mayor a 0");
      }
      return "redirect:/admin/rooms";
   }

   @PostMapping("/{id}")
   public String updateRoom(@PathVariable UUID id,
                             @Valid @ModelAttribute("request") RoomRequest request,
                             BindingResult bindingResult,
                             RedirectAttributes redirectAttributes) {
      if (!bindingResult.hasErrors()) {
         try {
            roomService.update(id, request);
            redirectAttributes.addFlashAttribute("success", true);
         } catch (BusinessException | ResourceNotFoundException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
         }
      } else {
         redirectAttributes.addFlashAttribute("error", "Nombre y capacidad son obligatorios y la capacidad tiene que ser mayor a 0");
      }
      return "redirect:/admin/rooms";
   }

   @PostMapping("/{id}/toggle-active")
   public String toggleActive(@PathVariable UUID id, RedirectAttributes redirectAttributes) {
      try {
         RoomResponse room = roomService.findById(id);
         roomService.setActive(id, !Boolean.TRUE.equals(room.active()));
         redirectAttributes.addFlashAttribute("success", true);
      } catch (BusinessException | ResourceNotFoundException e) {
         redirectAttributes.addFlashAttribute("error", e.getMessage());
      }
      return "redirect:/admin/rooms";
   }
}
