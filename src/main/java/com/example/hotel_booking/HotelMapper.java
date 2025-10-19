package com.example.hotel_booking;
import org.mapstruct.Mapper;



@Mapper(componentModel = "spring")
public interface HotelMapper {
    HotelDTO toDTO(Hotel hotel);
    Hotel toEntity(HotelDTO dto);
}