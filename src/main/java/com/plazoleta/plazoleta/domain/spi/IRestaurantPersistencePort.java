package com.plazoleta.plazoleta.domain.spi;

import com.plazoleta.plazoleta.domain.model.PageResult;
import com.plazoleta.plazoleta.domain.model.Restaurant;

import java.util.Optional;

public interface IRestaurantPersistencePort {

    Restaurant saveRestaurant(Restaurant restaurant);

    boolean existsByNit(String nit);

    Optional<Restaurant> findById(Long restaurantId);

    /**
     * @return la pagina solicitada de restaurantes, ordenados alfabeticamente por nombre.
     */
    PageResult<Restaurant> findAllOrderedByName(int page, int size);
}
