package promtior.booking.backend.service;

import jakarta.validation.Valid;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import promtior.booking.backend.dto.settings.BookingSettingsRequest;
import promtior.booking.backend.dto.settings.BookingSettingsResponse;
import promtior.booking.backend.entity.BookingSettings;
import promtior.booking.backend.exception.BusinessException;
import promtior.booking.backend.exception.ResourceNotFoundException;
import promtior.booking.backend.mapper.BookingSettingsMapper;
import promtior.booking.backend.repository.BookingSettingsRepository;


@Service
@RequiredArgsConstructor
public class BookingSettingsService {

   private final BookingSettingsRepository bookingSettingsRepository;
   private final BookingSettingsMapper bookingSettingsMapper;

   public BookingSettingsResponse getSettings() {
      return bookingSettingsMapper.toResponse(getSettingsEntity());
   }

   @Transactional(rollbackFor = Exception.class)
   public BookingSettingsResponse updateSettings(@Valid BookingSettingsRequest request) {
      validateSlotMinutes(request.slotMinutes());
      BookingSettings settings = getSettingsEntity();
      bookingSettingsMapper.updateEntityFromRequest(request, settings);
      return bookingSettingsMapper.toResponse(bookingSettingsRepository.save(settings));
   }

   private BookingSettings getSettingsEntity() {
      return bookingSettingsRepository.findAll().stream().findFirst()
            .orElseThrow(() -> new ResourceNotFoundException("Booking settings not configured"));
   }


   private void validateSlotMinutes(Integer slotMinutes) {
      if (slotMinutes % 60 != 0 && 60 % slotMinutes != 0) {
         throw new BusinessException("slotMinutes debe ser múltiplo o divisor de 60");
      }
   }
}
