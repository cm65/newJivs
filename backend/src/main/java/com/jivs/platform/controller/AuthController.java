package com.jivs.platform.controller;

import com.jivs.platform.common.constant.Constants;
import com.jivs.platform.common.dto.ApiResponse;
import com.jivs.platform.dto.auth.LoginRequest;
import com.jivs.platform.dto.auth.LoginResponse;
import com.jivs.platform.dto.auth.RefreshTokenRequest;
import com.jivs.platform.security.JwtTokenProvider;
import com.jivs.platform.security.TokenBlacklistService;
import com.jivs.platform.security.UserPrincipal;
import jakarta.servlet.http.HttpServletRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Authentication controller for login/logout operations
 */
@RestController
@RequestMapping(Constants.API_V1 + "/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Authentication and authorization endpoints")
public class AuthController {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(AuthController.class);

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider tokenProvider;
    private final TokenBlacklistService tokenBlacklistService;
    private final UserDetailsService userDetailsService;

    @Value("${jivs.security.jwt.expiration}")
    private long jwtExpirationMs;

    @PostMapping("/login")
    @Operation(summary = "User login", description = "Authenticate user and return JWT tokens")
    public ResponseEntity<ApiResponse<LoginResponse>> login(@Valid @RequestBody LoginRequest loginRequest) {
        log.info("Login attempt for user: {}", loginRequest.getUsername());

        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        loginRequest.getUsername(),
                        loginRequest.getPassword()
                )
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);

        String accessToken = tokenProvider.generateToken(authentication);
        String refreshToken = tokenProvider.generateRefreshToken(authentication);

        UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
        List<String> roles = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toList());

        LoginResponse loginResponse = new LoginResponse();
        loginResponse.setAccessToken(accessToken);
        loginResponse.setRefreshToken(refreshToken);
        loginResponse.setTokenType("Bearer");
        loginResponse.setExpiresIn(jwtExpirationMs / 1000);
        loginResponse.setUserId(userPrincipal.getId());
        loginResponse.setUsername(userPrincipal.getUsername());
        loginResponse.setEmail(userPrincipal.getEmail());
        loginResponse.setRoles(roles);

        log.info("User logged in successfully: {}", loginRequest.getUsername());

        return ResponseEntity.ok(ApiResponse.success("Login successful", loginResponse));
    }

    @GetMapping("/me")
    @Operation(summary = "Get current user", description = "Get currently authenticated user information")
    public ResponseEntity<ApiResponse<LoginResponse>> getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated() ||
            authentication.getPrincipal() instanceof String) {
            return ResponseEntity.status(401).body(ApiResponse.error("Not authenticated"));
        }

        UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
        List<String> roles = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toList());

        LoginResponse response = new LoginResponse();
        response.setUserId(userPrincipal.getId());
        response.setUsername(userPrincipal.getUsername());
        response.setEmail(userPrincipal.getEmail());
        response.setRoles(roles);

        log.debug("Current user fetched: {}", userPrincipal.getUsername());

        return ResponseEntity.ok(ApiResponse.success("User info retrieved", response));
    }

    @PostMapping("/refresh")
    @Operation(summary = "Refresh access token", description = "Generate new access token using refresh token")
    public ResponseEntity<ApiResponse<LoginResponse>> refreshToken(
            @Valid @RequestBody RefreshTokenRequest request) {
        log.info("Token refresh request received");

        try {
            // Validate refresh token
            String refreshToken = request.getRefreshToken();

            if (!tokenProvider.validateToken(refreshToken)) {
                log.warn("Invalid or expired refresh token");
                return ResponseEntity.status(401)
                    .body(ApiResponse.error("Invalid or expired refresh token"));
            }

            // Get username from refresh token
            String username = tokenProvider.getUsernameFromToken(refreshToken);
            log.debug("Refresh token validated for user: {}", username);

            // Load user details
            UserDetails userDetails = userDetailsService.loadUserByUsername(username);

            // Generate new access token
            Authentication authentication = new UsernamePasswordAuthenticationToken(
                userDetails, null, userDetails.getAuthorities());
            String newAccessToken = tokenProvider.generateToken(authentication);

            // Prepare response
            UserPrincipal userPrincipal = (UserPrincipal) userDetails;
            LoginResponse response = new LoginResponse();
            response.setAccessToken(newAccessToken);
            response.setRefreshToken(refreshToken); // Keep same refresh token
            response.setTokenType("Bearer");
            response.setExpiresIn(jwtExpirationMs / 1000);
            response.setUserId(userPrincipal.getId());
            response.setUsername(userPrincipal.getUsername());
            response.setEmail(userPrincipal.getEmail());
            response.setRoles(userPrincipal.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toList()));

            log.info("Token refreshed successfully for user: {}", username);
            return ResponseEntity.ok(ApiResponse.success("Token refreshed", response));

        } catch (Exception e) {
            log.error("Token refresh failed: {}", e.getMessage(), e);
            return ResponseEntity.status(401)
                .body(ApiResponse.error("Token refresh failed"));
        }
    }

    @PostMapping("/logout")
    @Operation(summary = "User logout", description = "Logout current user and blacklist token")
    public ResponseEntity<ApiResponse<Void>> logout(HttpServletRequest request) {
        try {
            // Extract token from request
            String bearerToken = request.getHeader(Constants.AUTHORIZATION_HEADER);
            if (bearerToken != null && bearerToken.startsWith(Constants.BEARER_PREFIX)) {
                String token = bearerToken.substring(Constants.TOKEN_BEGIN_INDEX);

                // Blacklist the token
                tokenBlacklistService.blacklistToken(token);
                log.info("Token blacklisted successfully during logout");
            }

            SecurityContextHolder.clearContext();
            return ResponseEntity.ok(ApiResponse.success("Logout successful", null));
        } catch (Exception e) {
            log.error("Error during logout", e);
            SecurityContextHolder.clearContext();
            return ResponseEntity.ok(ApiResponse.success("Logout successful", null));
        }
    }
}
