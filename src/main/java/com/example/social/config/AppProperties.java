package com.example.social.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "app")
public class AppProperties {
    private Auth auth = new Auth();
    private Admin admin = new Admin();

    @Data
    public static class Auth{
        private Token token = new Token();

        @Data
        public static class Token{
            private int ttlMinutes = 60;
            private int lengthBytes = 32;
        }
    }

    @Data
    public static class Admin{
        private String username = "admin";
        private String password = "admin";
    }
}
