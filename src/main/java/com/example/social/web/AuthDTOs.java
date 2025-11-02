package com.example.social.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class AuthDTOs {
    public record SignupRequest(
            @NotBlank @Size(min = 3, max = 64) String username,
            @NotBlank @Size(min = 3, max = 72) String password
            ){}

    public record LoginRequest(
            @NotBlank String username,
            @NotBlank String password
    ){}

    public record TokenResponse(
        String accessToken,
        long expiresInSeconds
    ){}

    public record MeResponse(long id, String username, String role){}
}
