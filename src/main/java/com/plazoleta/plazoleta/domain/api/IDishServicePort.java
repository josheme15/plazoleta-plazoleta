package com.plazoleta.plazoleta.domain.api;

import com.plazoleta.plazoleta.domain.model.Dish;

public interface IDishServicePort {

    Dish createDish(Dish dish);

    /**
     * Actualiza solo el precio y/o la descripcion del plato; el resto de campos de {@code changes} se ignora.
     */
    Dish updateDish(Long dishId, Dish changes);

    /**
     * Activa ({@code true}) o desactiva ({@code false}) un plato del restaurante del propietario autenticado.
     */
    Dish changeDishStatus(Long dishId, Boolean active);
}
