package org.example.util;

import io.github.cdimascio.dotenv.Dotenv;

public class EnvConfig {
    public static final Dotenv dotenv = Dotenv.configure().ignoreIfMissing().load();

    public static String get(String key, String fallback) {
        String value = dotenv.get(key);
        if (value == null || value.isBlank()) {
            value = System.getenv(key);
        }
        return value == null ? fallback : value;
    }
}
