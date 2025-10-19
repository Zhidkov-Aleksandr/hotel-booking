package com.example.hotel_booking.hotel_service.Controller;

import com.example.hotel_booking.hotel_service.DTO.HotelDTO;
import com.example.hotel_booking.hotel_service.Service.HotelService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.List;

@RestController
@RequestMapping("/api/hotels")
@Tag(name = "Hotel Management")
public class HotelController {
    private final HotelService hotelService;

    public HotelController(HotelService hotelService) {
        this.hotelService = hotelService;
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<HotelDTO> createHotel(@RequestBody @Valid HotelDTO hotelDTO) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(hotelService.createHotel(hotelDTO));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<List<HotelDTO>> getAllHotels() {
        return ResponseEntity.ok(hotelService.getAllHotels());
    }
}
