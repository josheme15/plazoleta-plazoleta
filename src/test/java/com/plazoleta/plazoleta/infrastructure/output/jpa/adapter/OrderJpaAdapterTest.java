package com.plazoleta.plazoleta.infrastructure.output.jpa.adapter;

import com.plazoleta.plazoleta.domain.model.Order;
import com.plazoleta.plazoleta.domain.model.OrderDish;
import com.plazoleta.plazoleta.domain.model.OrderStatus;
import com.plazoleta.plazoleta.infrastructure.output.jpa.entity.DishEntity;
import com.plazoleta.plazoleta.infrastructure.output.jpa.entity.OrderDishEntity;
import com.plazoleta.plazoleta.infrastructure.output.jpa.entity.OrderEntity;
import com.plazoleta.plazoleta.infrastructure.output.jpa.entity.RestaurantEntity;
import com.plazoleta.plazoleta.infrastructure.output.jpa.repository.IDishRepository;
import com.plazoleta.plazoleta.infrastructure.output.jpa.repository.IOrderRepository;
import com.plazoleta.plazoleta.infrastructure.output.jpa.repository.IRestaurantRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderJpaAdapterTest {

    private static final LocalDateTime DATE = LocalDateTime.of(2026, 11, 1, 12, 30);

    @Mock
    private IOrderRepository orderRepository;

    @Mock
    private IRestaurantRepository restaurantRepository;

    @Mock
    private IDishRepository dishRepository;

    @InjectMocks
    private OrderJpaAdapter adapter;

    private RestaurantEntity restaurant(Long id) {
        RestaurantEntity entity = new RestaurantEntity();
        entity.setId(id);
        return entity;
    }

    private DishEntity dish(Long id) {
        DishEntity entity = new DishEntity();
        entity.setId(id);
        return entity;
    }

    @Test
    void saveOrder_buildsOrderWithDishesAndMapsSavedEntityBack() {
        when(restaurantRepository.getReferenceById(10L)).thenReturn(restaurant(10L));
        when(dishRepository.getReferenceById(1L)).thenReturn(dish(1L));
        when(dishRepository.getReferenceById(2L)).thenReturn(dish(2L));
        when(orderRepository.save(any(OrderEntity.class))).thenAnswer(inv -> {
            OrderEntity saved = inv.getArgument(0);
            saved.setId(100L);
            return saved;
        });
        Order order = new Order(null, 30L, 10L, DATE, OrderStatus.PENDIENTE, null,
                List.of(new OrderDish(1L, 2), new OrderDish(2L, 1)));

        Order result = adapter.saveOrder(order);

        ArgumentCaptor<OrderEntity> captor = ArgumentCaptor.forClass(OrderEntity.class);
        verify(orderRepository).save(captor.capture());
        OrderEntity entity = captor.getValue();
        assertEquals(30L, entity.getClientId());
        assertEquals(OrderStatus.PENDIENTE, entity.getStatus());
        assertEquals(10L, entity.getRestaurant().getId());
        assertEquals(2, entity.getDishes().size());
        OrderDishEntity firstDish = entity.getDishes().get(0);
        assertSame(entity, firstDish.getOrder());
        assertEquals(1L, firstDish.getDish().getId());
        assertEquals(2, firstDish.getQuantity());

        assertEquals(100L, result.getId());
        assertEquals(30L, result.getClientId());
        assertEquals(10L, result.getRestaurantId());
        assertEquals(DATE, result.getDate());
        assertEquals(OrderStatus.PENDIENTE, result.getStatus());
        assertNull(result.getEmployeeId());
        assertEquals(List.of(1L, 2L), result.getDishes().stream().map(OrderDish::getDishId).toList());
        assertEquals(List.of(2, 1), result.getDishes().stream().map(OrderDish::getQuantity).toList());
    }

    @Test
    void existsByClientIdAndStatusIn_delegatesToRepository() {
        when(orderRepository.existsByClientIdAndStatusIn(30L, OrderStatus.IN_PROCESS)).thenReturn(true);

        assertTrue(adapter.existsByClientIdAndStatusIn(30L, OrderStatus.IN_PROCESS));
    }
}
