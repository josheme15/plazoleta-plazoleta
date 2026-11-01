package com.plazoleta.plazoleta.infrastructure.security;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AuthenticatedUserAdapterTest {

    private final AuthenticatedUserAdapter adapter = new AuthenticatedUserAdapter();

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void getAuthenticatedUserId_whenUserIsAuthenticated_returnsIdFromToken() {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                new AuthenticatedUser(7L, "dueno@correo.com"), "token", List.of()));

        assertEquals(7L, adapter.getAuthenticatedUserId());
    }

    @Test
    void getAuthenticatedUserId_whenNoAuthentication_throwsException() {
        assertThrows(AuthenticationCredentialsNotFoundException.class, adapter::getAuthenticatedUserId);
    }

    @Test
    void getAuthenticatedUserId_whenTokenHasNoId_throwsException() {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                new AuthenticatedUser(null, "dueno@correo.com"), "token", List.of()));

        assertThrows(AuthenticationCredentialsNotFoundException.class, adapter::getAuthenticatedUserId);
    }

    @Test
    void getAuthenticatedRestaurantId_forEmployee_returnsRestaurantFromToken() {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                new AuthenticatedUser(8L, "empleado@correo.com", 10L), "token", List.of()));

        assertEquals(10L, adapter.getAuthenticatedRestaurantId());
    }

    @Test
    void getAuthenticatedRestaurantId_forUserWithoutRestaurant_returnsNull() {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                new AuthenticatedUser(7L, "dueno@correo.com"), "token", List.of()));

        assertNull(adapter.getAuthenticatedRestaurantId());
    }

    @Test
    void getAuthenticatedRestaurantId_whenNoAuthentication_throwsException() {
        assertThrows(AuthenticationCredentialsNotFoundException.class, adapter::getAuthenticatedRestaurantId);
    }
}
