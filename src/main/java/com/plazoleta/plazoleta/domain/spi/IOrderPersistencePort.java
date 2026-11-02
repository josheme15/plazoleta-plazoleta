package com.plazoleta.plazoleta.domain.spi;

import com.plazoleta.plazoleta.domain.model.Order;
import com.plazoleta.plazoleta.domain.model.OrderStatus;
import com.plazoleta.plazoleta.domain.model.PageResult;

import java.util.List;
import java.util.Optional;

public interface IOrderPersistencePort {

    Order saveOrder(Order order);

    boolean existsByClientIdAndStatusIn(Long clientId, List<OrderStatus> statuses);

    /**
     * @param status estado por el que filtrar, o {@code null} para traer todos.
     * @return la pagina solicitada de pedidos del restaurante, del mas antiguo al mas reciente.
     */
    PageResult<Order> findByRestaurant(Long restaurantId, OrderStatus status, int page, int size);

    Optional<Order> findById(Long orderId);

    /**
     * Guarda los cambios de estado y empleado asignado de un pedido existente.
     */
    Order updateOrder(Order order);
}
