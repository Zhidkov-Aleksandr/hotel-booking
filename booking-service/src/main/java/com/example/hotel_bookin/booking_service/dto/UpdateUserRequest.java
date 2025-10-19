package com.example.hotel_bookin.booking_service.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.Size;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateUserRequest {
    @Size(min = 3, max = 50, message = "Username must be between 3 and 50 characters")
    private String username;

    @Size(min = 6, max = 100, message = "Password must be at least 6 characters")
    private String password;

    private String role;
}
