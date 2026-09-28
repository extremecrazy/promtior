package promtior.booking.backend.security;

import java.io.IOException;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import lombok.RequiredArgsConstructor;


@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

   private static final String BEARER_PREFIX = "Bearer ";

   private final JwtUtil jwtUtil;

   private final UserDetailsService userDetailsService;

   @Override
   protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws ServletException, IOException {
      String authHeader = request.getHeader("Authorization");
      if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)) {
         chain.doFilter(request, response);
         return;
      }

      String token = authHeader.substring(BEARER_PREFIX.length());
      if (jwtUtil.isTokenValid(token)) {
         String username = jwtUtil.extractUsername(token);
         UserDetails userDetails = userDetailsService.loadUserByUsername(username);
         UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
         auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
         SecurityContextHolder.getContext().setAuthentication(auth);
      }
      chain.doFilter(request, response);
   }
}
