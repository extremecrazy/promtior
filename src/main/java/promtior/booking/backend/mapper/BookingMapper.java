package promtior.booking.backend.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import promtior.booking.backend.dto.booking.BookingRequest;
import promtior.booking.backend.dto.booking.BookingResponse;
import promtior.booking.backend.entity.Booking;


@Mapper(componentModel = "spring")
public interface BookingMapper {

   @Mapping(target = "roomId", source = "room.id")
   @Mapping(target = "roomName", source = "room.name")
   @Mapping(target = "userId", source = "user.id")
   @Mapping(target = "userName", source = "user.name")
   BookingResponse toResponse(Booking booking);

   @Mapping(target = "bookingId", ignore = true)
   @Mapping(target = "room", ignore = true)
   @Mapping(target = "user", ignore = true)
   @Mapping(target = "status", ignore = true)
   Booking toEntity(BookingRequest request);

   @Mapping(target = "bookingId", ignore = true)
   @Mapping(target = "room", ignore = true)
   @Mapping(target = "user", ignore = true)
   @Mapping(target = "status", ignore = true)
   void updateEntityFromRequest(BookingRequest request, @MappingTarget Booking booking);
}
