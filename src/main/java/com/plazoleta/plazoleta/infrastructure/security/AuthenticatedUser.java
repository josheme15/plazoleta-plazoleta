package com.plazoleta.plazoleta.infrastructure.security;

/**
 * Datos del usuario autenticado extraidos del JWT (claims "id", "sub" y, para empleados, "restaurantId").
 */
public record AuthenticatedUser(Long id, String email, Long restaurantId) {

    public AuthenticatedUser(Long id, String email) {
        this(id, email, null);
    }
}
