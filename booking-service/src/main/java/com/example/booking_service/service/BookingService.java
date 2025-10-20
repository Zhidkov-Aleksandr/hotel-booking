package com.example.booking_service.service;

import com.example.booking_service.client.HotelServiceClient;
import com.example.booking_service.dto.BookingDTO;
import com.example.booking_service.dto.ConfirmAvailabilityRequest;
import com.example.booking_service.dto.CreateBookingRequest;
import com.example.booking_service.dto.RoomDTO;
import com.example.booking_service.entity.Booking;
import com.example.booking_service.entity.BookingStatus;
import com.example.booking_service.entity.User;
import com.example.booking_service.repository.BookingRepository;
import com.example.booking_service.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.springframework.http.HttpStatus.*;

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
        if (request.getEndDate().isBefore(request.getStartDate().plusDays(1))) {
            throw new ResponseStatusException(BAD_REQUEST, "End date must be after start date");
        }

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "User not found"));

        // Получаем кандидатов
        List<RoomDTO> candidates;
        if (request.isAutoSelect()) {
            candidates = hotelServiceClient.getRecommendedRooms();
        } else {
            RoomDTO room = hotelServiceClient.getRoom(request.getRoomId());
            candidates = (room == null)
                    ? Collections.emptyList()
                    : Collections.singletonList(room);
        }

        if (candidates == null || candidates.isEmpty()) {
            throw new ResponseStatusException(NOT_FOUND, "No available rooms");
        }

        Long roomId = candidates.get(0).getId();

        List<Long> booked = bookingRepository.findBookedRoomIds(request.getStartDate(), request.getEndDate());
        if (booked != null && booked.contains(roomId)) {
            throw new ResponseStatusException(CONFLICT, "Room is already booked");
        }

        String requestId = UUID.randomUUID().toString();
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

        boolean confirmed = hotelServiceClient.confirmRoomAvailability(
                roomId,
                new ConfirmAvailabilityRequest(requestId, request.getStartDate(), request.getEndDate())
        );

        if (confirmed) {
            booking.setStatus(BookingStatus.CONFIRMED);
        } else {
            booking.setStatus(BookingStatus.CANCELLED);
            try {
                hotelServiceClient.releaseRoom(roomId, requestId);
            } catch (Exception ex) {
                log.error("Error releasing room during compensation: {}", ex.getMessage());
            }
            throw new ResponseStatusException(CONFLICT, "Room not available");
        }

        bookingRepository.save(booking);
        return toDTO(booking);
    }

    @Transactional(readOnly = true)
    public Page<BookingDTO> getUserBookings(String username, Pageable pageable) {
        return bookingRepository.findByUserUsername(username, pageable)
                .map(this::toDTO);
    }

    @Transactional(readOnly = true)
    public Page<BookingDTO> getAllBookings(Pageable pageable) {
        return bookingRepository.findAll(pageable)
                .map(this::toDTO);
    }

    public void cancelBooking(Long id, String username) {
        Booking booking = bookingRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Booking not found"));

        boolean isAdmin = userRepository.findByUsername(username)
                .map(User::getRole)
                .map(Enum::name)
                .map("ADMIN"::equals)
                .orElse(false);

        if (!isAdmin && !booking.getUser().getUsername().equals(username)) {
            throw new ResponseStatusException(FORBIDDEN, "Access denied");
        }

        BookingStatus previousStatus = booking.getStatus();
        booking.setStatus(BookingStatus.CANCELLED);
        bookingRepository.save(booking);

        if (previousStatus == BookingStatus.CONFIRMED) {
            try {
                hotelServiceClient.releaseRoom(booking.getRoomId(), booking.getRequestId());
            } catch (Exception ex) {
                log.error("Error releasing room after cancellation: {}", ex.getMessage());
            }
        }
    }

    private BookingDTO toDTO(Booking b) {
        return BookingDTO.builder()
                .id(b.getId())
                .userId(b.getUser().getId())
                .roomId(b.getRoomId())
                .startDate(b.getStartDate())
                .endDate(b.getEndDate())
                .status(b.getStatus().name())
                .createdAt(b.getCreatedAt())
                .build();
    }
}
