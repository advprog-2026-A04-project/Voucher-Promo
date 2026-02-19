package com.example.demo.security;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.filter.OncePerRequestFilter;

public class AdminTokenFilter extends OncePerRequestFilter {

    private static final String ADMIN_TOKEN_HEADER = "X-Admin-Token";

    private final String adminToken;

    public AdminTokenFilter(String adminToken) {
        this.adminToken = adminToken;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return !(path.equals("/admin") || path.startsWith("/admin/"));
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String headerValue = request.getHeader(ADMIN_TOKEN_HEADER);
        if (headerValue != null && headerValue.equals(adminToken)) {
            filterChain.doFilter(request, response);
            return;
        }

        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
        response.getOutputStream().write("{\"message\":\"missing or invalid admin token\"}".getBytes(StandardCharsets.UTF_8));
    }
}

