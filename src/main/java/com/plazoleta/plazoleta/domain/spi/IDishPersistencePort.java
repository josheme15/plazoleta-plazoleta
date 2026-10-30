package com.plazoleta.plazoleta.domain.spi;

import com.plazoleta.plazoleta.domain.model.Dish;

import java.util.Optional;

public interface IDishPersistencePort {

    Dish saveDish(Dish dish);

    Optional<Dish> findById(Long dishId);
}
