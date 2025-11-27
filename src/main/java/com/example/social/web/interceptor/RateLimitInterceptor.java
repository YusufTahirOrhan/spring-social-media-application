package com.example.social.web.interceptor;

import com.example.social.service.RateLimitService;
import com.example.social.web.exception.TooManyRequestsException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
@RequiredArgsConstructor
public class RateLimitInterceptor implements HandlerInterceptor {
    private final RateLimitService rateLimitService;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response,
                             Object handler) throws Exception{
        String clientIp = request.getRemoteAddr();
        if(!rateLimitService.isAllowed(clientIp)){
            throw new TooManyRequestsException("Too many Requests. Please try again later.");
        }

        return true;
    }
}
