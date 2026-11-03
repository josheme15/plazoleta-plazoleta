package com.plazoleta.plazoleta.domain.exception;

public class OrderNotReadyException extends DomainException {

    public OrderNotReadyException() {
        super("Solo se puede marcar como ENTREGADO un pedido en estado LISTO");
    }
}
