package com.plazoleta.plazoleta.domain.exception;

public class ClientNotFoundException extends DomainException {

    public ClientNotFoundException() {
        super("No se encontro el cliente del pedido para enviarle la notificacion");
    }
}
