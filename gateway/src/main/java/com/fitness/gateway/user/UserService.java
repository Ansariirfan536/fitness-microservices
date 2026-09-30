package com.fitness.gateway.user;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {
    private final WebClient userServiceWebClient;

    public Mono<Boolean> validateUser(String userId) {
        log.info("Calling user-service to validate userId: {}", userId);

        return userServiceWebClient.get()
                .uri("/api/users/{userId}/validate", userId)
                .retrieve()
                .bodyToMono(Boolean.class)
                .onErrorResume(WebClientResponseException.class, e -> {
                    // FIX: Agar user nahi mila (404), to error mat throw karo.
                    // Mono.just(false) return karo taaki Filter ko pata chale ki register karna hai.
                    if (e.getStatusCode() == HttpStatus.NOT_FOUND) {
                        log.warn("User {} not found in database. Returning false to trigger registration.", userId);
                        return Mono.just(false);
                    }

                    log.error("Error occurred while validating user: {}", e.getMessage());
                    if (e.getStatusCode() == HttpStatus.BAD_REQUEST) {
                        return Mono.error(new RuntimeException("Invalid user request: " + userId));
                    } else {
                        return Mono.error(new RuntimeException("Unexpected error for user: " + userId));
                    }
                });
    }

    public Mono<UserResponse> registerUser(RegisterRequest registerRequest) {
        // Log message ko thik kiya taaki debug karte waqt 'validate' aur 'register' mein confusion na ho
        log.info("🚀 Triggering WebClient call to register user with email: {}", registerRequest.getEmail());

        return userServiceWebClient.post()
                .uri("/api/users/register")
                .bodyValue(registerRequest)
                .retrieve()
                .bodyToMono(UserResponse.class)
                .doOnSuccess(response -> log.info("✅ Successfully registered user in user-service: {}", response))
                .onErrorResume(WebClientResponseException.class, e -> {
                    log.error("❌ Error occurred while registering user: Status={}, Message={}", e.getStatusCode(), e.getMessage());

                    if (e.getStatusCode() == HttpStatus.BAD_REQUEST) {
                        return Mono.error(new RuntimeException("Bad request during registration: " + e.getMessage()));
                    } else {
                        return Mono.error(new RuntimeException("Unexpected error during registration: " + e.getMessage()));
                    }
                });
    }
}
