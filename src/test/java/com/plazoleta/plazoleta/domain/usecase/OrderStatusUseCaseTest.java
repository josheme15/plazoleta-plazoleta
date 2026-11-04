package com.plazoleta.plazoleta.domain.usecase;

import com.plazoleta.plazoleta.domain.exception.ClientNotFoundException;
import com.plazoleta.plazoleta.domain.exception.EmployeeWithoutRestaurantException;
import com.plazoleta.plazoleta.domain.exception.InvalidPinException;
import com.plazoleta.plazoleta.domain.exception.NotAssignedEmployeeException;
import com.plazoleta.plazoleta.domain.exception.OrderCannotBeCancelledException;
import com.plazoleta.plazoleta.domain.exception.OrderFromAnotherClientException;
import com.plazoleta.plazoleta.domain.exception.OrderFromAnotherRestaurantException;
import com.plazoleta.plazoleta.domain.exception.OrderNotFoundException;
import com.plazoleta.plazoleta.domain.exception.OrderNotInPreparationException;
import com.plazoleta.plazoleta.domain.exception.OrderNotPendingException;
import com.plazoleta.plazoleta.domain.exception.OrderNotReadyException;
import com.plazoleta.plazoleta.domain.exception.RequiredFieldException;
import com.plazoleta.plazoleta.domain.model.Order;
import com.plazoleta.plazoleta.domain.model.OrderDish;
import com.plazoleta.plazoleta.domain.model.OrderStatus;
import com.plazoleta.plazoleta.domain.spi.IAuthenticatedUserPort;
import com.plazoleta.plazoleta.domain.spi.IOrderPersistencePort;
import com.plazoleta.plazoleta.domain.spi.ITraceClientPort;
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

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderStatusUseCaseTest {

    private static final Long CLIENT_ID = 30L;
    private static final Long RESTAURANT_ID = 10L;
    private static final Long OTHER_RESTAURANT_ID = 11L;
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 11, 1, 12, 30);

    @Mock
    private IOrderPersistencePort orderPersistencePort;

    @Mock
    private IAuthenticatedUserPort authenticatedUserPort;

    @Mock
    private IUserClientPort userClientPort;

    @Mock
    private ISmsClientPort smsClientPort;

    @Mock
    private ITraceClientPort traceClientPort;

    private OrderStatusUseCase orderStatusUseCase;

    @BeforeEach
    void setUp() {
        orderStatusUseCase = new OrderStatusUseCase(orderPersistencePort, authenticatedUserPort, userClientPort,
                smsClientPort, traceClientPort);
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

        Order result = orderStatusUseCase.assignOrder(ORDER_ID);

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

        assertThrows(OrderNotFoundException.class, () -> orderStatusUseCase.assignOrder(ORDER_ID));
        verify(orderPersistencePort, never()).updateOrder(any());
    }

    @Test
    void assignOrder_whenOrderIsFromAnotherRestaurant_throwsException() {
        when(authenticatedUserPort.getAuthenticatedRestaurantId()).thenReturn(RESTAURANT_ID);
        when(orderPersistencePort.findById(ORDER_ID))
                .thenReturn(Optional.of(storedOrder(OTHER_RESTAURANT_ID, OrderStatus.PENDIENTE)));

        assertThrows(OrderFromAnotherRestaurantException.class, () -> orderStatusUseCase.assignOrder(ORDER_ID));
        verify(orderPersistencePort, never()).updateOrder(any());
    }

    @ParameterizedTest
    @EnumSource(value = OrderStatus.class, names = {"EN_PREPARACION", "LISTO", "ENTREGADO", "CANCELADO"})
    void assignOrder_whenOrderIsNotPending_throwsException(OrderStatus status) {
        when(authenticatedUserPort.getAuthenticatedRestaurantId()).thenReturn(RESTAURANT_ID);
        when(orderPersistencePort.findById(ORDER_ID)).thenReturn(Optional.of(storedOrder(RESTAURANT_ID, status)));

        assertThrows(OrderNotPendingException.class, () -> orderStatusUseCase.assignOrder(ORDER_ID));
        verify(authenticatedUserPort, never()).getAuthenticatedUserId();
        verify(orderPersistencePort, never()).updateOrder(any());
    }

    @Test
    void assignOrder_whenEmployeeHasNoRestaurant_throwsException() {
        when(authenticatedUserPort.getAuthenticatedRestaurantId()).thenReturn(null);

        assertThrows(EmployeeWithoutRestaurantException.class, () -> orderStatusUseCase.assignOrder(ORDER_ID));
        verify(orderPersistencePort, never()).findById(any());
    }

    @Test
    void assignOrder_withNullId_throwsException() {
        assertThrows(RequiredFieldException.class, () -> orderStatusUseCase.assignOrder(null));
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

        Order result = orderStatusUseCase.markOrderReady(ORDER_ID);

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
            pins.add(orderStatusUseCase.markOrderReady(ORDER_ID).getPin());
        }

        assertTrue(pins.size() > 1);
    }

    @Test
    void markOrderReady_whenSmsFails_orderIsNotUpdated() {
        givenAssignedEmployeeWithOrderInPreparation();
        when(userClientPort.findUserPhoneById(CLIENT_ID)).thenReturn(Optional.of(CLIENT_PHONE));
        doThrow(new IllegalStateException("mensajeria caida")).when(smsClientPort).sendSms(anyString(), anyString());

        assertThrows(IllegalStateException.class, () -> orderStatusUseCase.markOrderReady(ORDER_ID));
        verify(orderPersistencePort, never()).updateOrder(any());
    }

    @Test
    void markOrderReady_whenClientDoesNotExist_throwsException() {
        givenAssignedEmployeeWithOrderInPreparation();
        when(userClientPort.findUserPhoneById(CLIENT_ID)).thenReturn(Optional.empty());

        assertThrows(ClientNotFoundException.class, () -> orderStatusUseCase.markOrderReady(ORDER_ID));
        verify(smsClientPort, never()).sendSms(anyString(), anyString());
        verify(orderPersistencePort, never()).updateOrder(any());
    }

    @Test
    void markOrderReady_whenEmployeeIsNotTheAssignedOne_throwsException() {
        when(authenticatedUserPort.getAuthenticatedRestaurantId()).thenReturn(RESTAURANT_ID);
        when(authenticatedUserPort.getAuthenticatedUserId()).thenReturn(EMPLOYEE_ID);
        when(orderPersistencePort.findById(ORDER_ID)).thenReturn(Optional.of(orderInPreparation(99L)));

        assertThrows(NotAssignedEmployeeException.class, () -> orderStatusUseCase.markOrderReady(ORDER_ID));
        verify(userClientPort, never()).findUserPhoneById(any());
        verify(smsClientPort, never()).sendSms(anyString(), anyString());
        verify(orderPersistencePort, never()).updateOrder(any());
    }

    @ParameterizedTest
    @EnumSource(value = OrderStatus.class, names = {"PENDIENTE", "LISTO", "ENTREGADO", "CANCELADO"})
    void markOrderReady_whenOrderIsNotInPreparation_throwsException(OrderStatus status) {
        when(authenticatedUserPort.getAuthenticatedRestaurantId()).thenReturn(RESTAURANT_ID);
        when(orderPersistencePort.findById(ORDER_ID)).thenReturn(Optional.of(storedOrder(RESTAURANT_ID, status)));

        assertThrows(OrderNotInPreparationException.class, () -> orderStatusUseCase.markOrderReady(ORDER_ID));
        verify(smsClientPort, never()).sendSms(anyString(), anyString());
        verify(orderPersistencePort, never()).updateOrder(any());
    }

    @Test
    void markOrderReady_whenOrderIsFromAnotherRestaurant_throwsException() {
        when(authenticatedUserPort.getAuthenticatedRestaurantId()).thenReturn(RESTAURANT_ID);
        when(orderPersistencePort.findById(ORDER_ID))
                .thenReturn(Optional.of(storedOrder(OTHER_RESTAURANT_ID, OrderStatus.EN_PREPARACION)));

        assertThrows(OrderFromAnotherRestaurantException.class, () -> orderStatusUseCase.markOrderReady(ORDER_ID));
        verify(smsClientPort, never()).sendSms(anyString(), anyString());
    }

    @Test
    void markOrderReady_whenOrderDoesNotExist_throwsException() {
        when(authenticatedUserPort.getAuthenticatedRestaurantId()).thenReturn(RESTAURANT_ID);
        when(orderPersistencePort.findById(ORDER_ID)).thenReturn(Optional.empty());

        assertThrows(OrderNotFoundException.class, () -> orderStatusUseCase.markOrderReady(ORDER_ID));
    }

    @Test
    void markOrderReady_whenEmployeeHasNoRestaurant_throwsException() {
        when(authenticatedUserPort.getAuthenticatedRestaurantId()).thenReturn(null);

        assertThrows(EmployeeWithoutRestaurantException.class, () -> orderStatusUseCase.markOrderReady(ORDER_ID));
        verify(orderPersistencePort, never()).findById(any());
    }

    @Test
    void markOrderReady_withNullId_throwsException() {
        assertThrows(RequiredFieldException.class, () -> orderStatusUseCase.markOrderReady(null));
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

        Order result = orderStatusUseCase.deliverOrder(ORDER_ID, PIN);

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

        assertThrows(InvalidPinException.class, () -> orderStatusUseCase.deliverOrder(ORDER_ID, "000000"));
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

        assertThrows(OrderNotReadyException.class, () -> orderStatusUseCase.deliverOrder(ORDER_ID, PIN));
        verify(orderPersistencePort, never()).updateOrder(any());
    }

    @Test
    void deliverOrder_whenOrderIsFromAnotherRestaurant_throwsException() {
        when(authenticatedUserPort.getAuthenticatedRestaurantId()).thenReturn(RESTAURANT_ID);
        when(orderPersistencePort.findById(ORDER_ID)).thenReturn(Optional.of(readyOrder(OTHER_RESTAURANT_ID)));

        assertThrows(OrderFromAnotherRestaurantException.class, () -> orderStatusUseCase.deliverOrder(ORDER_ID, PIN));
        verify(orderPersistencePort, never()).updateOrder(any());
    }

    @Test
    void deliverOrder_whenOrderDoesNotExist_throwsException() {
        when(authenticatedUserPort.getAuthenticatedRestaurantId()).thenReturn(RESTAURANT_ID);
        when(orderPersistencePort.findById(ORDER_ID)).thenReturn(Optional.empty());

        assertThrows(OrderNotFoundException.class, () -> orderStatusUseCase.deliverOrder(ORDER_ID, PIN));
    }

    @Test
    void deliverOrder_whenEmployeeHasNoRestaurant_throwsException() {
        when(authenticatedUserPort.getAuthenticatedRestaurantId()).thenReturn(null);

        assertThrows(EmployeeWithoutRestaurantException.class, () -> orderStatusUseCase.deliverOrder(ORDER_ID, PIN));
        verify(orderPersistencePort, never()).findById(any());
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = "   ")
    void deliverOrder_withoutPin_throwsException(String pin) {
        assertThrows(RequiredFieldException.class, () -> orderStatusUseCase.deliverOrder(ORDER_ID, pin));
        verify(orderPersistencePort, never()).findById(any());
    }

    @Test
    void deliverOrder_withNullId_throwsException() {
        assertThrows(RequiredFieldException.class, () -> orderStatusUseCase.deliverOrder(null, PIN));
        verify(orderPersistencePort, never()).findById(any());
    }

    @Test
    void deliveredOrder_cannotBeAssignedOrMarkedReadyAgain() {
        when(authenticatedUserPort.getAuthenticatedRestaurantId()).thenReturn(RESTAURANT_ID);
        when(orderPersistencePort.findById(ORDER_ID))
                .thenReturn(Optional.of(storedOrder(RESTAURANT_ID, OrderStatus.ENTREGADO)));

        assertThrows(OrderNotPendingException.class, () -> orderStatusUseCase.assignOrder(ORDER_ID));
        assertThrows(OrderNotInPreparationException.class, () -> orderStatusUseCase.markOrderReady(ORDER_ID));
        assertThrows(OrderNotReadyException.class, () -> orderStatusUseCase.deliverOrder(ORDER_ID, PIN));
        verify(orderPersistencePort, never()).updateOrder(any());
    }

    // ---------- HU-16: cancelar pedido (solo PENDIENTE, solo el cliente duenio) ----------

    @Test
    void cancelOrder_pendingOrderOfTheClient_movesToCancelled() {
        when(authenticatedUserPort.getAuthenticatedUserId()).thenReturn(CLIENT_ID);
        when(orderPersistencePort.findById(ORDER_ID))
                .thenReturn(Optional.of(storedOrder(RESTAURANT_ID, OrderStatus.PENDIENTE)));
        when(orderPersistencePort.updateOrder(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        Order result = orderStatusUseCase.cancelOrder(ORDER_ID);

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
                assertThrows(OrderCannotBeCancelledException.class, () -> orderStatusUseCase.cancelOrder(ORDER_ID));
        assertEquals("Lo sentimos, tu pedido ya está en preparación y no se puede cancelar", ex.getMessage());
        verify(orderPersistencePort, never()).updateOrder(any());
    }

    @Test
    void cancelOrder_whenOrderBelongsToAnotherClient_throwsException() {
        when(authenticatedUserPort.getAuthenticatedUserId()).thenReturn(99L);
        when(orderPersistencePort.findById(ORDER_ID))
                .thenReturn(Optional.of(storedOrder(RESTAURANT_ID, OrderStatus.PENDIENTE)));

        assertThrows(OrderFromAnotherClientException.class, () -> orderStatusUseCase.cancelOrder(ORDER_ID));
        verify(orderPersistencePort, never()).updateOrder(any());
    }

    @Test
    void cancelOrder_whenOrderDoesNotExist_throwsException() {
        when(orderPersistencePort.findById(ORDER_ID)).thenReturn(Optional.empty());

        assertThrows(OrderNotFoundException.class, () -> orderStatusUseCase.cancelOrder(ORDER_ID));
        verify(orderPersistencePort, never()).updateOrder(any());
    }

    @Test
    void cancelOrder_withNullId_throwsException() {
        assertThrows(RequiredFieldException.class, () -> orderStatusUseCase.cancelOrder(null));
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

        assertThrows(OrderNotPendingException.class, () -> orderStatusUseCase.assignOrder(ORDER_ID));
        verify(orderPersistencePort, never()).updateOrder(any());
    }

    // ---------- HU-17: cada cambio de estado queda registrado en trazabilidad ----------

    @Test
    void assignOrder_registersTraceFromPendingToInPreparation() {
        when(authenticatedUserPort.getAuthenticatedRestaurantId()).thenReturn(RESTAURANT_ID);
        when(authenticatedUserPort.getAuthenticatedUserId()).thenReturn(EMPLOYEE_ID);
        when(orderPersistencePort.findById(ORDER_ID))
                .thenReturn(Optional.of(storedOrder(RESTAURANT_ID, OrderStatus.PENDIENTE)));
        when(orderPersistencePort.updateOrder(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        Order result = orderStatusUseCase.assignOrder(ORDER_ID);

        verify(traceClientPort).registerStatusChange(result, OrderStatus.PENDIENTE);
        assertEquals(OrderStatus.EN_PREPARACION, result.getStatus());
    }

    @Test
    void markOrderReady_registersTraceFromInPreparationToReady() {
        givenAssignedEmployeeWithOrderInPreparation();
        when(userClientPort.findUserPhoneById(CLIENT_ID)).thenReturn(Optional.of(CLIENT_PHONE));
        when(orderPersistencePort.updateOrder(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        Order result = orderStatusUseCase.markOrderReady(ORDER_ID);

        verify(traceClientPort).registerStatusChange(result, OrderStatus.EN_PREPARACION);
        assertEquals(OrderStatus.LISTO, result.getStatus());
    }

    @Test
    void deliverOrder_registersTraceFromReadyToDelivered() {
        when(authenticatedUserPort.getAuthenticatedRestaurantId()).thenReturn(RESTAURANT_ID);
        when(orderPersistencePort.findById(ORDER_ID)).thenReturn(Optional.of(readyOrder(RESTAURANT_ID)));
        when(orderPersistencePort.updateOrder(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        Order result = orderStatusUseCase.deliverOrder(ORDER_ID, PIN);

        verify(traceClientPort).registerStatusChange(result, OrderStatus.LISTO);
        assertEquals(OrderStatus.ENTREGADO, result.getStatus());
    }

    @Test
    void cancelOrder_registersTraceFromPendingToCancelled() {
        when(authenticatedUserPort.getAuthenticatedUserId()).thenReturn(CLIENT_ID);
        when(orderPersistencePort.findById(ORDER_ID))
                .thenReturn(Optional.of(storedOrder(RESTAURANT_ID, OrderStatus.PENDIENTE)));
        when(orderPersistencePort.updateOrder(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        Order result = orderStatusUseCase.cancelOrder(ORDER_ID);

        verify(traceClientPort).registerStatusChange(result, OrderStatus.PENDIENTE);
        assertEquals(OrderStatus.CANCELADO, result.getStatus());
    }

    @Test
    void rejectedStatusChange_doesNotRegisterTrace() {
        when(authenticatedUserPort.getAuthenticatedRestaurantId()).thenReturn(RESTAURANT_ID);
        when(orderPersistencePort.findById(ORDER_ID))
                .thenReturn(Optional.of(storedOrder(RESTAURANT_ID, OrderStatus.ENTREGADO)));

        assertThrows(OrderNotPendingException.class, () -> orderStatusUseCase.assignOrder(ORDER_ID));
        verify(traceClientPort, never()).registerStatusChange(any(), any());
    }

    @Test
    void statusChange_whenTraceFails_propagatesTheError() {
        when(authenticatedUserPort.getAuthenticatedUserId()).thenReturn(CLIENT_ID);
        when(orderPersistencePort.findById(ORDER_ID))
                .thenReturn(Optional.of(storedOrder(RESTAURANT_ID, OrderStatus.PENDIENTE)));
        when(orderPersistencePort.updateOrder(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));
        doThrow(new IllegalStateException("trazabilidad caida")).when(traceClientPort).registerStatusChange(any(), any());

        // La excepcion hace que el handler (@Transactional) deshaga el cambio de estado.
        assertThrows(IllegalStateException.class, () -> orderStatusUseCase.cancelOrder(ORDER_ID));
    }
}
