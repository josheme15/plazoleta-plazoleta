package com.plazoleta.plazoleta.infrastructure.security;

import com.plazoleta.plazoleta.domain.spi.IAuthenticatedUserPort;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Obtiene el id del usuario autenticado desde el contexto de seguridad (lo carga JwtAuthenticationFilter).
 */
public class AuthenticatedUserAdapter implements IAuthenticatedUserPort {

    @Override
    public Long getAuthenticatedUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof AuthenticatedUser user
                && user.id() != null) {
            return user.id();
        }
        throw new AuthenticationCredentialsNotFoundException("No hay un usuario autenticado en la peticion");
    }
}
