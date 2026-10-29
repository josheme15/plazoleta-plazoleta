package com.plazoleta.plazoleta.domain.exception;

public class CategoryNotFoundException extends DomainException {

    public CategoryNotFoundException() {
        super("No existe una categoria con el id indicado");
    }
}
