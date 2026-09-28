package promtior.booking.backend.service;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import promtior.booking.backend.dto.security.AuthResponse;
import promtior.booking.backend.dto.security.LoginRequest;
import promtior.booking.backend.entity.User;
import promtior.booking.backend.security.JwtUtil;

@Service
@RequiredArgsConstructor
public class AuthService {

   private final AuthenticationManager authenticationManager;
   private final JwtUtil jwtUtil;

   public AuthResponse login(LoginRequest request) {
      Authentication authentication = authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(request.username(), request.password()));
      User user = (User) authentication.getPrincipal();
      return new AuthResponse(jwtUtil.generateToken(user), user.getUsername());
   }
}
