package com.example.hotel_booking.booking_service.Controllers;

import com.example.hotel_booking.booking_service.DTO.BookingDTO;
import com.example.hotel_booking.booking_service.DTO.CreateBookingRequest;
import com.example.hotel_booking.booking_service.Service.BookingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/booking")
@Tag(name = "Booking Management", description = "Hotel room booking operations")
@SecurityRequirement(name = "Bearer Authentication")
public class BookingController {

    private final BookingService bookingService;

    public BookingController(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    @Operation(summary = "Create booking",
            description = "Create a new booking. Set autoSelect=true for automatic room selection")
    public ResponseEntity<BookingDTO> createBooking(
            @RequestBody @Valid CreateBookingRequest request,
            Authentication authentication) {
        String username = authentication.getName();
        BookingDTO booking = bookingService.createBooking(request, username);
        return ResponseEntity.status(HttpStatus.CREATED).body(booking);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    @Operation(summary = "Get booking by ID", description = "Retrieve booking details by ID")
    public ResponseEntity<BookingDTO> getBookingById(
            @PathVariable Long id,
            Authentication authentication) {
        String username = authentication.getName();
        BookingDTO booking = bookingService.getBookingById(id, username);
        return ResponseEntity.ok(booking);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    @Operation(summary = "Get user bookings", description = "Get all bookings for current user")
    public ResponseEntity<List<BookingDTO>> getUserBookings(Authentication authentication) {
        String username = authentication.getName();
        List<BookingDTO> bookings = bookingService.getUserBookings(username);
        return ResponseEntity.ok(bookings);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    @Operation(summary = "Cancel booking", description = "Cancel an existing booking")
    public ResponseEntity<Void> cancelBooking(
            @PathVariable Long id,
            Authentication authentication) {
        String username = authentication.getName();
        bookingService.cancelBooking(id, username);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/all")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get all bookings (Admin only)",
            description = "Admin can view all bookings in the system")
    public ResponseEntity<List<BookingDTO>> getAllBookings() {
        List<BookingDTO> bookings = bookingService.getAllBookings();
        return ResponseEntity.ok(bookings);
    }
}
