package com.example.hotel_service.service;

import com.example.hotel_service.dto.HotelStatsDTO;
import com.example.hotel_service.dto.OccupancyDTO;
import com.example.hotel_service.repository.BookingRepository;
import com.example.hotel_service.repository.RoomRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class HotelAnalyticsService {

    private final BookingRepository bookingRepo;
    private final RoomRepository roomRepo;

    public HotelAnalyticsService(BookingRepository bookingRepo, RoomRepository roomRepo) {
        this.bookingRepo = bookingRepo;
        this.roomRepo = roomRepo;
    }

    public HotelStatsDTO getHotelStats(Long hotelId, LocalDate from, LocalDate to) {
        Map<Long, Long> counts = bookingRepo.countByHotelRooms(hotelId, from, to)
                .stream().collect(Collectors.toMap(m -> (Long)m[0], m -> (Long)m[1]));
        return new HotelStatsDTO(hotelId, counts);
    }

    public OccupancyDTO getGlobalOccupancy(LocalDate from, LocalDate to) {
        long totalRooms = roomRepo.count();
        long booked = bookingRepo.countConfirmedBetween(from, to);
        double percent = totalRooms == 0 ? 0.0 : (booked * 100.0 / totalRooms);
        return new OccupancyDTO(from, to, percent);
    }
}
