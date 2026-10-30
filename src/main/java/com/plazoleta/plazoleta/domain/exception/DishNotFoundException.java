package com.plazoleta.plazoleta.domain.exception;

public class DishNotFoundException extends DomainException {

    public DishNotFoundException() {
        super("No existe un plato con el id indicado");
    }
}
