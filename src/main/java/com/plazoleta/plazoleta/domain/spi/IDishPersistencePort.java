package com.plazoleta.plazoleta.domain.spi;

import com.plazoleta.plazoleta.domain.model.Dish;

public interface IDishPersistencePort {

    Dish saveDish(Dish dish);
}
