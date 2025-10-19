package com.example.hotel_booking.hotel_service.mapper;


import com.example.hotel_booking.hotel_service.dto.RoomDTO;
import com.example.hotel_booking.hotel_service.entity.Room;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface RoomMapper {

    // MapStruct автоматически сопоставит поля id, number, available, timesBooked и hotelId
    RoomDTO toDTO(Room room);

    // При создании Entity из DTO установите поле hotelId, остальные поля совпадают
    Room toEntity(RoomDTO dto);
}
