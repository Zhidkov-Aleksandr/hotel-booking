package com.example.hotel_booking.booking_service.Repository;

import com.example.hotel_booking.booking_service.Entity.Booking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {
    List<Booking> findByUserId(Long userId);
    Optional<Booking> findByRequestId(String requestId);

    @Query("SELECT b FROM Booking b WHERE b.roomId = :roomId " +
            "AND b.status = 'CONFIRMED' " +
            "AND ((b.startDate <= :endDate AND b.endDate >= :startDate))")
    List<Booking> findConflictingBookings(
            @Param("roomId") Long roomId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );
}
