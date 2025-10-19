package com.example.hotel_booking.hotel_service.service;

import com.example.hotel_booking.hotel_service.dto.HotelDTO;
import com.example.hotel_booking.hotel_service.mapper.HotelMapper;
import com.example.hotel_booking.hotel_service.entity.Hotel;
import com.example.hotel_booking.hotel_service.repository.HotelRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
@Transactional
public class HotelService {
    private final HotelRepository hotelRepository;
    private final HotelMapper hotelMapper;

    public HotelService(HotelRepository hotelRepository, HotelMapper hotelMapper) {
        this.hotelRepository = hotelRepository;
        this.hotelMapper = hotelMapper;
    }

    public HotelDTO createHotel(HotelDTO hotelDTO) {
        Hotel hotel = hotelMapper.toEntity(hotelDTO);
        Hotel saved = hotelRepository.save(hotel);
        log.info("Hotel created: {}", saved.getId());
        return hotelMapper.toDTO(saved);
    }

    @Transactional(readOnly = true)
    public List<HotelDTO> getAllHotels() {
        return hotelRepository.findAll().stream()
                .map(hotelMapper::toDTO)
                .collect(Collectors.toList());
    }
}
