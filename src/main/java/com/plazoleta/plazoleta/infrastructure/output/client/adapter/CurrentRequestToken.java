package com.plazoleta.plazoleta.infrastructure.output.client.adapter;

import org.springframework.http.HttpHeaders;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Reenvia el JWT de la peticion actual a los otros microservicios (el filtro JWT lo guarda como credentials).
 */
final class CurrentRequestToken {

    private static final String BEARER_PREFIX = "Bearer ";

    private CurrentRequestToken() {
    }

    static void addTo(HttpHeaders headers) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getCredentials() instanceof String token) {
            headers.set(HttpHeaders.AUTHORIZATION, BEARER_PREFIX + token);
        }
    }
}
