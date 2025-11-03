package com.example.social.web.exception;

public class ForbiddenException extends RuntimeException{
    public ForbiddenException(String m) {
        super(m);
    }
}
