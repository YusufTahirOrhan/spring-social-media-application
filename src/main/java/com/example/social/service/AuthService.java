package com.example.social.service;

import at.favre.lib.crypto.bcrypt.BCrypt;
import com.example.social.config.AppProperties;
import com.example.social.domain.Role;
import com.example.social.domain.entity.Token;
import com.example.social.domain.entity.User;
import com.example.social.domain.repository.TokenRepository;
import com.example.social.domain.repository.UserRepository;
import com.example.social.security.CurrentUser;
import com.example.social.security.CurrentUserHolder;
import com.example.social.security.TokenUtils;
import com.example.social.web.exception.NotFoundException;
import com.example.social.web.exception.UnauthorizedException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final UserRepository users;
    private final TokenRepository tokens;
    private final AppProperties props;

    @Transactional
    public void signup(String username, String password){
        users.findUserByUsername(username).ifPresent(u -> {
            throw new IllegalArgumentException("Username already exists!");
        });
        String hash = BCrypt.withDefaults().hashToString(12, password.toCharArray());
        var user = User.builder()
                .username(username)
                .passwordHash(hash)
                .role(Role.USER)
                .deleted(false)
                .createdAt(Instant.now())
                .build();
        users.save(user);
    }

    @Transactional
    public LoginResult login(String username, String password, String userAgent, String ip){
        var user = users.findUserByUsername(username)
                .orElseThrow(() -> new NotFoundException("User not found!"));
        var result = BCrypt.verifyer().verify(password.toCharArray(), user.getPasswordHash());

        if (!result.verified){
            throw new UnauthorizedException("Invalid credentials");
        }

        int lengthBytes = props.getAuth().getToken().getLengthBytes();
        byte[] raw = TokenUtils.randomBytes(lengthBytes);
        String rawEncoded = Base64.getUrlEncoder().withoutPadding().encodeToString(raw);
        String hash = TokenUtils.sha256Hex(raw);

        Instant now = Instant.now();
        Instant exp = now.plus(props.getAuth().getToken().getTtlMinutes(), ChronoUnit.MINUTES);

        var token = Token.builder()
                .user(user)
                .tokenHash(hash)
                .issuedAt(now)
                .expiresAt(exp)
                .userAgent(userAgent)
                .ipAddress(ip)
                .build();
        tokens.save(token);

        long expiresIn = ChronoUnit.SECONDS.between(now, exp);
        return new LoginResult(rawEncoded, expiresIn);
    }

    @Transactional
    public void logout(String bearerRaw){
        if(bearerRaw == null || bearerRaw.isBlank()){
            return;
        }

        String hash = TokenUtils.sha256Hex(bearerRaw.getBytes());
        tokens.findByTokenHashAndRevokedAtIsNull(hash).ifPresent(token -> {
            token.setRevokedAt(Instant.now());
            tokens.save(token);
                });
    }

    public CurrentUser requireCurrent(){
        var currentUser = CurrentUserHolder.get();
        if(currentUser == null){
            throw new UnauthorizedException("Missing or invalid token");
        }
        return currentUser;
    }

    public record LoginResult(String accessToken, long expiresInSeconds) {}

}
