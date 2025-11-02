package com.example.social.web;

public class UnauthorizedException extends RuntimeException{
    public UnauthorizedException(String m) {
        super(m);
    }
}
