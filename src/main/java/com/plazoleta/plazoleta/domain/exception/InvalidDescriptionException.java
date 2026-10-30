package com.plazoleta.plazoleta.domain.exception;

public class InvalidDescriptionException extends DomainException {

    public InvalidDescriptionException() {
        super("La descripcion del plato no puede estar vacia");
    }
}
