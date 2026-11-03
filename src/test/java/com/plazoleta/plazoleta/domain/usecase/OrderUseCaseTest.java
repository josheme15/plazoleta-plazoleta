package com.plazoleta.plazoleta.domain.usecase;

import com.plazoleta.plazoleta.domain.exception.ClientHasActiveOrderException;
import com.plazoleta.plazoleta.domain.exception.ClientNotFoundException;
import com.plazoleta.plazoleta.domain.exception.DishFromAnotherRestaurantException;
import com.plazoleta.plazoleta.domain.exception.DishNotAvailableException;
import com.plazoleta.plazoleta.domain.exception.DishNotFoundException;
import com.plazoleta.plazoleta.domain.exception.DuplicateDishInOrderException;
import com.plazoleta.plazoleta.domain.exception.EmployeeWithoutRestaurantException;
import com.plazoleta.plazoleta.domain.exception.InvalidPaginationException;
import com.plazoleta.plazoleta.domain.exception.InvalidPinException;
import com.plazoleta.plazoleta.domain.exception.InvalidQuantityException;
import com.plazoleta.plazoleta.domain.exception.NotAssignedEmployeeException;
import com.plazoleta.plazoleta.domain.exception.OrderCannotBeCancelledException;
import com.plazoleta.plazoleta.domain.exception.OrderFromAnotherClientException;
import com.plazoleta.plazoleta.domain.exception.OrderFromAnotherRestaurantException;
import com.plazoleta.plazoleta.domain.exception.OrderNotFoundException;
import com.plazoleta.plazoleta.domain.exception.OrderNotInPreparationException;
import com.plazoleta.plazoleta.domain.exception.OrderNotPendingException;
import com.plazoleta.plazoleta.domain.exception.OrderNotReadyException;
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
import com.plazoleta.plazoleta.domain.spi.ISmsClientPort;
import com.plazoleta.plazoleta.domain.spi.IUserClientPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
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

    @Mock
    private IUserClientPort userClientPort;

    @Mock
    private ISmsClientPort smsClientPort;

    private OrderUseCase orderUseCase;

    @BeforeEach
    void setUp() {
        Clock fixedClock = Clock.fixed(NOW.toInstant(ZoneOffset.UTC), ZoneOffset.UTC);
        orderUseCase = new OrderUseCase(orderPersistencePort, restaurantPersistencePort, dishPersistencePort,
                authenticatedUserPort, userClientPort, smsClientPort, fixedClock);
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

    // ---------- HU-13: asignarse un pedido (pasa a EN_PREPARACION) ----------

    private static final Long ORDER_ID = 100L;
    private static final Long EMPLOYEE_ID = 8L;

    private Order storedOrder(Long restaurantId, OrderStatus status) {
        return new Order(ORDER_ID, CLIENT_ID, restaurantId, NOW, status, null,
                new ArrayList<>(List.of(new OrderDish(1L, 2))));
    }

    @Test
    void assignOrder_pendingOrderOfItsRestaurant_assignsEmployeeAndMovesToInPreparation() {
        when(authenticatedUserPort.getAuthenticatedRestaurantId()).thenReturn(RESTAURANT_ID);
        when(authenticatedUserPort.getAuthenticatedUserId()).thenReturn(EMPLOYEE_ID);
        when(orderPersistencePort.findById(ORDER_ID))
                .thenReturn(Optional.of(storedOrder(RESTAURANT_ID, OrderStatus.PENDIENTE)));
        when(orderPersistencePort.updateOrder(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        Order result = orderUseCase.assignOrder(ORDER_ID);

        ArgumentCaptor<Order> captor = ArgumentCaptor.forClass(Order.class);
        verify(orderPersistencePort).updateOrder(captor.capture());
        assertEquals(EMPLOYEE_ID, captor.getValue().getEmployeeId());
        assertEquals(OrderStatus.EN_PREPARACION, captor.getValue().getStatus());
        assertEquals(CLIENT_ID, captor.getValue().getClientId());
        assertSame(captor.getValue(), result);
    }

    @Test
    void assignOrder_whenOrderDoesNotExist_throwsException() {
        when(authenticatedUserPort.getAuthenticatedRestaurantId()).thenReturn(RESTAURANT_ID);
        when(orderPersistencePort.findById(ORDER_ID)).thenReturn(Optional.empty());

        assertThrows(OrderNotFoundException.class, () -> orderUseCase.assignOrder(ORDER_ID));
        verify(orderPersistencePort, never()).updateOrder(any());
    }

    @Test
    void assignOrder_whenOrderIsFromAnotherRestaurant_throwsException() {
        when(authenticatedUserPort.getAuthenticatedRestaurantId()).thenReturn(RESTAURANT_ID);
        when(orderPersistencePort.findById(ORDER_ID))
                .thenReturn(Optional.of(storedOrder(OTHER_RESTAURANT_ID, OrderStatus.PENDIENTE)));

        assertThrows(OrderFromAnotherRestaurantException.class, () -> orderUseCase.assignOrder(ORDER_ID));
        verify(orderPersistencePort, never()).updateOrder(any());
    }

    @ParameterizedTest
    @EnumSource(value = OrderStatus.class, names = {"EN_PREPARACION", "LISTO", "ENTREGADO", "CANCELADO"})
    void assignOrder_whenOrderIsNotPending_throwsException(OrderStatus status) {
        when(authenticatedUserPort.getAuthenticatedRestaurantId()).thenReturn(RESTAURANT_ID);
        when(orderPersistencePort.findById(ORDER_ID)).thenReturn(Optional.of(storedOrder(RESTAURANT_ID, status)));

        assertThrows(OrderNotPendingException.class, () -> orderUseCase.assignOrder(ORDER_ID));
        verify(authenticatedUserPort, never()).getAuthenticatedUserId();
        verify(orderPersistencePort, never()).updateOrder(any());
    }

    @Test
    void assignOrder_whenEmployeeHasNoRestaurant_throwsException() {
        when(authenticatedUserPort.getAuthenticatedRestaurantId()).thenReturn(null);

        assertThrows(EmployeeWithoutRestaurantException.class, () -> orderUseCase.assignOrder(ORDER_ID));
        verify(orderPersistencePort, never()).findById(any());
    }

    @Test
    void assignOrder_withNullId_throwsException() {
        assertThrows(RequiredFieldException.class, () -> orderUseCase.assignOrder(null));
        verify(orderPersistencePort, never()).findById(any());
    }

    // ---------- HU-14: pedido listo (pasa a LISTO y se notifica al cliente con un PIN) ----------

    private static final String CLIENT_PHONE = "+573005698325";

    private Order orderInPreparation(Long employeeId) {
        Order order = storedOrder(RESTAURANT_ID, OrderStatus.EN_PREPARACION);
        order.setEmployeeId(employeeId);
        return order;
    }

    private void givenAssignedEmployeeWithOrderInPreparation() {
        when(authenticatedUserPort.getAuthenticatedRestaurantId()).thenReturn(RESTAURANT_ID);
        when(authenticatedUserPort.getAuthenticatedUserId()).thenReturn(EMPLOYEE_ID);
        when(orderPersistencePort.findById(ORDER_ID)).thenAnswer(inv -> Optional.of(orderInPreparation(EMPLOYEE_ID)));
    }

    @Test
    void markOrderReady_assignedEmployee_sendsPinBySmsAndMovesToReady() {
        givenAssignedEmployeeWithOrderInPreparation();
        when(userClientPort.findUserPhoneById(CLIENT_ID)).thenReturn(Optional.of(CLIENT_PHONE));
        when(orderPersistencePort.updateOrder(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        Order result = orderUseCase.markOrderReady(ORDER_ID);

        ArgumentCaptor<Order> orderCaptor = ArgumentCaptor.forClass(Order.class);
        verify(orderPersistencePort).updateOrder(orderCaptor.capture());
        Order saved = orderCaptor.getValue();
        assertEquals(OrderStatus.LISTO, saved.getStatus());
        assertEquals(EMPLOYEE_ID, saved.getEmployeeId());
        assertTrue(saved.getPin().matches("\\d{6}"));

        ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);
        verify(smsClientPort).sendSms(eq(CLIENT_PHONE), messageCaptor.capture());
        assertEquals("Tu pedido #" + ORDER_ID + " esta listo. Presenta este PIN para reclamarlo: " + saved.getPin(),
                messageCaptor.getValue());
        assertSame(saved, result);
    }

    @Test
    void markOrderReady_generatesRandomPins() {
        givenAssignedEmployeeWithOrderInPreparation();
        when(userClientPort.findUserPhoneById(CLIENT_ID)).thenReturn(Optional.of(CLIENT_PHONE));
        when(orderPersistencePort.updateOrder(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        Set<String> pins = new HashSet<>();
        for (int i = 0; i < 20; i++) {
            pins.add(orderUseCase.markOrderReady(ORDER_ID).getPin());
        }

        assertTrue(pins.size() > 1);
    }

    @Test
    void markOrderReady_whenSmsFails_orderIsNotUpdated() {
        givenAssignedEmployeeWithOrderInPreparation();
        when(userClientPort.findUserPhoneById(CLIENT_ID)).thenReturn(Optional.of(CLIENT_PHONE));
        doThrow(new IllegalStateException("mensajeria caida")).when(smsClientPort).sendSms(anyString(), anyString());

        assertThrows(IllegalStateException.class, () -> orderUseCase.markOrderReady(ORDER_ID));
        verify(orderPersistencePort, never()).updateOrder(any());
    }

    @Test
    void markOrderReady_whenClientDoesNotExist_throwsException() {
        givenAssignedEmployeeWithOrderInPreparation();
        when(userClientPort.findUserPhoneById(CLIENT_ID)).thenReturn(Optional.empty());

        assertThrows(ClientNotFoundException.class, () -> orderUseCase.markOrderReady(ORDER_ID));
        verify(smsClientPort, never()).sendSms(anyString(), anyString());
        verify(orderPersistencePort, never()).updateOrder(any());
    }

    @Test
    void markOrderReady_whenEmployeeIsNotTheAssignedOne_throwsException() {
        when(authenticatedUserPort.getAuthenticatedRestaurantId()).thenReturn(RESTAURANT_ID);
        when(authenticatedUserPort.getAuthenticatedUserId()).thenReturn(EMPLOYEE_ID);
        when(orderPersistencePort.findById(ORDER_ID)).thenReturn(Optional.of(orderInPreparation(99L)));

        assertThrows(NotAssignedEmployeeException.class, () -> orderUseCase.markOrderReady(ORDER_ID));
        verify(userClientPort, never()).findUserPhoneById(any());
        verify(smsClientPort, never()).sendSms(anyString(), anyString());
        verify(orderPersistencePort, never()).updateOrder(any());
    }

    @ParameterizedTest
    @EnumSource(value = OrderStatus.class, names = {"PENDIENTE", "LISTO", "ENTREGADO", "CANCELADO"})
    void markOrderReady_whenOrderIsNotInPreparation_throwsException(OrderStatus status) {
        when(authenticatedUserPort.getAuthenticatedRestaurantId()).thenReturn(RESTAURANT_ID);
        when(orderPersistencePort.findById(ORDER_ID)).thenReturn(Optional.of(storedOrder(RESTAURANT_ID, status)));

        assertThrows(OrderNotInPreparationException.class, () -> orderUseCase.markOrderReady(ORDER_ID));
        verify(smsClientPort, never()).sendSms(anyString(), anyString());
        verify(orderPersistencePort, never()).updateOrder(any());
    }

    @Test
    void markOrderReady_whenOrderIsFromAnotherRestaurant_throwsException() {
        when(authenticatedUserPort.getAuthenticatedRestaurantId()).thenReturn(RESTAURANT_ID);
        when(orderPersistencePort.findById(ORDER_ID))
                .thenReturn(Optional.of(storedOrder(OTHER_RESTAURANT_ID, OrderStatus.EN_PREPARACION)));

        assertThrows(OrderFromAnotherRestaurantException.class, () -> orderUseCase.markOrderReady(ORDER_ID));
        verify(smsClientPort, never()).sendSms(anyString(), anyString());
    }

    @Test
    void markOrderReady_whenOrderDoesNotExist_throwsException() {
        when(authenticatedUserPort.getAuthenticatedRestaurantId()).thenReturn(RESTAURANT_ID);
        when(orderPersistencePort.findById(ORDER_ID)).thenReturn(Optional.empty());

        assertThrows(OrderNotFoundException.class, () -> orderUseCase.markOrderReady(ORDER_ID));
    }

    @Test
    void markOrderReady_whenEmployeeHasNoRestaurant_throwsException() {
        when(authenticatedUserPort.getAuthenticatedRestaurantId()).thenReturn(null);

        assertThrows(EmployeeWithoutRestaurantException.class, () -> orderUseCase.markOrderReady(ORDER_ID));
        verify(orderPersistencePort, never()).findById(any());
    }

    @Test
    void markOrderReady_withNullId_throwsException() {
        assertThrows(RequiredFieldException.class, () -> orderUseCase.markOrderReady(null));
        verify(orderPersistencePort, never()).findById(any());
    }

    // ---------- HU-15: pedido entregado (LISTO -> ENTREGADO validando el PIN) ----------

    private static final String PIN = "304930";

    private Order readyOrder(Long restaurantId) {
        Order order = storedOrder(restaurantId, OrderStatus.LISTO);
        order.setEmployeeId(EMPLOYEE_ID);
        order.setPin(PIN);
        return order;
    }

    @Test
    void deliverOrder_readyOrderWithCorrectPin_movesToDelivered() {
        when(authenticatedUserPort.getAuthenticatedRestaurantId()).thenReturn(RESTAURANT_ID);
        when(orderPersistencePort.findById(ORDER_ID)).thenReturn(Optional.of(readyOrder(RESTAURANT_ID)));
        when(orderPersistencePort.updateOrder(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        Order result = orderUseCase.deliverOrder(ORDER_ID, PIN);

        ArgumentCaptor<Order> captor = ArgumentCaptor.forClass(Order.class);
        verify(orderPersistencePort).updateOrder(captor.capture());
        assertEquals(OrderStatus.ENTREGADO, captor.getValue().getStatus());
        assertEquals(EMPLOYEE_ID, captor.getValue().getEmployeeId());
        assertSame(captor.getValue(), result);
    }

    @Test
    void deliverOrder_withWrongPin_throwsExceptionAndKeepsOrderReady() {
        when(authenticatedUserPort.getAuthenticatedRestaurantId()).thenReturn(RESTAURANT_ID);
        Order order = readyOrder(RESTAURANT_ID);
        when(orderPersistencePort.findById(ORDER_ID)).thenReturn(Optional.of(order));

        assertThrows(InvalidPinException.class, () -> orderUseCase.deliverOrder(ORDER_ID, "000000"));
        assertEquals(OrderStatus.LISTO, order.getStatus());
        verify(orderPersistencePort, never()).updateOrder(any());
    }

    @ParameterizedTest
    @EnumSource(value = OrderStatus.class, names = {"PENDIENTE", "EN_PREPARACION", "ENTREGADO", "CANCELADO"})
    void deliverOrder_whenOrderIsNotReady_throwsException(OrderStatus status) {
        when(authenticatedUserPort.getAuthenticatedRestaurantId()).thenReturn(RESTAURANT_ID);
        Order order = storedOrder(RESTAURANT_ID, status);
        order.setPin(PIN);
        when(orderPersistencePort.findById(ORDER_ID)).thenReturn(Optional.of(order));

        assertThrows(OrderNotReadyException.class, () -> orderUseCase.deliverOrder(ORDER_ID, PIN));
        verify(orderPersistencePort, never()).updateOrder(any());
    }

    @Test
    void deliverOrder_whenOrderIsFromAnotherRestaurant_throwsException() {
        when(authenticatedUserPort.getAuthenticatedRestaurantId()).thenReturn(RESTAURANT_ID);
        when(orderPersistencePort.findById(ORDER_ID)).thenReturn(Optional.of(readyOrder(OTHER_RESTAURANT_ID)));

        assertThrows(OrderFromAnotherRestaurantException.class, () -> orderUseCase.deliverOrder(ORDER_ID, PIN));
        verify(orderPersistencePort, never()).updateOrder(any());
    }

    @Test
    void deliverOrder_whenOrderDoesNotExist_throwsException() {
        when(authenticatedUserPort.getAuthenticatedRestaurantId()).thenReturn(RESTAURANT_ID);
        when(orderPersistencePort.findById(ORDER_ID)).thenReturn(Optional.empty());

        assertThrows(OrderNotFoundException.class, () -> orderUseCase.deliverOrder(ORDER_ID, PIN));
    }

    @Test
    void deliverOrder_whenEmployeeHasNoRestaurant_throwsException() {
        when(authenticatedUserPort.getAuthenticatedRestaurantId()).thenReturn(null);

        assertThrows(EmployeeWithoutRestaurantException.class, () -> orderUseCase.deliverOrder(ORDER_ID, PIN));
        verify(orderPersistencePort, never()).findById(any());
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = "   ")
    void deliverOrder_withoutPin_throwsException(String pin) {
        assertThrows(RequiredFieldException.class, () -> orderUseCase.deliverOrder(ORDER_ID, pin));
        verify(orderPersistencePort, never()).findById(any());
    }

    @Test
    void deliverOrder_withNullId_throwsException() {
        assertThrows(RequiredFieldException.class, () -> orderUseCase.deliverOrder(null, PIN));
        verify(orderPersistencePort, never()).findById(any());
    }

    @Test
    void deliveredOrder_cannotBeAssignedOrMarkedReadyAgain() {
        when(authenticatedUserPort.getAuthenticatedRestaurantId()).thenReturn(RESTAURANT_ID);
        when(orderPersistencePort.findById(ORDER_ID))
                .thenReturn(Optional.of(storedOrder(RESTAURANT_ID, OrderStatus.ENTREGADO)));

        assertThrows(OrderNotPendingException.class, () -> orderUseCase.assignOrder(ORDER_ID));
        assertThrows(OrderNotInPreparationException.class, () -> orderUseCase.markOrderReady(ORDER_ID));
        assertThrows(OrderNotReadyException.class, () -> orderUseCase.deliverOrder(ORDER_ID, PIN));
        verify(orderPersistencePort, never()).updateOrder(any());
    }

    // ---------- HU-16: cancelar pedido (solo PENDIENTE, solo el cliente duenio) ----------

    @Test
    void cancelOrder_pendingOrderOfTheClient_movesToCancelled() {
        when(authenticatedUserPort.getAuthenticatedUserId()).thenReturn(CLIENT_ID);
        when(orderPersistencePort.findById(ORDER_ID))
                .thenReturn(Optional.of(storedOrder(RESTAURANT_ID, OrderStatus.PENDIENTE)));
        when(orderPersistencePort.updateOrder(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        Order result = orderUseCase.cancelOrder(ORDER_ID);

        ArgumentCaptor<Order> captor = ArgumentCaptor.forClass(Order.class);
        verify(orderPersistencePort).updateOrder(captor.capture());
        assertEquals(OrderStatus.CANCELADO, captor.getValue().getStatus());
        assertEquals(CLIENT_ID, captor.getValue().getClientId());
        assertSame(captor.getValue(), result);
    }

    @ParameterizedTest
    @EnumSource(value = OrderStatus.class, names = {"EN_PREPARACION", "LISTO", "ENTREGADO", "CANCELADO"})
    void cancelOrder_whenOrderIsNotPending_throwsExceptionWithHuMessage(OrderStatus status) {
        when(authenticatedUserPort.getAuthenticatedUserId()).thenReturn(CLIENT_ID);
        when(orderPersistencePort.findById(ORDER_ID)).thenReturn(Optional.of(storedOrder(RESTAURANT_ID, status)));

        OrderCannotBeCancelledException ex =
                assertThrows(OrderCannotBeCancelledException.class, () -> orderUseCase.cancelOrder(ORDER_ID));
        assertEquals("Lo sentimos, tu pedido ya está en preparación y no se puede cancelar", ex.getMessage());
        verify(orderPersistencePort, never()).updateOrder(any());
    }

    @Test
    void cancelOrder_whenOrderBelongsToAnotherClient_throwsException() {
        when(authenticatedUserPort.getAuthenticatedUserId()).thenReturn(99L);
        when(orderPersistencePort.findById(ORDER_ID))
                .thenReturn(Optional.of(storedOrder(RESTAURANT_ID, OrderStatus.PENDIENTE)));

        assertThrows(OrderFromAnotherClientException.class, () -> orderUseCase.cancelOrder(ORDER_ID));
        verify(orderPersistencePort, never()).updateOrder(any());
    }

    @Test
    void cancelOrder_whenOrderDoesNotExist_throwsException() {
        when(orderPersistencePort.findById(ORDER_ID)).thenReturn(Optional.empty());

        assertThrows(OrderNotFoundException.class, () -> orderUseCase.cancelOrder(ORDER_ID));
        verify(orderPersistencePort, never()).updateOrder(any());
    }

    @Test
    void cancelOrder_withNullId_throwsException() {
        assertThrows(RequiredFieldException.class, () -> orderUseCase.cancelOrder(null));
        verify(orderPersistencePort, never()).findById(any());
    }

    @Test
    void cancelledOrder_isNotInProcess_soClientCanOrderAgain() {
        assertTrue(OrderStatus.IN_PROCESS.contains(OrderStatus.PENDIENTE));
        assertFalse(OrderStatus.IN_PROCESS.contains(OrderStatus.CANCELADO));
    }

    @Test
    void cancelledOrder_cannotBeAssignedByEmployee() {
        when(authenticatedUserPort.getAuthenticatedRestaurantId()).thenReturn(RESTAURANT_ID);
        when(orderPersistencePort.findById(ORDER_ID))
                .thenReturn(Optional.of(storedOrder(RESTAURANT_ID, OrderStatus.CANCELADO)));

        assertThrows(OrderNotPendingException.class, () -> orderUseCase.assignOrder(ORDER_ID));
        verify(orderPersistencePort, never()).updateOrder(any());
    }
}
