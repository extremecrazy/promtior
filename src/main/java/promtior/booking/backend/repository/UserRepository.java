package promtior.booking.backend.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import promtior.booking.backend.entity.User;

public interface UserRepository extends JpaRepository<User, UUID> {

   Optional<User> findByUsername(String username);
}
