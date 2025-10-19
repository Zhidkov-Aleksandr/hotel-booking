# Booking Service

Проект `booking-service` — сервис управления бронированиями для отелей на Spring Boot.

## Структура проекта

booking-service/
└── src/
└── main/
└── java/
└── com/
└── hotel/
└── booking/
└── bookingservice/
├── BookingServiceApplication.java
├── client/
│ └── HotelServiceClient.java
├── config/
│ ├── OpenApiConfig.java
│ ├── SecurityConfig.java
│ └── WebClientConfig.java
├── controller/
│ ├── BookingController.java
│ └── UserController.java
├── dto/
│ ├── AuthResponse.java
│ ├── BookingDTO.java
│ ├── ConfirmAvailabilityRequest.java
│ ├── CreateBookingRequest.java
│ ├── CreateUserRequest.java
│ ├── ErrorResponse.java
│ ├── LoginRequest.java
│ ├── RegisterRequest.java
│ ├── RoomDTO.java
│ ├── UpdateUserRequest.java
│ └── UserDTO.java
├── entity/
│ ├── Booking.java
│ ├── BookingStatus.java
│ ├── Role.java
│ └── User.java
├── exception/
│ └── GlobalExceptionHandler.java
├── repository/
│ ├── BookingRepository.java
│ └── UserRepository.java
├── security/
│ ├── JwtAuthenticationFilter.java
│ └── JwtUtil.java
└── service/
├── BookingService.java
├── JwtService.java
└── UserService.java


## Описание директорий

- **client/** — HTTP-клиенты для взаимодействия с другими сервисами (например, Hotel Service).
- **config/** — конфигурация приложения (Spring Security, OpenAPI, WebClient).
- **controller/** — REST-контроллеры для обработки HTTP-запросов.
- **dto/** — объекты передачи данных (Data Transfer Objects).
- **entity/** — JPA-сущности и перечисления.
- **exception/** — глобальные обработчики ошибок.
- **repository/** — интерфейсы репозиториев для работы с базой данных.
- **security/** — JWT и фильтры безопасности.
- **service/** — бизнес-логика сервиса.

## Технологии

- Java 17
- Spring Boot 3
- Spring Data JPA
- Spring Security (JWT)
- Spring WebFlux (WebClient)
- MapStruct
- Lombok



