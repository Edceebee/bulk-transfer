package com.interswitch.bulktransaction.security;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

/**
 * Service for JWT token operations using Auth0 Java JWT
 * Handles token validation, parsing, and user extraction
 */
@Service
public class JwtService {

    private final Algorithm algorithm;
    private final JWTVerifier verifier;

    /**
     * Constructor initializes the algorithm and verifier
     */
    public JwtService(@Value("${jwt.secret}") String secret) {
        this.algorithm = Algorithm.HMAC256(secret);
        this.verifier = JWT.require(algorithm).build();
    }

    /**
     * Extracts username from JWT token
     */
    public String extractUsername(String token) {
        DecodedJWT jwt = verifier.verify(token);
        return jwt.getSubject();
    }

    /**
     * Extracts user roles from JWT token
     */
    public List<String> extractRoles(String token) {
        DecodedJWT jwt = verifier.verify(token);
        return jwt.getClaim("roles").asList(String.class);
    }

    /**
     * Validates if the token is valid
     */
    public boolean isTokenValid(String token) {
        try {
            verifier.verify(token);
            return true;
        } catch (JWTVerificationException e) {
            return false;
        }
    }

    /**
     * Generates a JWT token
     */
    public String generateToken(String username, List<String> roles) {
        return JWT.create()
                .withSubject(username)
                .withClaim("roles", roles)
                .withIssuedAt(new Date())
                .withExpiresAt(new Date(System.currentTimeMillis() + 1000 * 60 * 60 * 2)) // token will last for 2 hours
                .sign(algorithm);
    }
}