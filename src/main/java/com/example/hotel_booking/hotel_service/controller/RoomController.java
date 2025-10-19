package com.example.hotel_booking.hotel_service.controller;

import com.example.hotel_booking.hotel_service.dto.ConfirmAvailabilityRequest;
import com.example.hotel_booking.hotel_service.dto.RoomDTO;
import com.example.hotel_booking.hotel_service.service.RoomService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/rooms")
@Tag(name = "Room Management")
public class RoomController {
    private final RoomService roomService;

    public RoomController(RoomService roomService) {
        this.roomService = roomService;
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<RoomDTO> createRoom(@RequestBody @Valid RoomDTO roomDTO) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(roomService.createRoom(roomDTO));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<List<RoomDTO>> getAvailableRooms() {
        return ResponseEntity.ok(roomService.getAvailableRooms());
    }

    @GetMapping("/recommend")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<List<RoomDTO>> getRecommendedRooms() {
        return ResponseEntity.ok(roomService.getRecommendedRooms());
    }

    @PostMapping("/{id}/confirm-availability")
    public ResponseEntity<Boolean> confirmAvailability(
            @PathVariable Long id,
            @RequestBody ConfirmAvailabilityRequest request) {
        boolean confirmed = roomService.confirmAvailability(id, request);
        return ResponseEntity.ok(confirmed);
    }

    @PostMapping("/{id}/release")
    public ResponseEntity<Void> releaseRoom(
            @PathVariable Long id,
            @RequestParam String requestId) {
        roomService.releaseRoom(id, requestId);
        return ResponseEntity.ok().build();
    }
}
