package com.plazoleta.plazoleta.domain.exception;

public class UserIsNotOwnerException extends DomainException {

    public UserIsNotOwnerException() {
        super("El usuario indicado no tiene rol de propietario");
    }
}
