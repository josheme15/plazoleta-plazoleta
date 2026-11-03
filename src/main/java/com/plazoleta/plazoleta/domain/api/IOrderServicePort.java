package com.plazoleta.plazoleta.domain.api;

import com.plazoleta.plazoleta.domain.model.Order;
import com.plazoleta.plazoleta.domain.model.OrderStatus;
import com.plazoleta.plazoleta.domain.model.PageResult;

public interface IOrderServicePort {

    /**
     * Crea el pedido del cliente autenticado en estado PENDIENTE.
     */
    Order createOrder(Order order);

    /**
     * Pedidos del restaurante del empleado autenticado, paginados, del mas antiguo al mas reciente.
     *
     * @param status estado por el que filtrar, o {@code null} para traer todos.
     */
    PageResult<Order> listRestaurantOrders(OrderStatus status, Integer page, Integer size);

    /**
     * El empleado autenticado se asigna un pedido PENDIENTE de su restaurante, que pasa a EN_PREPARACION.
     */
    Order assignOrder(Long orderId);

    /**
     * El empleado asignado marca su pedido EN_PREPARACION como LISTO: se genera un PIN de seguridad y se envia
     * por SMS al celular del cliente.
     */
    Order markOrderReady(Long orderId);

    /**
     * El empleado entrega un pedido LISTO de su restaurante, validando el PIN de seguridad que el cliente recibio por
     * SMS. El pedido pasa a ENTREGADO, estado final que ya no se puede modificar.
     */
    Order deliverOrder(Long orderId, String pin);
}
