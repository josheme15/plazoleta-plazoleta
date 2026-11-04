package com.plazoleta.plazoleta.domain.usecase;

import com.plazoleta.plazoleta.domain.api.IOrderServicePort;
import com.plazoleta.plazoleta.domain.exception.ClientHasActiveOrderException;
import com.plazoleta.plazoleta.domain.exception.DishFromAnotherRestaurantException;
import com.plazoleta.plazoleta.domain.exception.DishNotAvailableException;
import com.plazoleta.plazoleta.domain.exception.DishNotFoundException;
import com.plazoleta.plazoleta.domain.exception.DuplicateDishInOrderException;
import com.plazoleta.plazoleta.domain.exception.EmployeeWithoutRestaurantException;
import com.plazoleta.plazoleta.domain.exception.InvalidPaginationException;
import com.plazoleta.plazoleta.domain.exception.InvalidQuantityException;
import com.plazoleta.plazoleta.domain.exception.RequiredFieldException;
import com.plazoleta.plazoleta.domain.exception.RestaurantNotFoundException;
import com.plazoleta.plazoleta.domain.model.Dish;
import com.plazoleta.plazoleta.domain.model.Order;
import com.plazoleta.plazoleta.domain.model.OrderDish;
import com.plazoleta.plazoleta.domain.model.OrderStatus;
import com.plazoleta.plazoleta.domain.model.PageResult;
import com.plazoleta.plazoleta.domain.spi.IAuthenticatedUserPort;
import com.plazoleta.plazoleta.domain.spi.IDishPersistencePort;
import com.plazoleta.plazoleta.domain.spi.IOrderPersistencePort;
import com.plazoleta.plazoleta.domain.spi.IRestaurantPersistencePort;
import com.plazoleta.plazoleta.domain.spi.ITraceClientPort;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

/**
 * Crear pedidos (HU-11) y listar los pedidos del restaurante (HU-12). Los cambios de estado estan en
 * {@link OrderStatusUseCase}.
 */
public class OrderUseCase implements IOrderServicePort {

    private final IOrderPersistencePort orderPersistencePort;
    private final IRestaurantPersistencePort restaurantPersistencePort;
    private final IDishPersistencePort dishPersistencePort;
    private final IAuthenticatedUserPort authenticatedUserPort;
    private final ITraceClientPort traceClientPort;
    private final Clock clock;

    public OrderUseCase(IOrderPersistencePort orderPersistencePort,
                        IRestaurantPersistencePort restaurantPersistencePort,
                        IDishPersistencePort dishPersistencePort,
                        IAuthenticatedUserPort authenticatedUserPort,
                        ITraceClientPort traceClientPort,
                        Clock clock) {
        this.orderPersistencePort = orderPersistencePort;
        this.restaurantPersistencePort = restaurantPersistencePort;
        this.dishPersistencePort = dishPersistencePort;
        this.authenticatedUserPort = authenticatedUserPort;
        this.traceClientPort = traceClientPort;
        this.clock = clock;
    }

    @Override
    public Order createOrder(Order order) {
        validateRequestData(order);

        if (restaurantPersistencePort.findById(order.getRestaurantId()).isEmpty()) {
            throw new RestaurantNotFoundException();
        }
        Long clientId = authenticatedUserPort.getAuthenticatedUserId();
        if (orderPersistencePort.existsByClientIdAndStatusIn(clientId, OrderStatus.IN_PROCESS)) {
            throw new ClientHasActiveOrderException();
        }
        order.getDishes().forEach(item -> validateDish(item.getDishId(), order.getRestaurantId()));

        order.setClientId(clientId);
        order.setStatus(OrderStatus.PENDIENTE);
        order.setDate(LocalDateTime.now(clock));
        order.setEmployeeId(null);
        Order saved = orderPersistencePort.saveOrder(order);
        // HU-17: primer registro de trazabilidad (sin estado anterior -> PENDIENTE).
        traceClientPort.registerStatusChange(saved, null);
        return saved;
    }

    @Override
    public PageResult<Order> listRestaurantOrders(OrderStatus status, Integer page, Integer size) {
        if (page == null || size == null || page < 0 || size <= 0) {
            throw new InvalidPaginationException();
        }
        // El restaurante sale del token del empleado: solo puede ver los pedidos de su restaurante.
        Long restaurantId = authenticatedUserPort.getAuthenticatedRestaurantId();
        if (restaurantId == null) {
            throw new EmployeeWithoutRestaurantException();
        }
        return orderPersistencePort.findByRestaurant(restaurantId, status, page, size);
    }

    private void validateRequestData(Order order) {
        if (order.getRestaurantId() == null) {
            throw new RequiredFieldException("idRestaurante");
        }
        if (order.getDishes() == null || order.getDishes().isEmpty()) {
            throw new RequiredFieldException("platos");
        }
        Set<Long> dishIds = new HashSet<>();
        for (OrderDish item : order.getDishes()) {
            if (item.getDishId() == null) {
                throw new RequiredFieldException("idPlato");
            }
            if (item.getQuantity() == null) {
                throw new RequiredFieldException("cantidad");
            }
            if (item.getQuantity() <= 0) {
                throw new InvalidQuantityException();
            }
            if (!dishIds.add(item.getDishId())) {
                throw new DuplicateDishInOrderException();
            }
        }
    }

    private void validateDish(Long dishId, Long restaurantId) {
        Dish dish = dishPersistencePort.findById(dishId).orElseThrow(DishNotFoundException::new);
        if (!restaurantId.equals(dish.getRestaurantId())) {
            throw new DishFromAnotherRestaurantException();
        }
        if (!Boolean.TRUE.equals(dish.getActive())) {
            throw new DishNotAvailableException();
        }
    }
}
