package com.plazoleta.plazoleta.domain.exception;

public class InvalidNitException extends DomainException {

    public InvalidNitException() {
        super("El NIT debe ser numerico");
    }
}
