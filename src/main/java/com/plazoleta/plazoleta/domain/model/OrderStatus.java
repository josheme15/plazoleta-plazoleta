package com.plazoleta.plazoleta.domain.model;

import java.util.List;

/**
 * Ciclo de vida del pedido: PENDIENTE -> EN_PREPARACION -> LISTO -> ENTREGADO (o CANCELADO).
 */
public enum OrderStatus {
    PENDIENTE,
    EN_PREPARACION,
    LISTO,
    ENTREGADO,
    CANCELADO;

    /**
     * Estados "en proceso": mientras el cliente tenga un pedido en alguno de ellos, no puede hacer otro.
     */
    public static final List<OrderStatus> IN_PROCESS = List.of(PENDIENTE, EN_PREPARACION, LISTO);
}
