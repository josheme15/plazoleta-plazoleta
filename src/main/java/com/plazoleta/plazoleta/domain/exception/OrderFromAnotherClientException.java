package com.plazoleta.plazoleta.domain.exception;

public class OrderFromAnotherClientException extends DomainException {

    public OrderFromAnotherClientException() {
        super("El pedido no pertenece al cliente autenticado");
    }
}
