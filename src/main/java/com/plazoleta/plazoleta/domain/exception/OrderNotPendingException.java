package com.plazoleta.plazoleta.domain.exception;

public class OrderNotPendingException extends DomainException {

    public OrderNotPendingException() {
        super("Solo se puede asignar un pedido en estado PENDIENTE");
    }
}
