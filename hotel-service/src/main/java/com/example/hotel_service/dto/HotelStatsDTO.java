package com.example.hotel_service.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.Map;

@Data
@AllArgsConstructor
public class HotelStatsDTO {
    private Long hotelId;
    private Map<Long, Long> roomBookingCounts;
}
