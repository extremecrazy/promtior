package promtior.booking.backend.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import promtior.booking.backend.dto.room.RoomRequest;
import promtior.booking.backend.dto.room.RoomResponse;
import promtior.booking.backend.entity.Room;

@Mapper(componentModel = "spring")
public interface RoomMapper {

   RoomResponse toResponse(Room room);


   @Mapping(target = "id", ignore = true)
   @Mapping(target = "bookings", ignore = true)
   @Mapping(target = "active", ignore = true)
   Room toEntity(RoomRequest request);

   @Mapping(target = "id", ignore = true)
   @Mapping(target = "bookings", ignore = true)
   @Mapping(target = "active", ignore = true)
   void updateEntityFromRequest(RoomRequest request, @MappingTarget Room room);
}
