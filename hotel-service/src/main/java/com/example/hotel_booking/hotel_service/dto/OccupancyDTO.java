package com.example.hotel_booking.hotel_service.dto;


import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDate;

@Data
@AllArgsConstructor
public class OccupancyDTO {
    private LocalDate from;
    private LocalDate to;
    private double occupancyPercent;
}
