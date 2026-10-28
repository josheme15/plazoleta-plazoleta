package com.plazoleta.plazoleta.domain.spi;

import java.util.Optional;

/**
 * Puerto hacia el microservicio de usuarios.
 */
public interface IUserClientPort {

    /**
     * @return el rol del usuario (ej. "OWNER") o vacio si el usuario no existe.
     */
    Optional<String> findUserRoleById(Long userId);
}
