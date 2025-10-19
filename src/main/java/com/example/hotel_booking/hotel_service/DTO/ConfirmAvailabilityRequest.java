package com.example.hotel_booking.hotel_service.DTO;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ConfirmAvailabilityRequest {
    private String requestId;
    private LocalDate startDate;
    private LocalDate endDate;
}