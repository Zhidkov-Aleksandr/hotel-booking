package com.example.booking_service.client;

import com.example.booking_service.dto.ConfirmAvailabilityRequest;
import com.example.booking_service.dto.RoomDTO;
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

    @Value("${hotel-service.url:https://hotel-service}")
    private String baseUrl;

    public HotelServiceClient(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder.build();
    }

    public RoomDTO getRoom(Long roomId) {
        try {
            return webClient.get()
                    .uri(baseUrl + "/api/rooms/{id}", roomId)
                    .retrieve()
                    .bodyToMono(RoomDTO.class)
                    .block();
        } catch (Exception e) {
            log.error("Error fetching room {}: {}", roomId, e.getMessage());
            return null;
        }
    }

    public List<RoomDTO> getRecommendedRooms() {
        try {
            List<RoomDTO> list = webClient.get()
                    .uri(baseUrl + "/api/rooms/recommend")
                    .retrieve()
                    .bodyToFlux(RoomDTO.class)
                    .collectList()
                    .timeout(Duration.ofSeconds(5))
                    .retryWhen(Retry.backoff(3, Duration.ofSeconds(1)))
                    .block();
            return list != null ? list : Collections.emptyList();
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
