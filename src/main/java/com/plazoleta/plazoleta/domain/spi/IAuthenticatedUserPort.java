package com.plazoleta.plazoleta.domain.spi;

/**
 * Da acceso al usuario que hace la peticion sin que el dominio dependa de Spring Security.
 */
public interface IAuthenticatedUserPort {

    Long getAuthenticatedUserId();
}
