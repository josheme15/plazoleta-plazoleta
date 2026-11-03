package com.plazoleta.plazoleta.domain.exception;

public class InvalidPinException extends DomainException {

    public InvalidPinException() {
        super("El PIN de seguridad es incorrecto");
    }
}
