package promtior.booking.backend.service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import promtior.booking.backend.dto.room.RoomRequest;
import promtior.booking.backend.dto.room.RoomResponse;
import promtior.booking.backend.entity.Room;
import promtior.booking.backend.exception.BusinessException;
import promtior.booking.backend.exception.ResourceNotFoundException;
import promtior.booking.backend.mapper.RoomMapper;
import promtior.booking.backend.repository.BookingRepository;
import promtior.booking.backend.repository.RoomRepository;

@Service
@RequiredArgsConstructor
public class RoomService {

   private final RoomRepository roomRepository;
   private final BookingRepository bookingRepository;
   private final RoomMapper roomMapper;
   private final Clock clock;


   public List<RoomResponse> findAll() {
      return roomRepository.findAll(Sort.by(Sort.Direction.ASC, "name")).stream().map(roomMapper::toResponse).toList();
   }


   public Page<RoomResponse> findAll(String nameFilter, Pageable pageable) {
      Page<Room> rooms = (nameFilter == null || nameFilter.isBlank())
            ? roomRepository.findAll(pageable)
            : roomRepository.findByNameContainingIgnoreCase(nameFilter.trim(), pageable);
      return rooms.map(roomMapper::toResponse);
   }


   public List<RoomResponse> findAllActive() {
      return roomRepository.findByActiveTrueOrderByNameAsc().stream().map(roomMapper::toResponse).toList();
   }


   public List<RoomResponse> findAvailableRooms(LocalDateTime startTime, LocalDateTime endTime) {
      if (startTime == null || endTime == null || !endTime.isAfter(startTime)) {
         throw new BusinessException("endTime debe ser posterior a startTime");
      }
      return roomRepository.findAvailable(startTime, endTime).stream().map(roomMapper::toResponse).toList();
   }

   public RoomResponse findById(UUID id) {
      return roomMapper.toResponse(getRoomOrThrow(id));
   }


   @Transactional(rollbackFor = Exception.class)
   public RoomResponse create(RoomRequest request) {
      return create(request, true);
   }


   @Transactional(rollbackFor = Exception.class)
   public RoomResponse create(RoomRequest request, boolean active) {
      validateNameUnique(request.name(), null);
      Room room = roomMapper.toEntity(request);
      room.setActive(active);
      return roomMapper.toResponse(roomRepository.save(room));
   }


   @Transactional(rollbackFor = Exception.class)
   public RoomResponse update(UUID id, RoomRequest request) {
      validateNameUnique(request.name(), id);
      Room room = getRoomOrThrow(id);
      roomMapper.updateEntityFromRequest(request, room);
      return roomMapper.toResponse(roomRepository.save(room));
   }

   @Transactional(rollbackFor = Exception.class)
   public void delete(UUID id) {
      roomRepository.delete(getRoomOrThrow(id));
   }


   @Transactional(rollbackFor = Exception.class)
   public RoomResponse setActive(UUID id, boolean active) {
      Room room = getRoomOrThrow(id);
      if (!active && bookingRepository.existsActiveFutureByRoom(id, LocalDateTime.now(clock))) {
         throw new BusinessException("No se puede deshabilitar: la sala tiene reservas activas vigentes");
      }
      room.setActive(active);
      return roomMapper.toResponse(roomRepository.save(room));
   }


   private void validateNameUnique(String name, UUID excludeId) {
      boolean duplicate = excludeId == null
            ? roomRepository.existsByNameIgnoreCase(name)
            : roomRepository.existsByNameIgnoreCaseAndIdNot(name, excludeId);
      if (duplicate) {
         throw new BusinessException("Ya existe una sala con el nombre \"" + name + "\"");
      }
   }

   private Room getRoomOrThrow(UUID id) {
      return roomRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Room not found: " + id));
   }
}
