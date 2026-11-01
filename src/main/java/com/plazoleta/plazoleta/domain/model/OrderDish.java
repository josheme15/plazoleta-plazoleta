package com.plazoleta.plazoleta.domain.model;

/**
 * Un plato dentro de un pedido, con la cantidad pedida.
 */
public class OrderDish {

    private Long dishId;
    private Integer quantity;

    public OrderDish() {
    }

    public OrderDish(Long dishId, Integer quantity) {
        this.dishId = dishId;
        this.quantity = quantity;
    }

    public Long getDishId() { return dishId; }
    public void setDishId(Long dishId) { this.dishId = dishId; }

    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }
}
