package promtior.booking.backend.controller;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import promtior.booking.backend.dto.security.AuthResponse;
import promtior.booking.backend.dto.security.LoginRequest;
import promtior.booking.backend.service.AuthService;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

   private final AuthService authService;

   @PostMapping("/login")
   public AuthResponse login(@Valid @RequestBody LoginRequest request) {
      return authService.login(request);
   }
}
