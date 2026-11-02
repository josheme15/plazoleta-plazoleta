package com.plazoleta.plazoleta.domain.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class Order {

    private Long id;
    private Long clientId;
    private Long restaurantId;
    private LocalDateTime date;
    private OrderStatus status;
    // Empleado que se asigno el pedido (HU-13).
    private Long employeeId;
    // PIN de seguridad que el cliente presenta para reclamar el pedido listo (HU-14).
    private String pin;
    private List<OrderDish> dishes = new ArrayList<>();

    public Order() {
    }

    public Order(Long id, Long clientId, Long restaurantId, LocalDateTime date, OrderStatus status,
                 Long employeeId, List<OrderDish> dishes) {
        this.id = id;
        this.clientId = clientId;
        this.restaurantId = restaurantId;
        this.date = date;
        this.status = status;
        this.employeeId = employeeId;
        this.dishes = dishes;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getClientId() { return clientId; }
    public void setClientId(Long clientId) { this.clientId = clientId; }

    public Long getRestaurantId() { return restaurantId; }
    public void setRestaurantId(Long restaurantId) { this.restaurantId = restaurantId; }

    public LocalDateTime getDate() { return date; }
    public void setDate(LocalDateTime date) { this.date = date; }

    public OrderStatus getStatus() { return status; }
    public void setStatus(OrderStatus status) { this.status = status; }

    public Long getEmployeeId() { return employeeId; }
    public void setEmployeeId(Long employeeId) { this.employeeId = employeeId; }

    public String getPin() { return pin; }
    public void setPin(String pin) { this.pin = pin; }

    public List<OrderDish> getDishes() { return dishes; }
    public void setDishes(List<OrderDish> dishes) { this.dishes = dishes; }
}
