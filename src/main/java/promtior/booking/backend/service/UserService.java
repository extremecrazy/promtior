package promtior.booking.backend.service;

import java.util.List;
import java.util.UUID;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import promtior.booking.backend.dto.user.UserRequest;
import promtior.booking.backend.dto.user.UserResponse;
import promtior.booking.backend.entity.User;
import promtior.booking.backend.exception.ResourceNotFoundException;
import promtior.booking.backend.mapper.UserMapper;
import promtior.booking.backend.repository.UserRepository;

@Service
@RequiredArgsConstructor
public class UserService {

   private final UserRepository userRepository;
   private final UserMapper userMapper;
   private final PasswordEncoder passwordEncoder;

   public List<UserResponse> findAll() {
      return userRepository.findAll().stream().map(userMapper::toResponse).toList();
   }

   public UserResponse findById(UUID id) {
      return userMapper.toResponse(getUserOrThrow(id));
   }

   @Transactional(rollbackFor = Exception.class)
   public UserResponse create(UserRequest request) {
      User user = userMapper.toEntity(request);
      user.setPassword(passwordEncoder.encode(request.password()));
      return userMapper.toResponse(userRepository.save(user));
   }

   @Transactional(rollbackFor = Exception.class)
   public UserResponse update(UUID id, UserRequest request) {
      User user = getUserOrThrow(id);
      userMapper.updateEntityFromRequest(request, user);
      user.setPassword(passwordEncoder.encode(request.password()));
      return userMapper.toResponse(userRepository.save(user));
   }

   @Transactional(rollbackFor = Exception.class)
   public void delete(UUID id) {
      userRepository.delete(getUserOrThrow(id));
   }

   private User getUserOrThrow(UUID id) {
      return userRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("User not found: " + id));
   }
}
