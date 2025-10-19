package com.example.hotel_bookin.booking_service.repository;

import com.example.hotel_booking.booking_service.entity.Booking;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {

    // Получение всех бронирований пользователя (без пагинации)
    List<Booking> findByUserId(Long userId);

    // Получение бронирований пользователя с пагинацией
    Page<Booking> findByUserId(Long userId, Pageable pageable);

    // Поиск бронирования по requestId (для идемпотентности)
    Optional<Booking> findByRequestId(String requestId);

    // Проверка конфликтов бронирований для выбранной комнаты
    @Query("SELECT b FROM Booking b WHERE b.roomId = :roomId " +
            "AND b.status = 'CONFIRMED' " +
            "AND (:startDate < b.endDate AND :endDate > b.startDate)")
    List<Booking> findConflictingBookings(
            @Param("roomId") Long roomId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );

    // Пагинированный список бронирований пользователя по username
    @Query("SELECT b FROM Booking b WHERE b.user.username = :username")
    Page<Booking> findByUserUsername(@Param("username") String username, Pageable pageable);

    // Список ID занятых комнат за период
    @Query("SELECT b.roomId FROM Booking b WHERE b.status = 'CONFIRMED' " +
            "AND ((b.startDate <= :endDate AND b.endDate >= :startDate))")
    List<Long> findBookedRoomIds(@Param("startDate") LocalDate startDate,
                                 @Param("endDate") LocalDate endDate);


}
