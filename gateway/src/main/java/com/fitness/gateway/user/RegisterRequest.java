package com.fitness.gateway.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RegisterRequest {
    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email formate")
    private String email;
    @NotBlank(message = "Password is required")
    @Size(min=5,message = "password must have atLeast 5 characters")
    private String keycloakId;
    private String password;
    private String firstName;
    private String lastName;
}
