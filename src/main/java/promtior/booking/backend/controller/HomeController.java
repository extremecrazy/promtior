package promtior.booking.backend.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import promtior.booking.backend.entity.User;

@Controller
public class HomeController {

   @GetMapping("/")
   public String root() {
      return "redirect:/home";
   }

   @GetMapping("/home")
   public String home(@AuthenticationPrincipal User currentUser, Model model) {
      model.addAttribute("username", currentUser.getName());
      model.addAttribute("isAdmin", currentUser.isAdmin());
      return "home";
   }
}
