package com.plazoleta.plazoleta.domain.usecase;

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
import com.plazoleta.plazoleta.domain.model.Restaurant;
import com.plazoleta.plazoleta.domain.spi.IAuthenticatedUserPort;
import com.plazoleta.plazoleta.domain.spi.IDishPersistencePort;
import com.plazoleta.plazoleta.domain.spi.IOrderPersistencePort;
import com.plazoleta.plazoleta.domain.spi.IRestaurantPersistencePort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderUseCaseTest {

    private static final Long CLIENT_ID = 30L;
    private static final Long RESTAURANT_ID = 10L;
    private static final Long OTHER_RESTAURANT_ID = 11L;
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 11, 1, 12, 30);

    @Mock
    private IOrderPersistencePort orderPersistencePort;

    @Mock
    private IRestaurantPersistencePort restaurantPersistencePort;

    @Mock
    private IDishPersistencePort dishPersistencePort;

    @Mock
    private IAuthenticatedUserPort authenticatedUserPort;

    private OrderUseCase orderUseCase;

    @BeforeEach
    void setUp() {
        Clock fixedClock = Clock.fixed(NOW.toInstant(ZoneOffset.UTC), ZoneOffset.UTC);
        orderUseCase = new OrderUseCase(orderPersistencePort, restaurantPersistencePort, dishPersistencePort,
                authenticatedUserPort, fixedClock);
    }

    private Order order(OrderDish... dishes) {
        Order order = new Order();
        order.setRestaurantId(RESTAURANT_ID);
        order.setDishes(new ArrayList<>(List.of(dishes)));
        return order;
    }

    private Dish dish(Long id, Long restaurantId, boolean active) {
        return new Dish(id, "Plato " + id, 10000, "Descripcion", "https://img", 2L, restaurantId, active);
    }

    private void givenRestaurantExistsAndClientWithoutActiveOrder() {
        when(restaurantPersistencePort.findById(RESTAURANT_ID)).thenReturn(Optional.of(new Restaurant()));
        when(authenticatedUserPort.getAuthenticatedUserId()).thenReturn(CLIENT_ID);
        when(orderPersistencePort.existsByClientIdAndStatusIn(CLIENT_ID, OrderStatus.IN_PROCESS)).thenReturn(false);
    }

    @Test
    void createOrder_withValidData_savesPendingOrderOfAuthenticatedClient() {
        givenRestaurantExistsAndClientWithoutActiveOrder();
        when(dishPersistencePort.findById(1L)).thenReturn(Optional.of(dish(1L, RESTAURANT_ID, true)));
        when(dishPersistencePort.findById(2L)).thenReturn(Optional.of(dish(2L, RESTAURANT_ID, true)));
        when(orderPersistencePort.saveOrder(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));
        Order order = order(new OrderDish(1L, 2), new OrderDish(2L, 1));
        order.setEmployeeId(99L);

        Order result = orderUseCase.createOrder(order);

        ArgumentCaptor<Order> captor = ArgumentCaptor.forClass(Order.class);
        verify(orderPersistencePort).saveOrder(captor.capture());
        Order saved = captor.getValue();
        assertEquals(OrderStatus.PENDIENTE, saved.getStatus());
        assertEquals(CLIENT_ID, saved.getClientId());
        assertEquals(RESTAURANT_ID, saved.getRestaurantId());
        assertEquals(NOW, saved.getDate());
        assertNull(saved.getEmployeeId());
        assertEquals(2, saved.getDishes().size());
        assertEquals(2, saved.getDishes().get(0).getQuantity());
        assertSame(saved, result);
    }

    @Test
    void createOrder_ignoresStatusSentByClient_alwaysPending() {
        givenRestaurantExistsAndClientWithoutActiveOrder();
        when(dishPersistencePort.findById(1L)).thenReturn(Optional.of(dish(1L, RESTAURANT_ID, true)));
        when(orderPersistencePort.saveOrder(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));
        Order order = order(new OrderDish(1L, 1));
        order.setStatus(OrderStatus.ENTREGADO);

        assertEquals(OrderStatus.PENDIENTE, orderUseCase.createOrder(order).getStatus());
    }

    @Test
    void createOrder_whenClientHasOrderInProcess_throwsException() {
        when(restaurantPersistencePort.findById(RESTAURANT_ID)).thenReturn(Optional.of(new Restaurant()));
        when(authenticatedUserPort.getAuthenticatedUserId()).thenReturn(CLIENT_ID);
        when(orderPersistencePort.existsByClientIdAndStatusIn(CLIENT_ID, OrderStatus.IN_PROCESS)).thenReturn(true);

        assertThrows(ClientHasActiveOrderException.class, () -> orderUseCase.createOrder(order(new OrderDish(1L, 1))));
        verify(dishPersistencePort, never()).findById(any());
        verify(orderPersistencePort, never()).saveOrder(any());
    }

    @Test
    void inProcessStatuses_arePendingInPreparationAndReady() {
        assertEquals(List.of(OrderStatus.PENDIENTE, OrderStatus.EN_PREPARACION, OrderStatus.LISTO),
                OrderStatus.IN_PROCESS);
    }

    @Test
    void createOrder_whenRestaurantDoesNotExist_throwsException() {
        when(restaurantPersistencePort.findById(RESTAURANT_ID)).thenReturn(Optional.empty());

        assertThrows(RestaurantNotFoundException.class, () -> orderUseCase.createOrder(order(new OrderDish(1L, 1))));
        verify(orderPersistencePort, never()).saveOrder(any());
    }

    @Test
    void createOrder_whenDishDoesNotExist_throwsException() {
        givenRestaurantExistsAndClientWithoutActiveOrder();
        when(dishPersistencePort.findById(1L)).thenReturn(Optional.empty());

        assertThrows(DishNotFoundException.class, () -> orderUseCase.createOrder(order(new OrderDish(1L, 1))));
        verify(orderPersistencePort, never()).saveOrder(any());
    }

    @Test
    void createOrder_whenDishBelongsToAnotherRestaurant_throwsException() {
        givenRestaurantExistsAndClientWithoutActiveOrder();
        when(dishPersistencePort.findById(1L)).thenReturn(Optional.of(dish(1L, RESTAURANT_ID, true)));
        when(dishPersistencePort.findById(2L)).thenReturn(Optional.of(dish(2L, OTHER_RESTAURANT_ID, true)));

        assertThrows(DishFromAnotherRestaurantException.class,
                () -> orderUseCase.createOrder(order(new OrderDish(1L, 1), new OrderDish(2L, 1))));
        verify(orderPersistencePort, never()).saveOrder(any());
    }

    @Test
    void createOrder_whenDishIsInactive_throwsException() {
        givenRestaurantExistsAndClientWithoutActiveOrder();
        when(dishPersistencePort.findById(1L)).thenReturn(Optional.of(dish(1L, RESTAURANT_ID, false)));

        assertThrows(DishNotAvailableException.class, () -> orderUseCase.createOrder(order(new OrderDish(1L, 1))));
        verify(orderPersistencePort, never()).saveOrder(any());
    }

    @Test
    void createOrder_withRepeatedDish_throwsException() {
        assertThrows(DuplicateDishInOrderException.class,
                () -> orderUseCase.createOrder(order(new OrderDish(1L, 1), new OrderDish(1L, 2))));
        verify(restaurantPersistencePort, never()).findById(any());
    }

    @ParameterizedTest
    @ValueSource(ints = {0, -1, -10})
    void createOrder_withNonPositiveQuantity_throwsException(int quantity) {
        assertThrows(InvalidQuantityException.class,
                () -> orderUseCase.createOrder(order(new OrderDish(1L, quantity))));
        verify(orderPersistencePort, never()).saveOrder(any());
    }

    @Test
    void createOrder_withNullQuantity_throwsException() {
        assertThrows(RequiredFieldException.class, () -> orderUseCase.createOrder(order(new OrderDish(1L, null))));
    }

    @Test
    void createOrder_withNullDishId_throwsException() {
        assertThrows(RequiredFieldException.class, () -> orderUseCase.createOrder(order(new OrderDish(null, 1))));
    }

    @Test
    void createOrder_withoutDishes_throwsException() {
        assertThrows(RequiredFieldException.class, () -> orderUseCase.createOrder(order()));
        verify(restaurantPersistencePort, never()).findById(any());
    }

    @Test
    void createOrder_withNullDishList_throwsException() {
        Order order = order();
        order.setDishes(null);

        assertThrows(RequiredFieldException.class, () -> orderUseCase.createOrder(order));
    }

    @Test
    void createOrder_withoutRestaurant_throwsException() {
        Order order = order(new OrderDish(1L, 1));
        order.setRestaurantId(null);

        assertThrows(RequiredFieldException.class, () -> orderUseCase.createOrder(order));
        verify(orderPersistencePort, never()).saveOrder(any());
    }

    // ---------- HU-12: pedidos del restaurante del empleado, con filtro por estado ----------

    @Test
    void listRestaurantOrders_withStatus_returnsOrdersOfEmployeeRestaurant() {
        when(authenticatedUserPort.getAuthenticatedRestaurantId()).thenReturn(RESTAURANT_ID);
        PageResult<Order> page = new PageResult<>(List.of(new Order()), 0, 10, 1, 1);
        when(orderPersistencePort.findByRestaurant(RESTAURANT_ID, OrderStatus.PENDIENTE, 0, 10)).thenReturn(page);

        assertSame(page, orderUseCase.listRestaurantOrders(OrderStatus.PENDIENTE, 0, 10));
    }

    @Test
    void listRestaurantOrders_withoutStatus_returnsAllOrdersOfEmployeeRestaurant() {
        when(authenticatedUserPort.getAuthenticatedRestaurantId()).thenReturn(RESTAURANT_ID);
        PageResult<Order> page = new PageResult<>(List.of(), 2, 5, 10, 2);
        when(orderPersistencePort.findByRestaurant(RESTAURANT_ID, null, 2, 5)).thenReturn(page);

        assertSame(page, orderUseCase.listRestaurantOrders(null, 2, 5));
    }

    @Test
    void listRestaurantOrders_alwaysUsesRestaurantOfTheToken() {
        when(authenticatedUserPort.getAuthenticatedRestaurantId()).thenReturn(OTHER_RESTAURANT_ID);
        when(orderPersistencePort.findByRestaurant(OTHER_RESTAURANT_ID, null, 0, 10))
                .thenReturn(new PageResult<>(List.of(), 0, 10, 0, 0));

        orderUseCase.listRestaurantOrders(null, 0, 10);

        verify(orderPersistencePort).findByRestaurant(OTHER_RESTAURANT_ID, null, 0, 10);
        verify(orderPersistencePort, never()).findByRestaurant(eq(RESTAURANT_ID), any(), anyInt(), anyInt());
    }

    @Test
    void listRestaurantOrders_whenEmployeeHasNoRestaurant_throwsException() {
        when(authenticatedUserPort.getAuthenticatedRestaurantId()).thenReturn(null);

        assertThrows(EmployeeWithoutRestaurantException.class,
                () -> orderUseCase.listRestaurantOrders(null, 0, 10));
        verify(orderPersistencePort, never()).findByRestaurant(any(), any(), anyInt(), anyInt());
    }

    @ParameterizedTest
    @ValueSource(ints = {-1, -5})
    void listRestaurantOrders_withNegativePage_throwsException(int page) {
        assertThrows(InvalidPaginationException.class, () -> orderUseCase.listRestaurantOrders(null, page, 10));
        verify(authenticatedUserPort, never()).getAuthenticatedRestaurantId();
    }

    @ParameterizedTest
    @ValueSource(ints = {0, -1})
    void listRestaurantOrders_withNonPositiveSize_throwsException(int size) {
        assertThrows(InvalidPaginationException.class, () -> orderUseCase.listRestaurantOrders(null, 0, size));
    }

    @Test
    void listRestaurantOrders_withNullPagination_throwsException() {
        assertThrows(InvalidPaginationException.class, () -> orderUseCase.listRestaurantOrders(null, null, 10));
        assertThrows(InvalidPaginationException.class, () -> orderUseCase.listRestaurantOrders(null, 0, null));
    }
}
