package com.server.app.config;

import java.util.List;
import java.util.Map;
import java.util.Set;

public class SecurityRules {

  public static final Map<String, Set<String>> PUBLIC = Map.of(
      "GET", Set.of("/api/public/info"),
      "POST", Set.of("/api/auth/login", "/api/auth/signup", "/api/auth/refresh", "/api/auth/logout"));

  public static final Map<String, Set<String>> AUTH_ONLY = Map.of(
      "GET", Set.of("/api/auth/profile"),
      "PUT", Set.of("/api/auth/profile"),
      "PATCH", Set.of("/api/auth/password"));

  public static final Set<String> IGNORED_EXACT = Set.of(
      "/error",
      "/swagger-ui.html",
      "/docs.html");

  public static final List<String> IGNORED_PREFIXES = List.of(
      "/v3/api-docs",
      "/swagger-ui/");

  public static boolean isPublic(String method, String path) {
    return PUBLIC.containsKey(method) && PUBLIC.get(method).contains(path);
  }

  public static boolean isAuthOnly(String method, String path) {
    return AUTH_ONLY.containsKey(method) && AUTH_ONLY.get(method).contains(path);
  }

  public static boolean isIgnored(String path) {
    return IGNORED_EXACT.contains(path) || IGNORED_PREFIXES.stream().anyMatch(path::startsWith);
  }

  public static boolean requiresAuth(String method, String path) {
    return !isPublic(method, path) && !isIgnored(path);
  }
}