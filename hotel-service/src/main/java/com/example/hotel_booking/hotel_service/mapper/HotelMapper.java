package com.example.hotel_booking.hotel_service.mapper;
import com.example.hotel_booking.hotel_service.dto.HotelDTO;
import com.example.hotel_booking.hotel_service.entity.Hotel;
import org.mapstruct.Mapper;



@Mapper(componentModel = "spring")
public interface HotelMapper {
    HotelDTO toDTO(Hotel hotel);
    Hotel toEntity(HotelDTO dto);
}