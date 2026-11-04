package com.plazoleta.plazoleta.infrastructure.output.client.exception;

public class TraceServiceUnavailableException extends RuntimeException {

    public TraceServiceUnavailableException(Throwable cause) {
        super("No fue posible registrar el cambio de estado: el servicio de trazabilidad no esta disponible. "
                + "El pedido no fue modificado, intentalo de nuevo", cause);
    }
}
