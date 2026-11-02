package com.plazoleta.plazoleta.infrastructure.output.client.exception;

public class MessagingServiceUnavailableException extends RuntimeException {

    public MessagingServiceUnavailableException(Throwable cause) {
        super("No fue posible notificar al cliente: el servicio de mensajeria no esta disponible. "
                + "El pedido sigue EN_PREPARACION, intentalo de nuevo", cause);
    }
}
