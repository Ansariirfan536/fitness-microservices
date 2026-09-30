package com.fitness.userservice.dto;

import com.fasterxml.jackson.annotation.JsonProperty; // <-- Yeh import add karein
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RegisterRequest {
    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    private String email;


    @JsonProperty("keycloakId")
    private String keyCloakId;

    @NotBlank(message = "Password is required")
    @Size(min=5, message = "password must have at least 5 characters")
    private String password;

    private String firstName;
    private String lastName;
}
