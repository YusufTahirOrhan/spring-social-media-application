package com.example.social.security;

import com.example.social.domain.entity.Token;
import com.example.social.domain.repository.TokenRepository;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Instant;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class AuthFilter implements Filter {
    private final TokenRepository tokens;

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
        try{
            HttpServletRequest http = (HttpServletRequest) servletRequest;
            String header = http.getHeader("Authorization");
            if(header != null && header.startsWith("Bearer ")){
                String raw = header.substring(7);
                String hash = TokenUtils.sha256Hex(raw.getBytes());
                Optional<Token> tok = tokens.findByTokenHashAndRevokedAtIsNull(hash);

                if(tok.isPresent() && tok.get().isActive(Instant.now())){
                    var u = tok.get().getUser();
                    CurrentUserHolder.set(new CurrentUser(u.getId(), u.getUsername(), u.getRole()));
                }
            }
            filterChain.doFilter(servletRequest, servletResponse);
        }
        finally {
            CurrentUserHolder.clear();
        }
    }
}
