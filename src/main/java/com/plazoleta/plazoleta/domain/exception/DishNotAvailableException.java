package com.plazoleta.plazoleta.domain.exception;

public class DishNotAvailableException extends DomainException {

    public DishNotAvailableException() {
        super("El pedido incluye un plato que no esta disponible (desactivado)");
    }
}
