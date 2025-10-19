package com.example.hotel_booking.booking_service.client;

import com.example.hotel_booking.booking_service.dto.ConfirmAvailabilityRequest;
import com.example.hotel_booking.booking_service.dto.RoomDTO; // ✅ Используем DTO из booking_service
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
public class HotelServiceClient {

    private final WebClient webClient;

    @Value("${hotel-service.url:http://localhost:8081}") // ✅ Лучше сразу localhost для отладки
    private String baseUrl;

    public HotelServiceClient(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder.build();
    }

    // ✅ Внутреннее отображение — из "удалённого" DTO в локальный DTO
    private RoomDTO map(RemoteRoomDTO h) {
        if (h == null) return null;
        return RoomDTO.builder()
                .id(h.getId())
                .hotelId(h.getHotelId())
                .number(h.getNumber())
                .available(h.getAvailable())
                .timesBooked(h.getTimesBooked())
                .build();
    }

    // ✅ Локальный класс для данных, приходящих из hotel-service
    private static class RemoteRoomDTO {
        private Long id;
        private Long hotelId;
        private String number;
        private Boolean available;
        private Integer timesBooked;

        // Геттеры и сеттеры (или можно добавить Lombok @Data)
        public Long getId() { return id; }
        public Long getHotelId() { return hotelId; }
        public String getNumber() { return number; }
        public Boolean getAvailable() { return available; }
        public Integer getTimesBooked() { return timesBooked; }

        public void setId(Long id) { this.id = id; }
        public void setHotelId(Long hotelId) { this.hotelId = hotelId; }
        public void setNumber(String number) { this.number = number; }
        public void setAvailable(Boolean available) { this.available = available; }
        public void setTimesBooked(Integer timesBooked) { this.timesBooked = timesBooked; }
    }

    public RoomDTO getRoom(Long roomId) {
        try {
            RemoteRoomDTO h = webClient.get()
                    .uri(baseUrl + "/api/rooms/{id}", roomId)
                    .retrieve()
                    .bodyToMono(RemoteRoomDTO.class)
                    .block();
            return map(h);
        } catch (Exception e) {
            log.error("Error fetching room {}: {}", roomId, e.getMessage());
            return null;
        }
    }

    public List<RoomDTO> getRecommendedRooms() {
        try {
            List<RemoteRoomDTO> hs = webClient.get()
                    .uri(baseUrl + "/api/rooms/recommend")
                    .retrieve()
                    .bodyToFlux(RemoteRoomDTO.class)
                    .collectList()
                    .timeout(Duration.ofSeconds(5))
                    .retryWhen(Retry.backoff(3, Duration.ofSeconds(1)))
                    .block();

            if (hs == null || hs.isEmpty()) {
                return Collections.emptyList();
            }

            return hs.stream().map(this::map).collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Error fetching recommended rooms: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    public boolean confirmRoomAvailability(Long roomId, ConfirmAvailabilityRequest req) {
        try {
            Boolean result = webClient.post()
                    .uri(baseUrl + "/api/rooms/{id}/confirm-availability", roomId)
                    .bodyValue(req)
                    .retrieve()
                    .bodyToMono(Boolean.class)
                    .timeout(Duration.ofSeconds(5))
                    .retryWhen(Retry.backoff(3, Duration.ofSeconds(1)))
                    .block();
            return Boolean.TRUE.equals(result);
        } catch (Exception e) {
            log.error("Error confirming availability for room {}: {}", roomId, e.getMessage());
            return false;
        }
    }

    public void releaseRoom(Long roomId, String requestId) {
        try {
            webClient.post()
                    .uri(uriBuilder -> uriBuilder
                            .path(baseUrl + "/api/rooms/{id}/release")
                            .queryParam("requestId", requestId)
                            .build(roomId))
                    .retrieve()
                    .bodyToMono(Void.class)
                    .timeout(Duration.ofSeconds(5))
                    .retryWhen(Retry.backoff(3, Duration.ofSeconds(1)))
                    .block();
        } catch (Exception e) {
            log.error("Error releasing room {}: {}", roomId, e.getMessage());
        }
    }
}
