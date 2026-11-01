package com.plazoleta.plazoleta.infrastructure.output.jpa.adapter;

import com.plazoleta.plazoleta.domain.model.Order;
import com.plazoleta.plazoleta.domain.model.OrderDish;
import com.plazoleta.plazoleta.domain.model.OrderStatus;
import com.plazoleta.plazoleta.domain.spi.IOrderPersistencePort;
import com.plazoleta.plazoleta.infrastructure.output.jpa.entity.OrderDishEntity;
import com.plazoleta.plazoleta.infrastructure.output.jpa.entity.OrderEntity;
import com.plazoleta.plazoleta.infrastructure.output.jpa.repository.IDishRepository;
import com.plazoleta.plazoleta.infrastructure.output.jpa.repository.IOrderRepository;
import com.plazoleta.plazoleta.infrastructure.output.jpa.repository.IRestaurantRepository;
import lombok.RequiredArgsConstructor;

import java.util.List;

@RequiredArgsConstructor
public class OrderJpaAdapter implements IOrderPersistencePort {

    private final IOrderRepository orderRepository;
    private final IRestaurantRepository restaurantRepository;
    private final IDishRepository dishRepository;

    @Override
    public Order saveOrder(Order order) {
        OrderEntity entity = new OrderEntity();
        entity.setClientId(order.getClientId());
        entity.setDate(order.getDate());
        entity.setStatus(order.getStatus());
        entity.setEmployeeId(order.getEmployeeId());
        entity.setRestaurant(restaurantRepository.getReferenceById(order.getRestaurantId()));
        for (OrderDish item : order.getDishes()) {
            OrderDishEntity dishEntity = new OrderDishEntity();
            dishEntity.setOrder(entity);
            dishEntity.setDish(dishRepository.getReferenceById(item.getDishId()));
            dishEntity.setQuantity(item.getQuantity());
            entity.getDishes().add(dishEntity);
        }
        return toOrder(orderRepository.save(entity));
    }

    @Override
    public boolean existsByClientIdAndStatusIn(Long clientId, List<OrderStatus> statuses) {
        return orderRepository.existsByClientIdAndStatusIn(clientId, statuses);
    }

    private Order toOrder(OrderEntity entity) {
        List<OrderDish> dishes = entity.getDishes().stream()
                .map(item -> new OrderDish(item.getDish().getId(), item.getQuantity()))
                .toList();
        return new Order(entity.getId(), entity.getClientId(), entity.getRestaurant().getId(), entity.getDate(),
                entity.getStatus(), entity.getEmployeeId(), dishes);
    }
}
