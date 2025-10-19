package com.example.hotel.booking.bookingservice;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

@SpringBootTest(classes = BookingServiceApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class BookingControllerIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private WebTestClient webClient;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void testCreateBooking() {
        String body = """
            {
              "roomId":1,
              "startDate":"2025-12-01",
              "endDate":"2025-12-03",
              "autoSelect":false
            }
        """;

        webClient.post()
                .uri("http://localhost:" + port + "/booking")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .exchange()
                .expectStatus().isCreated()
                .expectBody()
                .jsonPath("$.status").isEqualTo("CONFIRMED");
    }
}
