package com.plazoleta.plazoleta.domain.spi;

import com.plazoleta.plazoleta.domain.model.Dish;
import com.plazoleta.plazoleta.domain.model.PageResult;

import java.util.Optional;

public interface IDishPersistencePort {

    Dish saveDish(Dish dish);

    Optional<Dish> findById(Long dishId);

    /**
     * @param categoryId categoria por la que filtrar, o {@code null} para traer todas.
     * @return la pagina solicitada de platos activos del restaurante, ordenados por nombre.
     */
    PageResult<Dish> findActiveByRestaurant(Long restaurantId, Long categoryId, int page, int size);
}
