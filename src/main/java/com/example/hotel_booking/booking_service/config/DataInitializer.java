package com.example.hotel_booking.booking_service.config;

package com.hotel.booking.bookingservice.config;

import com.example.hotel_booking.booking_service.Entity.*;
import org.springframework.stereotype.Repository.BookingRepository;
import org.springframework.stereotype.Repository.UserRepository;
import com.example.hotel_booking.booking_service.Entity.Hotel;
import com.example.hotel_booking.booking_service.Entity.Room;
import com.example.hotel_booking.booking_service.Repository.HotelRepository;
import com.example.hotel_booking.booking_service.Repository.RoomRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Component
public class DataInitializer {

    private final UserRepository userRepo;
    private final BookingRepository bookingRepo;
    private final HotelRepository hotelRepo;
    private final RoomRepository roomRepo;

    public DataInitializer(UserRepository userRepo,
                           BookingRepository bookingRepo,
                           HotelRepository hotelRepo,
                           RoomRepository roomRepo) {
        this.userRepo = userRepo;
        this.bookingRepo = bookingRepo;
        this.hotelRepo = hotelRepo;
        this.roomRepo = roomRepo;
    }

    @PostConstruct
    @Transactional
    public void init() {
        User admin = userRepo.save(new User(null, "admin", "{bcrypt}$2a$10$...", Role.ADMIN, null));
        User user = userRepo.save(new User(null, "user", "{bcrypt}$2a$10$...", Role.USER, null));

        Hotel h1 = hotelRepo.save(new Hotel(null, "Hotel A", "Address A", null));
        Room r1 = roomRepo.save(new Room(null, h1, "101", true, 0, null, null));
        Room r2 = roomRepo.save(new Room(null, h1, "102", true, 0, null, null));

        bookingRepo.save(new Booking(null, user, r1.getId(),
                LocalDate.now().plusDays(1), LocalDate.now().plusDays(3),
                BookingStatus.CONFIRMED, null, null));
    }
}
