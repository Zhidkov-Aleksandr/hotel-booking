package com.example.hotel_booking.booking_service.client;

import com.example.hotel_booking.booking_service.DTO.ConfirmAvailabilityRequest;
import com.example.hotel_booking.booking_service.DTO.RoomDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.Collections;
import java.util.List;

@Service
@Slf4j
public class HotelServiceClient {

    private final WebClient webClient;

    @Value("${hotel-service.url:http://hotel-service}")
    private String hotelServiceUrl;

    public HotelServiceClient(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder.build();
    }

    /**
     * Подтверждение доступности номера на указанные даты
     * @param roomId ID номера
     * @param request запрос с датами и requestId
     * @return true если номер доступен и забронирован, false в противном случае
     */
    public boolean confirmRoomAvailability(Long roomId, ConfirmAvailabilityRequest request) {
        log.info("Confirming availability for room {} with requestId: {}", roomId, request.getRequestId());

        try {
            Boolean result = webClient.post()
                    .uri(hotelServiceUrl + "/api/rooms/{id}/confirm-availability", roomId)
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(Boolean.class)
                    .timeout(Duration.ofSeconds(5))
                    .retryWhen(Retry.backoff(3, Duration.ofSeconds(1))
                            .maxBackoff(Duration.ofSeconds(5))
                            .filter(throwable -> !(throwable instanceof WebClientResponseException.BadRequest)))
                    .block();

            log.info("Room {} availability confirmation result: {}", roomId, result);
            return Boolean.TRUE.equals(result);

        } catch (WebClientResponseException e) {
            log.error("Error confirming room availability. Status: {}, Body: {}",
                    e.getStatusCode(), e.getResponseBodyAsString());
            return false;
        } catch (Exception e) {
            log.error("Error confirming room availability: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * Освобождение номера (компенсирующая транзакция)
     * @param roomId ID номера
     * @param requestId уникальный идентификатор запроса
     */
    public void releaseRoom(Long roomId, String requestId) {
        log.info("Releasing room {} for requestId: {}", roomId, requestId);

        try {
            webClient.post()
                    .uri(uriBuilder -> uriBuilder
                            .path(hotelServiceUrl + "/api/rooms/{id}/release")
                            .queryParam("requestId", requestId)
                            .build(roomId))
                    .retrieve()
                    .bodyToMono(Void.class)
                    .timeout(Duration.ofSeconds(5))
                    .retryWhen(Retry.backoff(3, Duration.ofSeconds(1))
                            .maxBackoff(Duration.ofSeconds(5)))
                    .block();

            log.info("Room {} successfully released for requestId: {}", roomId, requestId);

        } catch (Exception e) {
            log.error("Error releasing room {} for requestId {}: {}",
                    roomId, requestId, e.getMessage(), e);
        }
    }

    /**
     * Получение списка рекомендованных номеров (отсортированных по timesBooked)
     * @return список доступных номеров
     */
    public List<RoomDTO> getRecommendedRooms() {
        log.info("Fetching recommended rooms from Hotel Service");

        try {
            List<RoomDTO> rooms = webClient.get()
                    .uri(hotelServiceUrl + "/api/rooms/recommend")
                    .retrieve()
                    .bodyToFlux(RoomDTO.class)
                    .collectList()
                    .timeout(Duration.ofSeconds(5))
                    .retryWhen(Retry.backoff(3, Duration.ofSeconds(1))
                            .maxBackoff(Duration.ofSeconds(5)))
                    .block();

            log.info("Received {} recommended rooms", rooms != null ? rooms.size() : 0);
            return rooms != null ? rooms : Collections.emptyList();

        } catch (Exception e) {
            log.error("Error fetching recommended rooms: {}", e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    /**
     * Получение списка всех доступных номеров
     * @return список доступных номеров
     */
    public List<RoomDTO> getAvailableRooms() {
        log.info("Fetching available rooms from Hotel Service");

        try {
            List<RoomDTO> rooms = webClient.get()
                    .uri(hotelServiceUrl + "/api/rooms")
                    .retrieve()
                    .bodyToFlux(RoomDTO.class)
                    .collectList()
                    .timeout(Duration.ofSeconds(5))
                    .retryWhen(Retry.backoff(3, Duration.ofSeconds(1))
                            .maxBackoff(Duration.ofSeconds(5)))
                    .block();

            log.info("Received {} available rooms", rooms != null ? rooms.size() : 0);
            return rooms != null ? rooms : Collections.emptyList();

        } catch (Exception e) {
            log.error("Error fetching available rooms: {}", e.getMessage(), e);
            return Collections.emptyList();
        }
    }
}
