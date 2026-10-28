package com.plazoleta.plazoleta.infrastructure.output.client.exception;

public class UserServiceUnavailableException extends RuntimeException {

    public UserServiceUnavailableException(Throwable cause) {
        super("No fue posible validar el propietario: el servicio de usuarios no esta disponible", cause);
    }
}
