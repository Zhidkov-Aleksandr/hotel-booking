package com.example.hotel_booking.hotel_service.Mapper;
import com.example.hotel_booking.hotel_service.DTO.HotelDTO;
import com.example.hotel_booking.hotel_service.entity.Hotel;
import org.mapstruct.Mapper;



@Mapper(componentModel = "spring")
public interface HotelMapper {
    HotelDTO toDTO(Hotel hotel);
    Hotel toEntity(HotelDTO dto);
}