package com.example.hotel_booking.hotel_service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RoomDTO {
    private Long id;
    private Long hotelId;
    private String number;
    private Boolean available;
    private Integer timesBooked;
}