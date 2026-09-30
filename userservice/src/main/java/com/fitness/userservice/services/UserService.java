//package com.fitness.userservice.services;
//
//import com.fitness.userservice.UserRepository;
//import com.fitness.userservice.dto.RegisterRequest;
//import com.fitness.userservice.dto.UserResponse;
//import com.fitness.userservice.models.User;
//import lombok.AllArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.stereotype.Service;
//
//@Service
//@AllArgsConstructor
//@Slf4j
//public class UserService {
//    private  final UserRepository repository;
//    public UserResponse register(RegisterRequest request) {
//
//        if(repository.existsByEmail(request.getEmail())){
//            User existingUser=repository.findByEmail(request.getEmail());
//            UserResponse userResponse=new UserResponse();
//            userResponse.setId(existingUser.getId());
//            userResponse.setPassword(existingUser.getPassword());
//            userResponse.setEmail(existingUser.getEmail());
//            userResponse.setFirstName(existingUser.getFirstName());
//            userResponse.setLastName(existingUser.getLastName());
//            userResponse.setCreatedAt(existingUser.getCreatedAt());
//            userResponse.setUpdatedAt(existingUser.getUpdatedAt());
//
//            return  userResponse;
//        }
//
//
//
//        User user=new User();
//        user.setEmail(request.getEmail());
//        user.setFirstName(request.getFirstName());
//        user.setKeycloakId(request.getKeyCloakId());
//        user.setLastName(request.getLastName());
//        user.setPassword(request.getEmail());
//
//
//        User savedUser=repository.save(user);
//        UserResponse userResponse=new UserResponse();
//        userResponse.setId(savedUser.getId());
//        userResponse.setPassword(savedUser.getPassword());
//        userResponse.setEmail(savedUser.getEmail());
//        userResponse.setFirstName(savedUser.getFirstName());
//        userResponse.setLastName(savedUser.getLastName());
//        userResponse.setKeycloakId(savedUser.getKeycloakId());
//        userResponse.setCreatedAt(savedUser.getCreatedAt());
//        userResponse.setUpdatedAt(savedUser.getUpdatedAt());
//
//         savedUser = repository.save(user);
//        log.info("🚀 Successfully saved new user in DB. DB ID: {}, Keycloak ID: {}", savedUser.getId(), savedUser.getKeycloakId());
//
//
//        return  userResponse;
//    }
//
//    public  UserResponse getUserProfile(String userId) {
//        User user=repository.findById(userId)
//                .orElseThrow(()->new RuntimeException("User not found"));
//
//
//        UserResponse userResponse=new UserResponse();
//        userResponse.setId(user.getId());
//        userResponse.setPassword(user.getPassword());
//        userResponse.setEmail(user.getEmail());
//        userResponse.setFirstName(user.getFirstName());
//        userResponse.setKeycloakId(user.getKeycloakId());
//        userResponse.setLastName(user.getLastName());
//        userResponse.setCreatedAt(user.getCreatedAt());
//        userResponse.setUpdatedAt(user.getUpdatedAt());
//
//        return  userResponse;
//
//    }
//
//    public Boolean  existByUserId(String userId) {
//        log.info("calling user service {}",userId);
//        return  repository.existsByKeycloakId(userId);
//    }
//}


package com.fitness.userservice.services;

import com.fitness.userservice.UserRepository;
import com.fitness.userservice.dto.RegisterRequest;
import com.fitness.userservice.dto.UserResponse;
import com.fitness.userservice.models.User;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
@Slf4j
public class UserService {
    private final UserRepository repository;

    public UserResponse register(RegisterRequest request) {
        // Fallback agar email null ya empty aaye
        String emailToUse = request.getEmail();
        if (emailToUse == null || emailToUse.trim().isEmpty()) {
            emailToUse = request.getKeyCloakId() + "@placeholder.com";
        }

        if (repository.existsByEmail(emailToUse)) {
            User existingUser = repository.findByEmail(emailToUse);
            if (existingUser != null) {
                UserResponse userResponse = new UserResponse();
                userResponse.setId(existingUser.getId());
                userResponse.setPassword(existingUser.getPassword());
                userResponse.setEmail(existingUser.getEmail());
                userResponse.setFirstName(existingUser.getFirstName());
                userResponse.setLastName(existingUser.getLastName());
                userResponse.setKeycloakId(existingUser.getKeycloakId());
                userResponse.setCreatedAt(existingUser.getCreatedAt());
                userResponse.setUpdatedAt(existingUser.getUpdatedAt());
                return userResponse;
            }
        }

        User user = new User();
        user.setEmail(emailToUse);
        user.setFirstName(request.getFirstName() != null ? request.getFirstName() : "User");
        user.setKeycloakId(request.getKeyCloakId());
        user.setLastName(request.getLastName() != null ? request.getLastName() : "");
        user.setPassword(emailToUse);

        User savedUser = repository.save(user);
        log.info("🚀 Successfully saved new user in DB. DB ID: {}, Keycloak ID: {}", savedUser.getId(), savedUser.getKeycloakId());

        UserResponse userResponse = new UserResponse();
        userResponse.setId(savedUser.getId());
        userResponse.setPassword(savedUser.getPassword());
        userResponse.setEmail(savedUser.getEmail());
        userResponse.setFirstName(savedUser.getFirstName());
        userResponse.setLastName(savedUser.getLastName());
        userResponse.setKeycloakId(savedUser.getKeycloakId());
        userResponse.setCreatedAt(savedUser.getCreatedAt());
        userResponse.setUpdatedAt(savedUser.getUpdatedAt());

        return userResponse;
    }

    public UserResponse getUserProfile(String userId) {
        User user = repository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        UserResponse userResponse = new UserResponse();
        userResponse.setId(user.getId());
        userResponse.setPassword(user.getPassword());
        userResponse.setEmail(user.getEmail());
        userResponse.setFirstName(user.getFirstName());
        userResponse.setKeycloakId(user.getKeycloakId());
        userResponse.setLastName(user.getLastName());
        userResponse.setCreatedAt(user.getCreatedAt());
        userResponse.setUpdatedAt(user.getUpdatedAt());

        return userResponse;
    }

    public Boolean existByUserId(String userId) {
        log.info("calling user service {}", userId);
        return repository.existsByKeycloakId(userId);
    }
}