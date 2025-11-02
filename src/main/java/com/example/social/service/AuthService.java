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
import com.example.social.web.NotFoundException;
import com.example.social.web.UnauthorizedException;
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
        var u = User.builder()
                .username(username)
                .passwordHash(hash)
                .role(Role.USER)
                .deleted(false)
                .createdAt(Instant.now())
                .build();
        users.save(u);
    }

    @Transactional
    public LoginResult login(String username, String password, String userAgent, String ip){
        var u = users.findUserByUsername(username)
                .orElseThrow(() -> new NotFoundException("User not found!"));
        var res = BCrypt.verifyer().verify(password.toCharArray(), u.getPasswordHash());

        if (!res.verified){
            throw new UnauthorizedException("Invalid credentials");
        }

        int len = props.getAuth().getToken().getLengthBytes();
        byte[] raw = TokenUtils.randomBytes(len);
        String rawEncoded = Base64.getUrlEncoder().withoutPadding().encodeToString(raw);
        String hash = TokenUtils.sha256Hex(raw);

        Instant now = Instant.now();
        Instant exp = now.plus(props.getAuth().getToken().getTtlMinutes(), ChronoUnit.MINUTES);

        var t = Token.builder()
                .user(u)
                .tokenHash(hash)
                .issuedAt(now)
                .expiresAt(exp)
                .userAgent(userAgent)
                .ipAddress(ip)
                .build();
        tokens.save(t);

        long expiresIn = ChronoUnit.SECONDS.between(now, exp);
        return new LoginResult(rawEncoded, expiresIn);
    }

    @Transactional
    public void logout(String bearerRaw){
        if(bearerRaw == null || bearerRaw.isBlank()){
            return;
        }

        String hash = TokenUtils.sha256Hex(bearerRaw.getBytes());
        tokens.findByTokenHashAndRevokedAtIsNull(hash).ifPresent(tok -> {
            tok.setRevokedAt(Instant.now());
            tokens.save(tok);
                });
    }

    public CurrentUser requireCurrent(){
        var cu = CurrentUserHolder.get();
        if(cu == null){
            throw new UnauthorizedException("Missing or invalid token");
        }
        return cu;
    }

    public record LoginResult(String accessToken, long expiresInSeconds) {}

}
