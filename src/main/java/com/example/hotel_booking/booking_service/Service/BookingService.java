package com.example.hotel_booking.booking_service.Service;

import com.example.hotel_booking.booking_service.client.HotelServiceClient;
import com.example.hotel_booking.booking_service.DTO.BookingDTO;
import com.example.hotel_booking.booking_service.DTO.ConfirmAvailabilityRequest;
import com.example.hotel_booking.booking_service.DTO.CreateBookingRequest;
import com.example.hotel_booking.booking_service.DTO.RoomDTO;
import com.example.hotel_booking.booking_service.Entity.Booking;
import com.example.hotel_booking.booking_service.Entity.BookingStatus;
import com.example.hotel_booking.booking_service.Entity.User;
import com.example.hotel_booking.booking_service.Repository.BookingRepository;
import com.example.hotel_booking.booking_service.Repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
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

        // Получаем candidate rooms
        List<RoomDTO> candidates = request.isAutoSelect()
                ? hotelServiceClient.getRecommendedRooms()
                : List.of(hotelServiceClient.getRoom(request.getRoomId()));
        if (candidates.isEmpty()) {
            throw new ResponseStatusException(NOT_FOUND, "No available rooms");
        }
        Long roomId = candidates.get(0).getId();

        // Проверка конфликтов локально
        List<Long> booked = bookingRepository.findBookedRoomIds(request.getStartDate(), request.getEndDate());
        if (booked.contains(roomId)) {
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
            bookingRepository.save(booking);
        } else {
            booking.setStatus(BookingStatus.CANCELLED);
            bookingRepository.save(booking);
            hotelServiceClient.releaseRoom(roomId, requestId);
            throw new ResponseStatusException(CONFLICT, "Room not available");
        }
        return toDTO(booking);
    }

    @Transactional(readOnly = true)
    public Page<BookingDTO> getAllBookings(Pageable pageable) {
        Page<Booking> page = bookingRepository.findAll(pageable);
        return page.map(this::mapToDTO);
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
                .map(r -> r.name().equals("ADMIN"))
                .orElse(false);
        if (!isAdmin && !booking.getUser().getUsername().equals(username)) {
            throw new ResponseStatusException(FORBIDDEN, "Access denied");
        }
        if (booking.getStatus() == BookingStatus.CONFIRMED) {
            booking.setStatus(BookingStatus.CANCELLED);
            bookingRepository.save(booking);
            hotelServiceClient.releaseRoom(booking.getRoomId(), booking.getRequestId());
        } else {
            booking.setStatus(BookingStatus.CANCELLED);
            bookingRepository.save(booking);
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
