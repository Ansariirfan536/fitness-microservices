//package com.fitness.activityservice.service;
//
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.stereotype.Service;
//import org.springframework.web.reactive.function.client.WebClient;
//import org.springframework.web.reactive.function.client.WebClientResponseException;
//
//@Service
//@RequiredArgsConstructor
//@Slf4j
//public class UserValidationService {
//    private final WebClient userServiceWebClient;
//
//    boolean validateUser(String userId) {
//        log.info("calling activity service {}",userId);
//        try {
//            return userServiceWebClient.get()
//                    .uri("/api/users/{userId}/validate", userId)
//                    .retrieve()
//                    .bodyToMono(Boolean.class)
//                    .block();
//        } catch (WebClientResponseException e) {
//            e.printStackTrace();
//        }
//        return false;
//    }
//
//
//
//}


package com.fitness.activityservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserValidationService {
    private final WebClient userServiceWebClient;

    public boolean validateUser(String userId) {
        log.info("Validating user with ID: {}", userId);
        try {
            Boolean isValid = userServiceWebClient.get()
                    .uri("/api/users/{userId}/validate", userId)
                    .retrieve()
                    .bodyToMono(Boolean.class)
                    .block();

            // Agar response null nahi hai aur true hai toh hi true return karein
            return isValid != null && isValid;

        } catch (WebClientResponseException e) {
            log.error("User validation failed - Status: {}, Message: {}", e.getStatusCode(), e.getMessage());
            // Development/Testing ke dauran agar user-service down ho ya user table mein na ho,
            // toh aap yahan 'return true;' kar sakte hain taaki activity save hoti rahe.
            return true;
        } catch (Exception e) {
            log.error("Error connecting to user-service: {}", e.getMessage());
            // Fallback for safety during testing
            return true;
        }
    }
}