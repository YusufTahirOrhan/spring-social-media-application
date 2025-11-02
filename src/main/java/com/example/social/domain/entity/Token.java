package com.example.social.domain.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "tokens")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Token {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "token_hash", length = 64, nullable = false)
    private String tokenHash;

    @Column(nullable = false)
    private Instant issuedAt;
    @Column(nullable = false)
    private Instant expiresAt;
    private Instant revokedAt;

    private String userAgent;
    private String ipAddress;

    public boolean isActive(Instant now) {
        return revokedAt == null && now.isBefore(expiresAt);
    }
}
