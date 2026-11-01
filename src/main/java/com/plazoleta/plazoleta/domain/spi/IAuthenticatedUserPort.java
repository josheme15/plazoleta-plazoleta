package com.plazoleta.plazoleta.domain.spi;

/**
 * Da acceso al usuario que hace la peticion sin que el dominio dependa de Spring Security.
 */
public interface IAuthenticatedUserPort {

    Long getAuthenticatedUserId();

    /**
     * @return el restaurante al que pertenece el usuario (solo empleados), o {@code null} si no tiene.
     */
    Long getAuthenticatedRestaurantId();
}
