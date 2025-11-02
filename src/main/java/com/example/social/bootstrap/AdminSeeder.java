package com.example.social.bootstrap;

import at.favre.lib.crypto.bcrypt.BCrypt;
import com.example.social.config.AppProperties;
import com.example.social.domain.Role;
import com.example.social.domain.entity.User;
import com.example.social.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
@RequiredArgsConstructor
public class AdminSeeder implements CommandLineRunner {
    private final UserRepository users;
    private final AppProperties properties;


    @Override
    public void run(String... args){
        String username = properties.getAdmin().getUsername();
        users.findUserByUsername(username).orElseGet(()->{
            String hash = BCrypt.withDefaults().hashToString(12, properties.getAdmin().getPassword().toCharArray());
            var admin = User.builder()
                    .username(username)
                    .passwordHash(hash)
                    .role(Role.ADMIN)
                    .deleted(false)
                    .createdAt(Instant.now())
                    .build();
            return users.save(admin);
        });
    }
}
