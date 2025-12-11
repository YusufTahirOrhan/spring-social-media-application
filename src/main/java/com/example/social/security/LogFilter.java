package com.example.social.security;

import com.example.social.domain.entity.ApiLog;
import com.example.social.domain.repository.ApiLogRepository;
import com.example.social.domain.repository.ClickHouseLogRepository; // Yeni eklenen
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j; // Loglama için eklendi (Opsiyonel)
import org.springframework.stereotype.Component;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class LogFilter implements Filter {

    private final ApiLogRepository apiLogRepository;
    private final ClickHouseLogRepository clickHouseLogRepository;

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

            String logId = UUID.randomUUID().toString();

            ApiLog log = ApiLog.builder()
                    .id(logId)
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

            try {
                clickHouseLogRepository.save(log);
            } catch (Exception e) {
                System.err.println("ClickHouse Logging Error: " + e.getCause().getMessage());
            }

            responseWrapper.copyBodyToResponse();
        }
    }
}