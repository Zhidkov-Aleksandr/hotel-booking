package com.example.hotel_bookin.booking_service.client;

import com.example.hotel_booking.booking_service.dto.ConfirmAvailabilityRequest;
import com.example.hotel_booking.booking_service.dto.RoomDTO;
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

    @Value("${hotel-service.url:https://hotel-service}")
    private String baseUrl;

    public HotelServiceClient(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder.build();
    }

    private RoomDTO map(com.example.hotel_booking.hotel_service.dto.RoomDTO h) {
        if (h == null) {
            return null;
        }
        return RoomDTO.builder()
                .id(h.getId())
                .hotelId(h.getHotelId())
                .number(h.getNumber())
                .available(h.getAvailable())
                .timesBooked(h.getTimesBooked())
                .build();
    }

    public RoomDTO getRoom(Long roomId) {
        try {
            com.example.hotel_booking.hotel_service.dto.RoomDTO h = webClient.get()
                    .uri(baseUrl + "/api/rooms/{id}", roomId)
                    .retrieve()
                    .bodyToMono(com.example.hotel_booking.hotel_service.dto.RoomDTO.class)
                    .block();
            return map(h);
        } catch (Exception e) {
            log.error("Error fetching room {}: {}", roomId, e.getMessage());
            return null;
        }
    }

    public List<RoomDTO> getRecommendedRooms() {
        try {
            List<com.example.hotel_booking.hotel_service.dto.RoomDTO> hs = webClient.get()
                    .uri(baseUrl + "/api/rooms/recommend")
                    .retrieve()
                    .bodyToFlux(com.example.hotel_booking.hotel_service.dto.RoomDTO.class)
                    .collectList()
                    .timeout(Duration.ofSeconds(5))
                    .retryWhen(Retry.backoff(3, Duration.ofSeconds(1)))
                    .block();
            if (hs == null) {
                return Collections.emptyList();
            }
            return hs.stream()
                    .map(this::map)
                    .collect(Collectors.toList());
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
