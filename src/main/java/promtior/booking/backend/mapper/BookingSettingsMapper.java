package promtior.booking.backend.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import promtior.booking.backend.dto.settings.BookingSettingsRequest;
import promtior.booking.backend.dto.settings.BookingSettingsResponse;
import promtior.booking.backend.entity.BookingSettings;

@Mapper(componentModel = "spring")
public interface BookingSettingsMapper {

   BookingSettingsResponse toResponse(BookingSettings settings);

   @Mapping(target = "id", ignore = true)
   void updateEntityFromRequest(BookingSettingsRequest request, @MappingTarget BookingSettings settings);
}
