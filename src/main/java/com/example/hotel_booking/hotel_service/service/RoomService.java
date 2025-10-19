package com.example.hotel_booking.hotel_service.service;

import com.example.hotel_booking.hotel_service.dto.ConfirmAvailabilityRequest;
import com.example.hotel_booking.hotel_service.dto.RoomDTO;
import com.example.hotel_booking.hotel_service.mapper.RoomMapper;
import com.example.hotel_booking.hotel_service.entity.Hotel;
import com.example.hotel_booking.hotel_service.entity.Room;
import com.example.hotel_booking.hotel_service.repository.HotelRepository;
import com.example.hotel_booking.hotel_service.repository.RoomRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
@Transactional
public class RoomService {
    private final RoomRepository roomRepository;
    private final HotelRepository hotelRepository;
    private final RoomMapper roomMapper;

    public RoomService(RoomRepository roomRepository,
                       HotelRepository hotelRepository,
                       RoomMapper roomMapper) {
        this.roomRepository = roomRepository;
        this.hotelRepository = hotelRepository;
        this.roomMapper = roomMapper;
    }

    public RoomDTO createRoom(RoomDTO roomDTO) {
        Hotel hotel = hotelRepository.findById(roomDTO.getHotelId())
                .orElseThrow(() -> new RuntimeException("Hotel not found"));

        Room room = roomMapper.toEntity(roomDTO);
        room.setHotel(hotel);
        room.setTimesBooked(0);

        Room saved = roomRepository.save(room);
        log.info("Room created: {}", saved.getId());
        return roomMapper.toDTO(saved);
    }

    @Transactional(readOnly = true)
    public List<RoomDTO> getAvailableRooms() {
        return roomRepository.findByAvailableTrue().stream()
                .filter(this::isNotTempBlocked)
                .map(roomMapper::toDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<RoomDTO> getRecommendedRooms() {
        return roomRepository.findAvailableRoomsOrderedByTimesBooked().stream()
                .filter(this::isNotTempBlocked)
                .map(roomMapper::toDTO)
                .collect(Collectors.toList());
    }

    public boolean confirmAvailability(Long roomId, ConfirmAvailabilityRequest request) {
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new RuntimeException("Room not found"));

        // Проверка идемпотентности
        if (request.getRequestId().equals(room.getTempBlockRequestId())) {
            log.info("Request {} already processed for room {}", request.getRequestId(), roomId);
            return true;
        }

        // Проверка доступности
        if (!room.getAvailable() || isNotTempBlocked(room)) {
            log.warn("Room {} is not available", roomId);
            return false;
        }

        // Временная блокировка на 5 минут
        room.setTempBlockedUntil(LocalDateTime.now().plusMinutes(5));
        room.setTempBlockRequestId(request.getRequestId());
        room.setTimesBooked(room.getTimesBooked() + 1);

        roomRepository.save(room);
        log.info("Room {} temporarily blocked for request {}", roomId, request.getRequestId());
        return true;
    }

    public void releaseRoom(Long roomId, String requestId) {
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new RuntimeException("Room not found"));

        // Проверка идемпотентности
        if (!requestId.equals(room.getTempBlockRequestId())) {
            log.info("Request {} does not match current block, skipping release", requestId);
            return;
        }

        room.setTempBlockedUntil(null);
        room.setTempBlockRequestId(null);
        room.setTimesBooked(Math.max(0, room.getTimesBooked() - 1));

        roomRepository.save(room);
        log.info("Room {} released for request {}", roomId, requestId);
    }

    private boolean isNotTempBlocked(Room room) {
        if (room.getTempBlockedUntil() == null) {
            return true;
        }
        return LocalDateTime.now().isAfter(room.getTempBlockedUntil());
    }
}
