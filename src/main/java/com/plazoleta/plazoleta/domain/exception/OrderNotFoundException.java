package com.plazoleta.plazoleta.domain.exception;

public class OrderNotFoundException extends DomainException {

    public OrderNotFoundException() {
        super("No existe un pedido con el id indicado");
    }
}
