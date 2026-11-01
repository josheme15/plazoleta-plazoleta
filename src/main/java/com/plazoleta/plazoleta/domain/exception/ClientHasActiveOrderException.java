package com.plazoleta.plazoleta.domain.exception;

public class ClientHasActiveOrderException extends DomainException {

    public ClientHasActiveOrderException() {
        super("El cliente ya tiene un pedido en proceso (pendiente, en preparacion o listo)");
    }
}
