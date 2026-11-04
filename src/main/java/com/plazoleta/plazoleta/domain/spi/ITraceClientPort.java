package com.plazoleta.plazoleta.domain.spi;

import com.plazoleta.plazoleta.domain.model.Order;
import com.plazoleta.plazoleta.domain.model.OrderStatus;

/**
 * Puerto hacia el microservicio de trazabilidad (HU-17).
 */
public interface ITraceClientPort {

    /**
     * Registra que el pedido paso de {@code previousStatus} a su estado actual.
     *
     * @param previousStatus estado anterior, o {@code null} cuando el pedido se acaba de crear.
     */
    void registerStatusChange(Order order, OrderStatus previousStatus);
}
