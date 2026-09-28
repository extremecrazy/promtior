package promtior.booking.backend.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import promtior.booking.backend.dto.user.UserRequest;
import promtior.booking.backend.dto.user.UserResponse;
import promtior.booking.backend.entity.User;


@Mapper(componentModel = "spring")
public interface UserMapper {

   UserResponse toResponse(User user);

   @Mapping(target = "id", ignore = true)
   @Mapping(target = "bookings", ignore = true)
   @Mapping(target = "password", ignore = true)
   User toEntity(UserRequest request);

   @Mapping(target = "id", ignore = true)
   @Mapping(target = "bookings", ignore = true)
   @Mapping(target = "password", ignore = true)
   void updateEntityFromRequest(UserRequest request, @MappingTarget User user);
}
