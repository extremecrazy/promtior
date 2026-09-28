package promtior.booking.backend.shared;

import java.time.LocalDateTime;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import lombok.Getter;
import lombok.Setter;


@Getter
@Setter
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class Auditable {

   @CreatedDate
   @Column(name = "created_date", nullable = false, updatable = false)
   private LocalDateTime createdDate;

   @LastModifiedDate
   @Column(name = "last_modified_date", nullable = false)
   private LocalDateTime lastModifiedDate;
}
