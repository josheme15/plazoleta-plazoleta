package com.plazoleta.plazoleta.domain.usecase;

import com.plazoleta.plazoleta.domain.api.IOrderServicePort;
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
import com.plazoleta.plazoleta.domain.spi.IAuthenticatedUserPort;
import com.plazoleta.plazoleta.domain.spi.IDishPersistencePort;
import com.plazoleta.plazoleta.domain.spi.IOrderPersistencePort;
import com.plazoleta.plazoleta.domain.spi.IRestaurantPersistencePort;
import com.plazoleta.plazoleta.domain.spi.ISmsClientPort;
import com.plazoleta.plazoleta.domain.spi.IUserClientPort;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

public class OrderUseCase implements IOrderServicePort {

    private static final int PIN_BOUND = 1_000_000;
    private static final String READY_MESSAGE = "Tu pedido #%d esta listo. Presenta este PIN para reclamarlo: %s";
    private static final SecureRandom RANDOM = new SecureRandom();

    private final IOrderPersistencePort orderPersistencePort;
    private final IRestaurantPersistencePort restaurantPersistencePort;
    private final IDishPersistencePort dishPersistencePort;
    private final IAuthenticatedUserPort authenticatedUserPort;
    private final IUserClientPort userClientPort;
    private final ISmsClientPort smsClientPort;
    private final Clock clock;

    public OrderUseCase(IOrderPersistencePort orderPersistencePort,
                        IRestaurantPersistencePort restaurantPersistencePort,
                        IDishPersistencePort dishPersistencePort,
                        IAuthenticatedUserPort authenticatedUserPort,
                        IUserClientPort userClientPort,
                        ISmsClientPort smsClientPort,
                        Clock clock) {
        this.orderPersistencePort = orderPersistencePort;
        this.restaurantPersistencePort = restaurantPersistencePort;
        this.dishPersistencePort = dishPersistencePort;
        this.authenticatedUserPort = authenticatedUserPort;
        this.userClientPort = userClientPort;
        this.smsClientPort = smsClientPort;
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

    @Override
    public PageResult<Order> listRestaurantOrders(OrderStatus status, Integer page, Integer size) {
        if (page == null || size == null || page < 0 || size <= 0) {
            throw new InvalidPaginationException();
        }
        // El restaurante sale del token del empleado: solo puede ver los pedidos de su restaurante.
        return orderPersistencePort.findByRestaurant(employeeRestaurantId(), status, page, size);
    }

    @Override
    public Order assignOrder(Long orderId) {
        Order order = findEmployeeRestaurantOrder(orderId);
        if (order.getStatus() != OrderStatus.PENDIENTE) {
            throw new OrderNotPendingException();
        }

        // Asignarse significa que el campo empleado asignado tiene el id de quien se asigna.
        order.setEmployeeId(authenticatedUserPort.getAuthenticatedUserId());
        order.setStatus(OrderStatus.EN_PREPARACION);
        return orderPersistencePort.updateOrder(order);
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
        order.setStatus(OrderStatus.LISTO);
        return orderPersistencePort.updateOrder(order);
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

        order.setStatus(OrderStatus.ENTREGADO);
        return orderPersistencePort.updateOrder(order);
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

        order.setStatus(OrderStatus.CANCELADO);
        return orderPersistencePort.updateOrder(order);
    }

    /**
     * Busca el pedido y verifica que sea del restaurante del empleado autenticado.
     */
    private Order findEmployeeRestaurantOrder(Long orderId) {
        if (orderId == null) {
            throw new RequiredFieldException("idPedido");
        }
        Long restaurantId = employeeRestaurantId();
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

    private Long employeeRestaurantId() {
        Long restaurantId = authenticatedUserPort.getAuthenticatedRestaurantId();
        if (restaurantId == null) {
            throw new EmployeeWithoutRestaurantException();
        }
        return restaurantId;
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
