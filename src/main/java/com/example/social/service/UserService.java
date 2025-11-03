package com.example.social.service;

import at.favre.lib.crypto.bcrypt.BCrypt;
import com.example.social.domain.Role;
import com.example.social.domain.entity.Token;
import com.example.social.domain.entity.User;
import com.example.social.domain.repository.TokenRepository;
import com.example.social.domain.repository.UserRepository;
import com.example.social.security.CurrentUser;
import com.example.social.web.exception.ForbiddenException;
import com.example.social.web.exception.NotFoundException;
import com.example.social.web.exception.UnauthorizedException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository users;
    private final TokenRepository tokens;
    private final AuthService authService;

    @Transactional()
    public User getUserVisibleById(Long id){
        User user = users.findById(id).orElseThrow(()-> new NotFoundException("User not found"));

        if(user.isDeleted()){
            throw new NotFoundException("User not found");
        }

        return user;
    }

    @Transactional(readOnly = true)
    public List<User> getUsersByIds(List<Long> ids) {
        return users.findAllById(ids);
    }

    @Transactional
    public void updateOwnPassword(String currentPassword, String newPassword){
        CurrentUser currentUser = authService.requireCurrent();
        User user = users.findById(currentUser.id()).orElseThrow(()-> new UnauthorizedException("Invalid credentials or session. Please log in again."));

        var verified = BCrypt.verifyer()
                .verify(currentPassword.toCharArray(), user.getPasswordHash())
                .verified;

        if(!verified){
            throw new ForbiddenException("Current password does not match");
        }

        String newHash = BCrypt.withDefaults().hashToString(12, newPassword.toCharArray());
        user.setPasswordHash(newHash);
        users.save(user);

        revokeAllActiveTokensOf(user.getId());
    }

    @Transactional
    public void deleteSelf(){
        CurrentUser currentUser = authService.requireCurrent();
        User user = users.findById(currentUser.id()).orElseThrow(()-> new UnauthorizedException("Invalid credentials or session. Please log in again."));

        if (user.isDeleted()){
            throw new ForbiddenException("User already deleted.");
        }

        user.setDeleted(true);
        users.save(user);

        revokeAllActiveTokensOf(user.getId());
    }

    @Transactional
    public void adminDeleteUser(Long id){
        CurrentUser currentUser = authService.requireCurrent();
        if(currentUser.role() != Role.ADMIN){
            throw new ForbiddenException("Admin only");
        }

        User user = users.findById(id).orElseThrow(()-> new NotFoundException("User not found"));

        if(user.isDeleted()){
            throw new ForbiddenException("User already deleted.");
        }

        user.setDeleted(true);
        users.save(user);

        revokeAllActiveTokensOf(user.getId());
    }

    private void revokeAllActiveTokensOf(Long userId) {
        List<Token> active = tokens.findAllByUser_IdAndRevokedAtIsNull(userId);
        if (active.isEmpty()) return;
        Instant now = Instant.now();
        active.forEach(t -> t.setRevokedAt(now));
        tokens.saveAll(active);
    }
}
