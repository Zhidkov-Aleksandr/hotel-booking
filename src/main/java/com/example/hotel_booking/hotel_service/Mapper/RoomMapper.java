package com.example.hotel_booking.hotel_service.Mapper;

import com.example.hotel_booking.hotel_service.DTO.RoomDTO;
import com.example.hotel_booking.hotel_service.entity.Room;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface RoomMapper {
    @Mapping(source = "hotel.id", target = "hotelId")
    RoomDTO toDTO(Room room);

    @Mapping(target = "hotel", ignore = true)
    Room toEntity(RoomDTO dto);
}
