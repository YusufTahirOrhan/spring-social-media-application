package com.example.social.security;

public class CurrentUserHolder {
    private static final ThreadLocal<CurrentUser> TL = new ThreadLocal<>();

    public static void set(CurrentUser user) {
        TL.set(user);
    }

    public static CurrentUser get() {
        return TL.get();
    }

    public static void clear() {
        TL.remove();
    }
}
