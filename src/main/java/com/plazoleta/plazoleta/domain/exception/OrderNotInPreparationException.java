package com.plazoleta.plazoleta.domain.exception;

public class OrderNotInPreparationException extends DomainException {

    public OrderNotInPreparationException() {
        super("Solo se puede marcar como LISTO un pedido en estado EN_PREPARACION");
    }
}
