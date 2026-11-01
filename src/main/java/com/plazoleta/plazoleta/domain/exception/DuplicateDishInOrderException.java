package com.plazoleta.plazoleta.domain.exception;

public class DuplicateDishInOrderException extends DomainException {

    public DuplicateDishInOrderException() {
        super("Cada plato debe aparecer una sola vez en el pedido; use la cantidad para pedir varias unidades");
    }
}
