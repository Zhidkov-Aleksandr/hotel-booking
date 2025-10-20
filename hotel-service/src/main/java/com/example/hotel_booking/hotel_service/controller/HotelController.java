package com.example.hotel_booking.hotel_service.controller;

import com.example.hotel_booking.hotel_service.dto.HotelStatsDTO;
import com.example.hotel_booking.hotel_service.dto.OccupancyDTO;
import com.example.hotel_booking.hotel_service.service.HotelAnalyticsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;


@Tag(name = "Hotel Analytics", description = "Analytics endpoints for hotel occupancy")
@RestController
@RequestMapping("/api/hotels")
public class HotelController {

    private final HotelAnalyticsService analyticsService;

    public HotelController(HotelAnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }

    @GetMapping("/{id}/stats")
    @Operation(summary = "Hotel stats", description = "Get per-room booking counts for given hotel")
    public ResponseEntity<HotelStatsDTO> getHotelStats(
            @PathVariable Long id,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ResponseEntity.ok(analyticsService.getHotelStats(id, from, to));
    }

    @GetMapping("/stats/occupancy")
    @Operation(summary = "Global occupancy", description = "Get overall occupancy percentage")
    public ResponseEntity<OccupancyDTO> getGlobalOccupancy(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ResponseEntity.ok(analyticsService.getGlobalOccupancy(from, to));
    }
}
