package com.jivs.platform.controller;

import com.jivs.platform.common.constant.Constants;
import com.jivs.platform.common.dto.ApiResponse;
import com.jivs.platform.dto.auth.LoginRequest;
import com.jivs.platform.dto.auth.LoginResponse;
import com.jivs.platform.security.JwtTokenProvider;
import com.jivs.platform.security.TokenBlacklistService;
import com.jivs.platform.security.UserPrincipal;
import jakarta.servlet.http.HttpServletRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Authentication controller for login/logout operations
 */
@Slf4j
@RestController
@RequestMapping(Constants.API_V1 + "/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Authentication and authorization endpoints")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider tokenProvider;
    private final TokenBlacklistService tokenBlacklistService;

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

        LoginResponse loginResponse = LoginResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(jwtExpirationMs / 1000)
                .userId(userPrincipal.getId())
                .username(userPrincipal.getUsername())
                .email(userPrincipal.getEmail())
                .roles(roles)
                .build();

        log.info("User logged in successfully: {}", loginRequest.getUsername());

        return ResponseEntity.ok(ApiResponse.success("Login successful", loginResponse));
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
