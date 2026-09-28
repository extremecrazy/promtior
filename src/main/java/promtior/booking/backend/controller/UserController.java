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
import promtior.booking.backend.dto.user.UserRequest;
import promtior.booking.backend.dto.user.UserResponse;
import promtior.booking.backend.service.UserService;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

   private final UserService userService;

   @GetMapping
   public List<UserResponse> getAll() {
      return userService.findAll();
   }

   @GetMapping("/{id}")
   public UserResponse getById(@PathVariable UUID id) {
      return userService.findById(id);
   }

   @PostMapping
   public UserResponse create(@Valid @RequestBody UserRequest request) {
      return userService.create(request);
   }

   @PutMapping("/{id}")
   public UserResponse update(@PathVariable UUID id, @Valid @RequestBody UserRequest request) {
      return userService.update(id, request);
   }

   @DeleteMapping("/{id}")
   public void delete(@PathVariable UUID id) {
      userService.delete(id);
   }
}
