package com.plazoleta.plazoleta.domain.usecase;

import com.plazoleta.plazoleta.domain.api.IOrderServicePort;
import com.plazoleta.plazoleta.domain.exception.ClientHasActiveOrderException;
import com.plazoleta.plazoleta.domain.exception.DishFromAnotherRestaurantException;
import com.plazoleta.plazoleta.domain.exception.DishNotAvailableException;
import com.plazoleta.plazoleta.domain.exception.DishNotFoundException;
import com.plazoleta.plazoleta.domain.exception.DuplicateDishInOrderException;
import com.plazoleta.plazoleta.domain.exception.InvalidQuantityException;
import com.plazoleta.plazoleta.domain.exception.RequiredFieldException;
import com.plazoleta.plazoleta.domain.exception.RestaurantNotFoundException;
import com.plazoleta.plazoleta.domain.model.Dish;
import com.plazoleta.plazoleta.domain.model.Order;
import com.plazoleta.plazoleta.domain.model.OrderDish;
import com.plazoleta.plazoleta.domain.model.OrderStatus;
import com.plazoleta.plazoleta.domain.spi.IAuthenticatedUserPort;
import com.plazoleta.plazoleta.domain.spi.IDishPersistencePort;
import com.plazoleta.plazoleta.domain.spi.IOrderPersistencePort;
import com.plazoleta.plazoleta.domain.spi.IRestaurantPersistencePort;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

public class OrderUseCase implements IOrderServicePort {

    private final IOrderPersistencePort orderPersistencePort;
    private final IRestaurantPersistencePort restaurantPersistencePort;
    private final IDishPersistencePort dishPersistencePort;
    private final IAuthenticatedUserPort authenticatedUserPort;
    private final Clock clock;

    public OrderUseCase(IOrderPersistencePort orderPersistencePort,
                        IRestaurantPersistencePort restaurantPersistencePort,
                        IDishPersistencePort dishPersistencePort,
                        IAuthenticatedUserPort authenticatedUserPort,
                        Clock clock) {
        this.orderPersistencePort = orderPersistencePort;
        this.restaurantPersistencePort = restaurantPersistencePort;
        this.dishPersistencePort = dishPersistencePort;
        this.authenticatedUserPort = authenticatedUserPort;
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
        return orderPersistencePort.saveOrder(order);
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
