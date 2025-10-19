package com.example.hotel_booking.booking_service.config;

import com.example.hotel_booking.booking_service.Entity.Booking;
import com.example.hotel_booking.booking_service.Entity.BookingStatus;
import com.example.hotel_booking.booking_service.Entity.Role;
import com.example.hotel_booking.booking_service.Entity.User;
import com.example.hotel_booking.booking_service.Repository.BookingRepository;
import com.example.hotel_booking.booking_service.Repository.UserRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Component
public class DataInitializer {

    private final UserRepository userRepo;
    private final BookingRepository bookingRepo;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(UserRepository userRepo,
                           BookingRepository bookingRepo,
                           PasswordEncoder passwordEncoder) {
        this.userRepo = userRepo;
        this.bookingRepo = bookingRepo;
        this.passwordEncoder = passwordEncoder;
    }

    @PostConstruct
    @Transactional
    public void init() {
        if (userRepo.count() == 0) {
            User admin = userRepo.save(new User(null, "admin",
                    passwordEncoder.encode("adminpass"), Role.ADMIN, null));
            User user = userRepo.save(new User(null, "user",
                    passwordEncoder.encode("userpass"), Role.USER, null));

            bookingRepo.save(new Booking(null, user, 1L,
                    LocalDate.now().plusDays(1), LocalDate.now().plusDays(3),
                    BookingStatus.CONFIRMED, null, "seed-req-1"));
        }
    }
}
