package com.interswitch.bulktransaction.controller;

import com.interswitch.bulktransaction.dto.request.TokenRequest;
import com.interswitch.bulktransaction.dto.response.AuthResponse;
import com.interswitch.bulktransaction.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final JwtService jwtService;

    /**
     * Generate token for regular USER role
     */
    @PostMapping("/token/user")
    public AuthResponse generateUserToken() {
        List<String> roles = List.of("USER");
        String token = jwtService.generateToken("test-user", roles);
        return new AuthResponse(token, "USER");
    }

    /**
     * Generate token for ADMIN role
     */
    @PostMapping("/token/admin")
    public AuthResponse generateAdminToken() {
        List<String> roles = List.of("ADMIN");
        String token = jwtService.generateToken("test-admin", roles);
        return new AuthResponse(token, "ADMIN");
    }

    /**
     * Generate token with custom username and roles
     */
    @PostMapping("/token")
    public AuthResponse generateCustomToken(@RequestBody TokenRequest request) {
        String token = jwtService.generateToken(request.username(), request.roles());
        return new AuthResponse(token, String.join(",", request.roles()));
    }
}