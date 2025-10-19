package com.example.hotel_booking;

import com.example.hotel_booking.booking_service.dto.BookingDTO;
import com.example.hotel_booking.booking_service.dto.CreateBookingRequest;
import com.example.hotel_booking.booking_service.entity.BookingStatus;
import com.example.hotel_booking.booking_service.service.BookingService;
import com.example.hotel_booking.booking_service.service.HotelServiceClient;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringBootTest(properties = "spring.profiles.active=test")
@Transactional
class BookingServiceTest {

    @Autowired
    private BookingService bookingService;

    @MockBean
    private HotelServiceClient hotelServiceClient;

    @Test
    void testCreateBooking_Success() {
        // Arrange
        CreateBookingRequest request = new CreateBookingRequest();
        request.setRoomId(1L);
        request.setStartDate(LocalDate.now());
        request.setEndDate(LocalDate.now().plusDays(2));
        request.setAutoSelect(false);

        when(hotelServiceClient.confirmRoomAvailability(any(), any()))
                .thenReturn(true);

        // Act
        BookingDTO result = bookingService.createBooking(request, "testuser");

        // Assert
        assertNotNull(result, "Booking result should not be null");
        assertEquals(BookingStatus.CONFIRMED.name(), result.getStatus(), "Booking should be confirmed");
    }

    @Test
    void testCreateBooking_CompensationOnFailure() {
        // Arrange
        CreateBookingRequest request = new CreateBookingRequest();
        request.setRoomId(1L);
        request.setStartDate(LocalDate.now());
        request.setEndDate(LocalDate.now().plusDays(2));
        request.setAutoSelect(false);

        when(hotelServiceClient.confirmRoomAvailability(any(), any()))
                .thenReturn(false);

        // Act & Assert
        assertThrows(ResponseStatusException.class, () ->
                        bookingService.createBooking(request, "testuser"),
                "Should throw ResponseStatusException when room not available");

        verify(hotelServiceClient).releaseRoom(eq(1L), any());
    }
}
