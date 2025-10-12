package com.jivs.platform.config;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;

import java.io.IOException;

/**
 * Configuration for security headers to prevent XSS, clickjacking, and other attacks
 * Implements OWASP security best practices
 */
@Configuration
@Order(Ordered.HIGHEST_PRECEDENCE)
public class SecurityHeadersConfig implements Filter {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(SecurityHeadersConfig.class);

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletResponse httpResponse = (HttpServletResponse) response;

        // 1. Content Security Policy (CSP) - Prevents XSS attacks
        httpResponse.setHeader("Content-Security-Policy",
            "default-src 'self'; " +
            "script-src 'self' 'unsafe-inline' 'unsafe-eval'; " + // Allow inline scripts for React
            "style-src 'self' 'unsafe-inline'; " +
            "img-src 'self' data: https:; " +
            "font-src 'self' data:; " +
            "connect-src 'self' ws: wss:; " + // Allow WebSocket connections
            "frame-ancestors 'none'; " +      // Prevent clickjacking
            "base-uri 'self'; " +
            "form-action 'self'"
        );

        // 2. X-Content-Type-Options - Prevents MIME type sniffing
        httpResponse.setHeader("X-Content-Type-Options", "nosniff");

        // 3. X-Frame-Options - Prevents clickjacking
        httpResponse.setHeader("X-Frame-Options", "DENY");

        // 4. X-XSS-Protection - Enable browser XSS filter (legacy support)
        httpResponse.setHeader("X-XSS-Protection", "1; mode=block");

        // 5. Strict-Transport-Security - Force HTTPS
        httpResponse.setHeader("Strict-Transport-Security",
            "max-age=31536000; includeSubDomains; preload");

        // 6. Referrer-Policy - Control referrer information
        httpResponse.setHeader("Referrer-Policy", "strict-origin-when-cross-origin");

        // 7. Permissions-Policy - Control browser features
        httpResponse.setHeader("Permissions-Policy",
            "geolocation=(), " +
            "microphone=(), " +
            "camera=(), " +
            "payment=(), " +
            "usb=(), " +
            "magnetometer=(), " +
            "accelerometer=(), " +
            "gyroscope=()"
        );

        // 8. Cache-Control for sensitive endpoints
        String uri = httpResponse.toString();
        if (uri != null && (uri.contains("/api/") || uri.contains("/auth/"))) {
            httpResponse.setHeader("Cache-Control",
                "no-cache, no-store, must-revalidate, private");
            httpResponse.setHeader("Pragma", "no-cache");
            httpResponse.setHeader("Expires", "0");
        }

        // 9. Remove server header to hide server information
        httpResponse.setHeader("Server", "");

        log.debug("Security headers added to response");

        chain.doFilter(request, response);
    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        log.info("Security Headers Filter initialized");
    }

    @Override
    public void destroy() {
        log.info("Security Headers Filter destroyed");
    }
}
