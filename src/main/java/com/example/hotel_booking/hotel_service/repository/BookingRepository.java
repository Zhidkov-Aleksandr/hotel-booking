package com.example.hotel_booking.hotel_service.repository;

import com.example.hotel_booking.hotel_service.Entity.Booking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {

    @Query("SELECT b.room.id FROM Booking b WHERE b.status = 'CONFIRMED' " +
            "AND ((b.startDate <= :endDate AND b.endDate >= :startDate))")
    List<Long> findBookedRoomIds(@Param("startDate") LocalDate startDate,
                                 @Param("endDate") LocalDate endDate);

    @Query("SELECT b.room.id, COUNT(b.id) FROM Booking b " +
            "WHERE b.room.hotel.id = :hotelId " +
            "AND b.startDate >= :from AND b.endDate <= :to " +
            "GROUP BY b.room.id")
    List<Object[]> countByHotelRooms(@Param("hotelId") Long hotelId,
                                     @Param("from") LocalDate from,
                                     @Param("to") LocalDate to);

    @Query("SELECT COUNT(b.id) FROM Booking b " +
            "WHERE b.status = 'CONFIRMED' AND b.startDate >= :from AND b.endDate <= :to")
    long countConfirmedBetween(@Param("from") LocalDate from, @Param("to") LocalDate to);
}
