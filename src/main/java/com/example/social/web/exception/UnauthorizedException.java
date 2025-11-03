package com.example.social.web.exception;

public class UnauthorizedException extends RuntimeException{
    public UnauthorizedException(String m) {
        super(m);
    }
}
