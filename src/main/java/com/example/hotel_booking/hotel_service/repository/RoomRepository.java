package com.example.hotel_booking.hotel_service.repository;

import com.example.hotel_booking.hotel_service.entity.Room;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RoomRepository extends JpaRepository<Room, Long> {
    List<Room> findByAvailableTrue();

    @Query("SELECT r FROM Room r WHERE r.available = true ORDER BY r.timesBooked ASC, r.id ASC")
    List<Room> findAvailableRoomsOrderedByTimesBooked();
}