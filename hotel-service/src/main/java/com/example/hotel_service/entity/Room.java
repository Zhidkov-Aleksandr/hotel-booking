package com.example.hotel_service.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


import java.time.LocalDateTime;

@Entity
@Table(name = "rooms")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Room {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "hotel_id", nullable = false)
    private Hotel hotel;

    @Column(nullable = false)
    private String number;

    @Column(nullable = false)
    private Boolean available = true;

    @Column(name = "times_booked", nullable = false)
    private Integer timesBooked = 0;

    // Поле для временной блокировки
    @Column(name = "temp_blocked_until")
    private LocalDateTime tempBlockedUntil;

    @Column(name = "temp_block_request_id")
    private String tempBlockRequestId;
}
