package com.plazoleta.plazoleta.domain.api;

import com.plazoleta.plazoleta.domain.model.PageResult;
import com.plazoleta.plazoleta.domain.model.Restaurant;

public interface IRestaurantServicePort {

    Restaurant createRestaurant(Restaurant restaurant);

    Restaurant getRestaurantById(Long restaurantId);

    /**
     * Lista los restaurantes en orden alfabetico, paginados.
     *
     * @param page numero de pagina (desde 0)
     * @param size cantidad de restaurantes por pagina
     */
    PageResult<Restaurant> listRestaurants(Integer page, Integer size);
}
