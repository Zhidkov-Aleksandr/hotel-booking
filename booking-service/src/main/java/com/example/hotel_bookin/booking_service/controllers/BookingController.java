package com.example.hotel_bookin.booking_service.controllers;

import com.example.hotel_booking.booking_service.dto.BookingDTO;
import com.example.hotel_booking.booking_service.dto.CreateBookingRequest;
import com.example.hotel_booking.booking_service.service.BookingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;


@Tag(name = "Booking Management", description = "Hotel room booking operations with pagination")
@RestController
@RequestMapping("/booking")
@SecurityRequirement(name = "Bearer Authentication")
public class BookingController {

    private final BookingService bookingService;

    public BookingController(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('USER','ADMIN')")
    @Operation(summary = "Create booking",
            description = "Create a new booking. Set autoSelect=true for automatic room selection")
    public ResponseEntity<BookingDTO> createBooking(
            @RequestBody @Valid CreateBookingRequest request,
            Authentication authentication) {
        String username = authentication.getName();
        BookingDTO booking = bookingService.createBooking(request, username);
        return ResponseEntity.status(HttpStatus.CREATED).body(booking);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('USER','ADMIN')")
    @Operation(summary = "Get user bookings",
            description = "Get paginated bookings for current user")
    public ResponseEntity<Page<BookingDTO>> getUserBookings(
            Authentication authentication,
            @PageableDefault(size = 10) Pageable pageable) {
        String username = authentication.getName();
        Page<BookingDTO> page = bookingService.getUserBookings(username, pageable);
        return ResponseEntity.ok(page);
    }

    @GetMapping("/all")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get all bookings (Admin only)", description = "Admin can view all bookings with pagination")
    public ResponseEntity<Page<BookingDTO>> getAllBookings(
            @PageableDefault(size = 20) Pageable pageable) {
        Page<BookingDTO> page = bookingService.getAllBookings(pageable);
        return ResponseEntity.ok(page);
    }


    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('USER','ADMIN')")
    @Operation(summary = "Cancel booking",
            description = "Cancel an existing booking")
    public ResponseEntity<Void> cancelBooking(
            @PathVariable Long id,
            Authentication authentication) {
        String username = authentication.getName();
        bookingService.cancelBooking(id, username);
        return ResponseEntity.noContent().build();
    }
}
