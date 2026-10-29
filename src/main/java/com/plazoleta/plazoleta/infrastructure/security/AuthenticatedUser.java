package com.plazoleta.plazoleta.infrastructure.security;

/**
 * Datos del usuario autenticado extraidos del JWT (claims "id" y "sub").
 */
public record AuthenticatedUser(Long id, String email) {
}
