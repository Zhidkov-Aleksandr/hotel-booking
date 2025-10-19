package com.example.hotel_booking.booking_service.service;

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

@Service
@Slf4j
public class HotelServiceClient {

    private final WebClient webClient;

    @Value("${hotel-service.url}")
    private String hotelServiceUrl;

    public HotelServiceClient(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder.build();
    }

    public boolean confirmRoomAvailability(Long roomId, ConfirmAvailabilityRequest request) {
        try {
            Boolean result = webClient.post()
                    .uri(hotelServiceUrl + "/api/rooms/{id}/confirm-availability", roomId)
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(Boolean.class)
                    .timeout(Duration.ofSeconds(5))
                    .retryWhen(Retry.backoff(3, Duration.ofSeconds(1)).maxBackoff(Duration.ofSeconds(5)))
                    .block();

            return Boolean.TRUE.equals(result);
        } catch (Exception e) {
            log.error("Failed to confirm room availability: {}", e.getMessage());
            return false;
        }
    }

    public void releaseRoom(Long roomId, String requestId) {
        try {
            webClient.post()
                    .uri(uriBuilder -> uriBuilder
                            .path(hotelServiceUrl + "/api/rooms/{id}/release")
                            .queryParam("requestId", requestId)
                            .build(roomId))
                    .retrieve()
                    .bodyToMono(Void.class)
                    .timeout(Duration.ofSeconds(5))
                    .retryWhen(Retry.backoff(3, Duration.ofSeconds(1)))
                    .block();
        } catch (Exception e) {
            log.error("Failed to release room: {}", e.getMessage());
        }
    }

    public List<RoomDTO> getRecommendedRooms() {
        try {
            return webClient.get()
                    .uri(hotelServiceUrl + "/api/rooms/recommend")
                    .retrieve()
                    .bodyToFlux(RoomDTO.class)
                    .collectList()
                    .block();
        } catch (Exception e) {
            log.error("Failed to get recommended rooms: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    public RoomDTO getRoom(Long roomId) {
        try {
            return webClient.get()
                    .uri(hotelServiceUrl + "/api/rooms/{id}", roomId)
                    .retrieve()
                    .bodyToMono(RoomDTO.class)
                    .block();
        } catch (Exception e) {
            log.error("Failed to get room {}: {}", roomId, e.getMessage());
            return null;
        }
    }
}
