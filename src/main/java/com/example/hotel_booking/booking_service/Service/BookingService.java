package com.example.hotel_booking.booking_service.Service;

import com.example.hotel_booking.booking_service.Entity.Booking;
import com.example.hotel_booking.booking_service.Entity.BookingStatus;
import com.example.hotel_booking.booking_service.Entity.User;
import com.example.hotel_booking.booking_service.Repository.BookingRepository;
import com.example.hotel_booking.booking_service.Repository.UserRepository;
import com.example.hotel_booking.hotel_service.DTO.ConfirmAvailabilityRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
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
        String requestId = UUID.randomUUID().toString();

        // Проверка идемпотентности
        Optional<Booking> existing = bookingRepository.findByRequestId(requestId);
        if (existing.isPresent()) {
            log.info("Booking already exists for requestId: {}", requestId);
            return mapToDTO(existing.get());
        }

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Long roomId;
        if (request.isAutoSelect()) {
            // Автоподбор комнаты
            List<RoomDTO> recommendedRooms = hotelServiceClient.getRecommendedRooms(getCurrentToken());
            if (recommendedRooms.isEmpty()) {
                throw new RuntimeException("No available rooms");
            }
            roomId = recommendedRooms.get(0).getId();
        } else {
            roomId = request.getRoomId();
        }

        // Проверка конфликтов
        List<Booking> conflicts = bookingRepository.findConflictingBookings(
                roomId, request.getStartDate(), request.getEndDate());

        if (!conflicts.isEmpty()) {
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
        log.info("Booking created with status PENDING: {}", booking.getId());

        // Шаг 2: Подтверждение доступности у Hotel Service
        ConfirmAvailabilityRequest confirmRequest = new ConfirmAvailabilityRequest(
                requestId, request.getStartDate(), request.getEndDate());

        boolean confirmed = hotelServiceClient.confirmRoomAvailability(roomId, confirmRequest);

        if (confirmed) {
            booking.setStatus(BookingStatus.CONFIRMED);
            bookingRepository.save(booking);
            log.info("Booking confirmed: {}", booking.getId());
        } else {
            // Компенсация: отмена бронирования
            booking.setStatus(BookingStatus.CANCELLED);
            bookingRepository.save(booking);
            hotelServiceClient.releaseRoom(roomId, requestId);
            log.warn("Booking cancelled due to unavailable room: {}", booking.getId());
            throw new RuntimeException("Room is not available");
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

    public void cancelBooking(Long bookingId, String username) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new RuntimeException("Booking not found"));

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (!booking.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("Access denied");
        }

        if (booking.getStatus() == BookingStatus.CONFIRMED) {
            booking.setStatus(BookingStatus.CANCELLED);
            bookingRepository.save(booking);
            hotelServiceClient.releaseRoom(booking.getRoomId(), booking.getRequestId());
            log.info("Booking cancelled: {}", bookingId);
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

    private String getCurrentToken() {
        // Получение токена из SecurityContext
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth instanceof JwtAuthenticationToken) {
            return ((JwtAuthenticationToken) auth).getToken().getTokenValue();
        }
        return null;
    }
}
