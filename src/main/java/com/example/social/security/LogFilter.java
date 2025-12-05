package com.example.social.security;

import com.example.social.domain.entity.ApiLog;
import com.example.social.domain.repository.ApiLogRepository;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;

@Component
@RequiredArgsConstructor
public class LogFilter implements Filter {

    private final ApiLogRepository apiLogRepository;

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
        ContentCachingRequestWrapper  requestWrapper = new ContentCachingRequestWrapper((HttpServletRequest) servletRequest,8192);
        ContentCachingResponseWrapper responseWrapper = new ContentCachingResponseWrapper((HttpServletResponse) servletResponse);

        long startTime = System.currentTimeMillis();
        try {
            filterChain.doFilter(requestWrapper, responseWrapper);
        }
        catch (Exception e) {
            filterChain.doFilter(servletRequest, servletResponse);
        }
        finally {
            long duration = System.currentTimeMillis() - startTime;

            String requestHeader = requestWrapper.getHeader("Content-Type");
            String requestBody = new String(requestWrapper.getContentAsByteArray(), StandardCharsets.UTF_8);
            String responseHeader = responseWrapper.getHeader("Content-Type");
            String responseBody = new String(responseWrapper.getContentAsByteArray(), StandardCharsets.UTF_8);

            String currentUserId = (requestWrapper.getUserPrincipal() != null)
                    ? requestWrapper.getUserPrincipal().getName()
                    : "Anonymous";

            ApiLog log = ApiLog.builder()
                    .timestamp(Instant.now())
                    .httpMethod(requestWrapper.getMethod())
                    .path(requestWrapper.getRequestURI())
                    .statusCode(responseWrapper.getStatus())
                    .duration(duration)
                    .requestHeader(requestHeader)
                    .requestBody(requestBody)
                    .responseHeader(responseHeader)
                    .responseBody(responseBody)
                    .clientIp(requestWrapper.getRemoteAddr())
                    .userAgent(requestWrapper.getHeader("User-Agent"))
                    .userId(currentUserId)
                    .build();

            apiLogRepository.save(log);

            responseWrapper.copyBodyToResponse();
        }
    }
}
