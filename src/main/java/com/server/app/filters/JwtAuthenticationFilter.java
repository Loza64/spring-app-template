package com.server.app.filters;

import java.io.IOException;
import java.util.Set;
import java.util.stream.Collectors;

import com.server.app.config.SecurityRules;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.server.app.config.JsonWebToken;
import com.server.app.dto.response.ExceptionResponse;
import com.server.app.entities.User;
import com.server.app.services.UserService;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JsonWebToken jwtUtil;
    private final UserService userService;

    public JwtAuthenticationFilter(@Lazy JsonWebToken jwtUtil,
                                   UserService userService) {

        this.jwtUtil = jwtUtil;
        this.userService = userService;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {

        String method = request.getMethod();
        String path = request.getRequestURI();

        return SecurityRules.isPublic(method, path)
                || SecurityRules.isIgnored(path);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {

        final String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            sendErrorResponse(response, HttpServletResponse.SC_UNAUTHORIZED, "Bearer token required");
            return;
        }

        final String token = authHeader.substring(7);

        try {

            if (jwtUtil.isTokenExpired(token)) {
                sendErrorResponse(response, HttpServletResponse.SC_UNAUTHORIZED, "Token expired");
                return;
            }

            // 📦 Claims
            Claims claims = jwtUtil.extracClaims(token);

            if (claims == null) {
                sendErrorResponse(response, HttpServletResponse.SC_UNAUTHORIZED, "Invalid token data");
                return;
            }

            Integer userId = jwtUtil.extractIdUser(token);

            if (userId == null) {
                sendErrorResponse(response,HttpServletResponse.SC_UNAUTHORIZED,"Token data invalid");
                return;
            }

            User user = userService.findById(userId);

            if (user == null) {
                sendErrorResponse(response,HttpServletResponse.SC_UNAUTHORIZED,"Your account has been deleted");
                return;
            }

            if (user.isBlocked()) {
                sendErrorResponse(response, HttpServletResponse.SC_UNAUTHORIZED, "Your account has been blocked");
                return;
            }

            if (!user.getRole().getActive()) {
                sendErrorResponse(response, HttpServletResponse.SC_UNAUTHORIZED, "Your account role is not active");
                return;
            }

            Set<GrantedAuthority> authorities =
                    user.getRole()
                            .getPermissions()
                            .stream()
                            .map(permission ->
                                    new SimpleGrantedAuthority(permission.getMethod() + ":" + permission.getPath())
                            )
                            .collect(Collectors.toSet());

            authorities.add(new SimpleGrantedAuthority("ROLE_" + user.getRole().getName()));

            UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(user,null, authorities);

            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

            SecurityContextHolder.getContext().setAuthentication(authentication);

            filterChain.doFilter(request, response);

        } catch (ExpiredJwtException e) {

            sendErrorResponse(response, HttpServletResponse.SC_UNAUTHORIZED, "Token expirado" );

        } catch (JwtException e) {

            sendErrorResponse(response, HttpServletResponse.SC_UNAUTHORIZED, "Token inválido");

        } catch (Exception e) {
            sendErrorResponse(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Error interno del servidor");
        }
    }

    private void sendErrorResponse(HttpServletResponse response, int status, String message) throws IOException {

        if (response.isCommitted())  return;

        response.setStatus(status);
        response.setContentType("application/json");

        ExceptionResponse error = new ExceptionResponse(status, message);

        String json = new ObjectMapper().writeValueAsString(error);

        response.getWriter().write(json);
    }
}