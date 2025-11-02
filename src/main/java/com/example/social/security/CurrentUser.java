package com.example.social.security;

import com.example.social.domain.Role;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class CurrentUser {
    public final Long id;
    public final String username;
    public final Role role;
}
