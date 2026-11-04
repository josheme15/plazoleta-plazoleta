package com.plazoleta.plazoleta.domain.api;

import com.plazoleta.plazoleta.domain.model.Order;

/**
 * Cambios de estado del pedido. Cada cambio queda registrado en trazabilidad (HU-17).
 */
public interface IOrderStatusServicePort {

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

    /**
     * El cliente autenticado cancela uno de sus pedidos. Solo se pueden cancelar pedidos PENDIENTE.
     */
    Order cancelOrder(Long orderId);
}
