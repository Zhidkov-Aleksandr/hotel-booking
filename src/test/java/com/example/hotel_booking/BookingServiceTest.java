package com.example.hotel.booking.bookingservice;

import com.example.hotel.booking.bookingservice.dto.CreateBookingRequest;
import com.example.hotel.booking.bookingservice.entity.BookingStatus;
import com.example.hotel.booking.bookingservice.service.BookingService;
import com.example.hotel.booking.bookingservice.client.HotelServiceClient;
import com.example.hotel.booking.bookingservice.dto.BookingDTO;
import com.example.hotel.booking.bookingservice.entity.User;
import com.example.hotel.booking.bookingservice.repository.BookingRepository;
import com.example.hotel.booking.bookingservice.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;

@SpringBootTest(classes = BookingServiceApplication.class)
class BookingServiceTest {

    @Autowired
    private BookingService bookingService;

    @MockBean
    private HotelServiceClient hotelServiceClient;

    @MockBean
    private UserRepository userRepository;

    @MockBean
    private BookingRepository bookingRepository;

    @Test
    void testCreateBooking_Success() {
        CreateBookingRequest req = new CreateBookingRequest();
        req.setRoomId(1L);
        req.setStartDate(LocalDate.now().plusDays(1));
        req.setEndDate(LocalDate.now().plusDays(2));
        req.setAutoSelect(false);

        User user = new User();
        user.setId(1L);
        user.setUsername("testuser");
        Mockito.when(userRepository.findByUsername("testuser")).thenReturn(java.util.Optional.of(user));
        Mockito.when(hotelServiceClient.getRoom(1L)).thenReturn(new com.example.hotel.booking.bookingservice.dto.RoomDTO(1L,1L,"101",true,0));
        Mockito.when(bookingRepository.findBookedRoomIds(any(),any())).thenReturn(java.util.Collections.emptyList());
        Mockito.when(hotelServiceClient.confirmRoomAvailability(any(),any())).thenReturn(true);

        BookingDTO dto = bookingService.createBooking(req, "testuser");
        assertNotNull(dto);
        assertEquals(BookingStatus.CONFIRMED.name(), dto.getStatus());
    }

    @Test
    void testCreateBooking_CompensationOnFailure() {
        CreateBookingRequest req = new CreateBookingRequest();
        req.setRoomId(1L);
        req.setStartDate(LocalDate.now().plusDays(1));
        req.setEndDate(LocalDate.now().plusDays(2));
        req.setAutoSelect(false);

        User user = new User();
        user.setId(1L);
        user.setUsername("testuser");
        Mockito.when(userRepository.findByUsername("testuser")).thenReturn(java.util.Optional.of(user));
        Mockito.when(hotelServiceClient.getRoom(1L)).thenReturn(new com.example.hotel.booking.bookingservice.dto.RoomDTO(1L,1L,"101",true,0));
        Mockito.when(bookingRepository.findBookedRoomIds(any(),any())).thenReturn(java.util.Collections.emptyList());
        Mockito.when(hotelServiceClient.confirmRoomAvailability(any(),any())).thenReturn(false);

        assertThrows(RuntimeException.class, () -> bookingService.createBooking(req, "testuser"));
        Mockito.verify(hotelServiceClient).releaseRoom(1L, dtoRequestIdCaptor.capture());
    }
}
