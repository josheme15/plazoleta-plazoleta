package com.plazoleta.plazoleta.domain.usecase;

import com.plazoleta.plazoleta.domain.api.IOrderStatusServicePort;
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
import com.plazoleta.plazoleta.domain.model.OrderStatus;
import com.plazoleta.plazoleta.domain.spi.IAuthenticatedUserPort;
import com.plazoleta.plazoleta.domain.spi.IOrderPersistencePort;
import com.plazoleta.plazoleta.domain.spi.ISmsClientPort;
import com.plazoleta.plazoleta.domain.spi.ITraceClientPort;
import com.plazoleta.plazoleta.domain.spi.IUserClientPort;

import java.security.SecureRandom;

/**
 * Cambios de estado del pedido: asignarse (HU-13), listo (HU-14), entregado (HU-15) y cancelado (HU-16).
 * Cada cambio queda registrado en trazabilidad (HU-17).
 */
public class OrderStatusUseCase implements IOrderStatusServicePort {

    private static final int PIN_BOUND = 1_000_000;
    private static final String READY_MESSAGE = "Tu pedido #%d esta listo. Presenta este PIN para reclamarlo: %s";
    private static final SecureRandom RANDOM = new SecureRandom();

    private final IOrderPersistencePort orderPersistencePort;
    private final IAuthenticatedUserPort authenticatedUserPort;
    private final IUserClientPort userClientPort;
    private final ISmsClientPort smsClientPort;
    private final ITraceClientPort traceClientPort;

    public OrderStatusUseCase(IOrderPersistencePort orderPersistencePort,
                              IAuthenticatedUserPort authenticatedUserPort,
                              IUserClientPort userClientPort,
                              ISmsClientPort smsClientPort,
                              ITraceClientPort traceClientPort) {
        this.orderPersistencePort = orderPersistencePort;
        this.authenticatedUserPort = authenticatedUserPort;
        this.userClientPort = userClientPort;
        this.smsClientPort = smsClientPort;
        this.traceClientPort = traceClientPort;
    }

    @Override
    public Order assignOrder(Long orderId) {
        Order order = findEmployeeRestaurantOrder(orderId);
        if (order.getStatus() != OrderStatus.PENDIENTE) {
            throw new OrderNotPendingException();
        }

        // Asignarse significa que el campo empleado asignado tiene el id de quien se asigna.
        order.setEmployeeId(authenticatedUserPort.getAuthenticatedUserId());
        return changeStatus(order, OrderStatus.EN_PREPARACION);
    }

    @Override
    public Order markOrderReady(Long orderId) {
        Order order = findEmployeeRestaurantOrder(orderId);
        if (order.getStatus() != OrderStatus.EN_PREPARACION) {
            throw new OrderNotInPreparationException();
        }
        if (!authenticatedUserPort.getAuthenticatedUserId().equals(order.getEmployeeId())) {
            throw new NotAssignedEmployeeException();
        }
        String phone = userClientPort.findUserPhoneById(order.getClientId())
                .orElseThrow(ClientNotFoundException::new);

        // Primero se notifica: si el SMS falla, el pedido sigue EN_PREPARACION y se puede reintentar.
        String pin = generatePin();
        smsClientPort.sendSms(phone, String.format(READY_MESSAGE, order.getId(), pin));

        order.setPin(pin);
        return changeStatus(order, OrderStatus.LISTO);
    }

    @Override
    public Order deliverOrder(Long orderId, String pin) {
        if (pin == null || pin.isBlank()) {
            throw new RequiredFieldException("pin");
        }
        Order order = findEmployeeRestaurantOrder(orderId);
        // Solo LISTO -> ENTREGADO. Como ENTREGADO no es LISTO, un pedido entregado nunca vuelve a cambiar de estado.
        if (order.getStatus() != OrderStatus.LISTO) {
            throw new OrderNotReadyException();
        }
        if (!pin.equals(order.getPin())) {
            throw new InvalidPinException();
        }
        return changeStatus(order, OrderStatus.ENTREGADO);
    }

    @Override
    public Order cancelOrder(Long orderId) {
        if (orderId == null) {
            throw new RequiredFieldException("idPedido");
        }
        Order order = orderPersistencePort.findById(orderId).orElseThrow(OrderNotFoundException::new);
        // El cliente sale del token: solo puede cancelar sus propios pedidos.
        if (!authenticatedUserPort.getAuthenticatedUserId().equals(order.getClientId())) {
            throw new OrderFromAnotherClientException();
        }
        if (order.getStatus() != OrderStatus.PENDIENTE) {
            throw new OrderCannotBeCancelledException();
        }
        return changeStatus(order, OrderStatus.CANCELADO);
    }

    /**
     * Guarda el nuevo estado y registra el cambio en trazabilidad (HU-17). Si trazabilidad falla, la excepcion
     * deshace la transaccion y el pedido conserva su estado anterior.
     */
    private Order changeStatus(Order order, OrderStatus newStatus) {
        OrderStatus previousStatus = order.getStatus();
        order.setStatus(newStatus);
        Order updated = orderPersistencePort.updateOrder(order);
        traceClientPort.registerStatusChange(updated, previousStatus);
        return updated;
    }

    /**
     * Busca el pedido y verifica que sea del restaurante del empleado autenticado.
     */
    private Order findEmployeeRestaurantOrder(Long orderId) {
        if (orderId == null) {
            throw new RequiredFieldException("idPedido");
        }
        Long restaurantId = authenticatedUserPort.getAuthenticatedRestaurantId();
        if (restaurantId == null) {
            throw new EmployeeWithoutRestaurantException();
        }
        Order order = orderPersistencePort.findById(orderId).orElseThrow(OrderNotFoundException::new);
        if (!restaurantId.equals(order.getRestaurantId())) {
            throw new OrderFromAnotherRestaurantException();
        }
        return order;
    }

    /**
     * PIN de seguridad de 6 digitos (con ceros a la izquierda) para reclamar el pedido.
     */
    private static String generatePin() {
        return String.format("%06d", RANDOM.nextInt(PIN_BOUND));
    }
}
