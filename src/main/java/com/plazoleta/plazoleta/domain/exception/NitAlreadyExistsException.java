package com.plazoleta.plazoleta.domain.exception;

public class NitAlreadyExistsException extends DomainException {

    public NitAlreadyExistsException() {
        super("Ya existe un restaurante registrado con ese NIT");
    }
}
