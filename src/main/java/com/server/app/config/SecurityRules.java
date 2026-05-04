package com.server.app.config;

import java.util.List;
import java.util.Set;

public class SecurityRules {

    // 🔓 No pasan por ningún filtro JWT
    public static final Set<String> PUBLIC = Set.of(
            "/api/auth/login",
            "/api/auth/signup"
    );

    // 🔐 Requieren token pero NO permisos específicos
    public static final Set<String> AUTH_ONLY = Set.of(
            "/api/auth/profile",
            "/api/auth/logout"
    );

    public static final Set<String> IGNORED = Set.of(
            "/error"
    );

    public static boolean isPublic(String path) {
        return PUBLIC.contains(path);
    }

    public static boolean isAuthOnly(String path) {
        return AUTH_ONLY.contains(path);
    }

    public static boolean isIgnored(String path) {
        return IGNORED.contains(path);
    }
}