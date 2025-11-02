package com.example.social.web.controller;

import com.example.social.service.AuthService;
import com.example.social.web.AuthDTOs;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;

    @PostMapping("/signup")
    public ResponseEntity<Void> signup(@RequestBody @Valid AuthDTOs.SignupRequest signupRequest){
        authService.signup(signupRequest.username(), signupRequest.password());
        return ResponseEntity.ok().build();
    }

    @PostMapping("login")
    public ResponseEntity<AuthDTOs.TokenResponse> login(@RequestBody @Valid AuthDTOs.LoginRequest loginRequest, HttpServletRequest http){
        var response = authService.login(loginRequest.username(), loginRequest.password(),
                http.getHeader("User-Agent"), http.getRemoteAddr());

        return ResponseEntity.ok(new AuthDTOs.TokenResponse(response.accessToken(), response.expiresInSeconds()));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@RequestHeader (name = "Authorization", required = false) String authHeader){
        String raw = (authHeader != null &&  authHeader.startsWith("Bearer "))?authHeader.substring(7):null;
        authService.logout(raw);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/me")
    public ResponseEntity<AuthDTOs.MeResponse> me(){
        var currentUser = authService.requireCurrent();
        return ResponseEntity.ok(new AuthDTOs.MeResponse(currentUser.id, currentUser.username, currentUser.role.name()));
    }
}
