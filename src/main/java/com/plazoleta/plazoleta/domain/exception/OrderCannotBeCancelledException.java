package com.plazoleta.plazoleta.domain.exception;

public class OrderCannotBeCancelledException extends DomainException {

    public OrderCannotBeCancelledException() {
        // Mensaje exacto que pide la HU-16.
        super("Lo sentimos, tu pedido ya está en preparación y no se puede cancelar");
    }
}
