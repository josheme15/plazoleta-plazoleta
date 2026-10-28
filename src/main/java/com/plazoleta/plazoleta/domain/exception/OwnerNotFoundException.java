package com.plazoleta.plazoleta.domain.exception;

public class OwnerNotFoundException extends DomainException {

    public OwnerNotFoundException() {
        super("No existe un usuario con el id de propietario indicado");
    }
}
