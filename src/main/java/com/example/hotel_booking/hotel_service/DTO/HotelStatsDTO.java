package com.example.hotel_booking.hotel_service.DTO;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDate;
import java.util.Map;

@Data
@AllArgsConstructor
public class HotelStatsDTO {
    private Long hotelId;
    private Map<Long, Long> roomBookingCounts;
}

@Data
@AllArgsConstructor
public class OccupancyDTO {
    private LocalDate from;
    private LocalDate to;
    private double occupancyPercent;
}
