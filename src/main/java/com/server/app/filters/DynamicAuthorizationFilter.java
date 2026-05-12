package com.server.app.filters;

import java.io.IOException;

import com.server.app.config.SecurityRules;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.server.app.dto.response.ExceptionResponse;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class DynamicAuthorizationFilter extends OncePerRequestFilter {

    private final AntPathMatcher pathMatcher = new AntPathMatcher();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {

        String method = request.getMethod();
        String path = request.getRequestURI();

        // 🚫 Ignorados
        if (SecurityRules.isIgnored(path)) {
            filterChain.doFilter(request, response);
            return;
        }

        // 🔓 Públicos
        if (SecurityRules.isPublic(method, path)) {
            filterChain.doFilter(request, response);
            return;
        }

        // 🔐 Solo autenticados
        if (SecurityRules.isAuthOnly(method, path)) {
            filterChain.doFilter(request, response);
            return;
        }

        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        if (authentication != null && authentication.isAuthenticated()) {

            if (!isAuthorized(authentication, method, path)) {

                sendError(response, HttpServletResponse.SC_FORBIDDEN,"Acceso denegado: no tienes permisos para esta ruta: " + path);

                return;
            }
        }

        filterChain.doFilter(request, response);
    }

    private boolean isAuthorized(Authentication authentication, String method, String path) {

        return authentication.getAuthorities().stream().anyMatch(a -> {

            String authority = a.getAuthority();

            String[] parts = authority.split(":", 2);

            if (parts.length != 2) return false;

            String authMethod = parts[0];
            String authPath = parts[1];

            return method.equalsIgnoreCase(authMethod) && pathMatcher.match(authPath, path);
        });
    }

    private void sendError(HttpServletResponse response, int status,String message) throws IOException {

        if (response.isCommitted()) {
            return;
        }

        response.setStatus(status);
        response.setContentType("application/json;charset=UTF-8");

        ExceptionResponse error = new ExceptionResponse(status, message);

        String payload = objectMapper.writeValueAsString(error);

        response.getWriter().write(payload);
    }
}