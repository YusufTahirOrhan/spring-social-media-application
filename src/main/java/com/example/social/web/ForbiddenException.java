package com.example.social.web;

public class ForbiddenException extends RuntimeException{
    public ForbiddenException(String m) {
        super(m);
    }
}
