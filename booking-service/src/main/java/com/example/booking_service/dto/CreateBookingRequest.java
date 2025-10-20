package com.example.booking_service.dto;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Future;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateBookingRequest {
    private Long roomId;

    @NotNull @Future
    private LocalDate startDate;

    @NotNull @Future
    private LocalDate endDate;

    private boolean autoSelect;
}
