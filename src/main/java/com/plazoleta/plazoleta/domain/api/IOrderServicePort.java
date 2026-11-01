package com.plazoleta.plazoleta.domain.api;

import com.plazoleta.plazoleta.domain.model.Order;

public interface IOrderServicePort {

    /**
     * Crea el pedido del cliente autenticado en estado PENDIENTE.
     */
    Order createOrder(Order order);
}
