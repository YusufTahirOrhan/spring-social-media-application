package com.example.social.web.exception;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.Map;

@Data
@Builder
public class ApiError {
    private Instant timestamp;
    private String path;
    private int status;
    private String code;
    private String message;
    private Map<String, Object> details;
}
