package com.server.app.config;

import java.util.Map;
import java.util.Set;

public class SecurityRules {

    //public endpoint
    public static final Map<String, Set<String>> PUBLIC = Map.of(
            "GET", Set.of("/api/public/info"),
            "POST", Set.of("/api/auth/login","/api/auth/signup")
    );

    //just auth required
    public static final Map<String, Set<String>> AUTH_ONLY = Map.of(
            "GET", Set.of("/api/auth/profile"),
            "POST", Set.of("/api/auth/logout")
    );

    public static final Set<String> IGNORED = Set.of("/error");

    public static boolean isPublic(String method, String path) {
        return PUBLIC.containsKey(method) && PUBLIC.get(method).contains(path);
    }

    public static boolean isAuthOnly(String method, String path) {
        return AUTH_ONLY.containsKey(method) && AUTH_ONLY.get(method).contains(path);
    }

    public static boolean isIgnored(String path) {
        return IGNORED.contains(path);
    }

    public static boolean requiresAuth(String method, String path) {
        return !isPublic(method, path) && !isIgnored(path);
    }
}