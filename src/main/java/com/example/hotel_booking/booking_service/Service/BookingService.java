package com.example.hotel_booking.booking_service.Service;

import com.example.hotel_booking.booking_service.client.HotelServiceClient;
import com.example.hotel_booking.booking_service.DTO.*;
import com.example.hotel_booking.booking_service.Entity.*;
import com.example.hotel_booking.booking_service.Repository.;

import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Slf4j
@Transactional
public class BookingService {

    private final BookingRepository bookingRepository;
    private final UserRepository userRepository;
    private final HotelServiceClient hotelServiceClient;

    public BookingService(BookingRepository bookingRepository,
                          UserRepository userRepository,
                          HotelServiceClient hotelServiceClient) {
        this.bookingRepository = bookingRepository;
        this.userRepository = userRepository;
        this.hotelServiceClient = hotelServiceClient;
    }

    public BookingDTO createBooking(CreateBookingRequest request, String username) {
        // Валидация дат
        if (request.getEndDate().isBefore(request.getStartDate()) ||
                request.getEndDate().isEqual(request.getStartDate())) {
            throw new RuntimeException("End date must be after start date");
        }

        String requestId = UUID.randomUUID().toString();
        log.info("Creating booking with requestId: {}", requestId);

        // Проверка идемпотентности
        Optional<Booking> existing = bookingRepository.findByRequestId(requestId);
        if (existing.isPresent()) {
            log.info("Booking already exists for requestId: {}", requestId);
            return mapToDTO(existing.get());
        }

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Long roomId;

        // Автоподбор или выбор номера
        if (request.isAutoSelect()) {
            log.info("Auto-selecting room for user: {}", username);
            List<RoomDTO> recommendedRooms = hotelServiceClient.getRecommendedRooms();
            if (recommendedRooms.isEmpty()) {
                throw new RuntimeException("No available rooms");
            }
            roomId = recommendedRooms.get(0).getId();
            log.info("Selected room: {}", roomId);
        } else {
            if (request.getRoomId() == null) {
                throw new RuntimeException("Room ID is required when autoSelect is false");
            }
            roomId = request.getRoomId();
        }

        // Проверка конфликтов бронирования
        List<Booking> conflicts = bookingRepository.findConflictingBookings(
                roomId, request.getStartDate(), request.getEndDate());

        if (!conflicts.isEmpty()) {
            log.warn("Room {} is already booked for selected dates", roomId);
            throw new RuntimeException("Room is already booked for selected dates");
        }

        // Шаг 1: Создание бронирования в статусе PENDING
        Booking booking = Booking.builder()
                .user(user)
                .roomId(roomId)
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .status(BookingStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .requestId(requestId)
                .build();

        booking = bookingRepository.save(booking);
        log.info("Booking {} created with status PENDING for user: {}", booking.getId(), username);

        // Шаг 2: Подтверждение доступности у Hotel Service
        ConfirmAvailabilityRequest confirmRequest = new ConfirmAvailabilityRequest(
                requestId, request.getStartDate(), request.getEndDate());

        boolean confirmed = false;
        try {
            confirmed = hotelServiceClient.confirmRoomAvailability(roomId, confirmRequest);
        } catch (Exception e) {
            log.error("Error confirming room availability: {}", e.getMessage());
        }

        if (confirmed) {
            // Успешное подтверждение
            booking.setStatus(BookingStatus.CONFIRMED);
            bookingRepository.save(booking);
            log.info("Booking {} confirmed successfully", booking.getId());
        } else {
            // Компенсация: отмена бронирования
            booking.setStatus(BookingStatus.CANCELLED);
            bookingRepository.save(booking);

            // Попытка освободить комнату (компенсирующая транзакция)
            try {
                hotelServiceClient.releaseRoom(roomId, requestId);
                log.info("Compensation: Room {} released for requestId: {}", roomId, requestId);
            } catch (Exception e) {
                log.error("Error releasing room during compensation: {}", e.getMessage());
            }

            log.warn("Booking {} cancelled due to unavailable room", booking.getId());
            throw new RuntimeException("Room is not available for selected dates");
        }

        return mapToDTO(booking);
    }

    @Transactional(readOnly = true)
    public BookingDTO getBookingById(Long id, String username) {
        Booking booking = bookingRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Booking not found"));

        // Проверка прав доступа
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean isAdmin = auth.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_ADMIN"));

        if (!isAdmin && !booking.getUser().getUsername().equals(username)) {
            throw new RuntimeException("Access denied");
        }

        return mapToDTO(booking);
    }

    @Transactional(readOnly = true)
    public List<BookingDTO> getUserBookings(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        return bookingRepository.findByUserId(user.getId()).stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<BookingDTO> getAllBookings() {
        return bookingRepository.findAll().stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    public void cancelBooking(Long bookingId, String username) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new RuntimeException("Booking not found"));

        // Проверка прав доступа
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean isAdmin = auth.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_ADMIN"));

        if (!isAdmin && !booking.getUser().getUsername().equals(username)) {
            throw new RuntimeException("Access denied");
        }

        if (booking.getStatus() == BookingStatus.CANCELLED) {
            throw new RuntimeException("Booking is already cancelled");
        }

        if (booking.getStatus() == BookingStatus.CONFIRMED) {
            booking.setStatus(BookingStatus.CANCELLED);
            bookingRepository.save(booking);

            // Освобождение номера
            try {
                hotelServiceClient.releaseRoom(booking.getRoomId(), booking.getRequestId());
                log.info("Booking {} cancelled and room {} released", bookingId, booking.getRoomId());
            } catch (Exception e) {
                log.error("Error releasing room after cancellation: {}", e.getMessage());
            }
        } else {
            booking.setStatus(BookingStatus.CANCELLED);
            bookingRepository.save(booking);
            log.info("Booking {} cancelled", bookingId);
        }
    }

    private BookingDTO mapToDTO(Booking booking) {
        return BookingDTO.builder()
                .id(booking.getId())
                .userId(booking.getUser().getId())
                .roomId(booking.getRoomId())
                .startDate(booking.getStartDate())
                .endDate(booking.getEndDate())
                .status(booking.getStatus().name())
                .createdAt(booking.getCreatedAt())
                .build();
    }
}

