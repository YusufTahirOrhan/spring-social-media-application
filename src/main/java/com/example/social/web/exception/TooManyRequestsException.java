package com.example.social.web.exception;

public class TooManyRequestsException extends  RuntimeException {
    public TooManyRequestsException(String message) {
        super(message);
    }
}
