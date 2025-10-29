package com.interswitch.bulktransaction;

import com.interswitch.bulktransaction.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for JwtService
 * Tests JWT token generation, validation, and parsing
 */
class JwtServiceTest {

    private JwtService jwtService;

    // Test secret key (256 bits minimum for HS256)
    private static final String TEST_SECRET = "testSecretKeyForJWTAuthenticationTesting12345678901234";

    @BeforeEach
    void setUp() {
        jwtService = new JwtService(TEST_SECRET);
    }

    /**
     * Test token generation and username extraction
     */
    @Test
    void testGenerateAndExtractUsername() {
        // Arrange
        String username = "testuser";
        List<String> roles = Arrays.asList("USER", "ADMIN");

        // Act: Generate token
        String token = jwtService.generateToken(username, roles);

        // Assert: Token should not be null or empty
        assertNotNull(token);
        assertFalse(token.isEmpty());

        // Extract and verify username
        String extractedUsername = jwtService.extractUsername(token);
        assertEquals(username, extractedUsername);
    }

    /**
     * Test roles extraction from token
     */
    @Test
    void testExtractRoles() {
        // Arrange
        String username = "admin";
        List<String> roles = Arrays.asList("USER", "ADMIN");

        // Act
        String token = jwtService.generateToken(username, roles);
        List<String> extractedRoles = jwtService.extractRoles(token);

        // Assert
        assertNotNull(extractedRoles);
        assertEquals(2, extractedRoles.size());
        assertTrue(extractedRoles.contains("USER"));
        assertTrue(extractedRoles.contains("ADMIN"));
    }

    /**
     * Test token validation for valid token
     */
    @Test
    void testIsTokenValid_ValidToken() {
        // Arrange
        String token = jwtService.generateToken("user", Arrays.asList("USER"));

        // Act & Assert
        assertTrue(jwtService.isTokenValid(token));
    }

    /**
     * Test token validation for invalid token
     */
    @Test
    void testIsTokenValid_InvalidToken() {
        // Arrange: Create an invalid token
        String invalidToken = "invalid.token.here";

        // Act & Assert
        assertFalse(jwtService.isTokenValid(invalidToken));
    }

    /**
     * Test token validation for malformed token
     */
    @Test
    void testIsTokenValid_MalformedToken() {
        // Arrange
        String malformedToken = "not-a-valid-jwt";

        // Act & Assert
        assertFalse(jwtService.isTokenValid(malformedToken));
    }

    /**
     * Test multiple role scenarios
     */
    @Test
    void testDifferentRoleCombinations() {
        // Test USER only
        String userToken = jwtService.generateToken("user1", Arrays.asList("USER"));
        List<String> userRoles = jwtService.extractRoles(userToken);
        assertEquals(1, userRoles.size());
        assertEquals("USER", userRoles.get(0));

        // Test ADMIN only
        String adminToken = jwtService.generateToken("admin1", Arrays.asList("ADMIN"));
        List<String> adminRoles = jwtService.extractRoles(adminToken);
        assertEquals(1, adminRoles.size());
        assertEquals("ADMIN", adminRoles.get(0));

        // Test multiple roles
        String multiRoleToken = jwtService.generateToken("superuser",
                Arrays.asList("USER", "ADMIN", "MANAGER"));
        List<String> multiRoles = jwtService.extractRoles(multiRoleToken);
        assertEquals(3, multiRoles.size());
    }
}