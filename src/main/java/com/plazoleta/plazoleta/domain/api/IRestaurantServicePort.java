package com.plazoleta.plazoleta.domain.api;

import com.plazoleta.plazoleta.domain.model.Restaurant;

public interface IRestaurantServicePort {

    Restaurant createRestaurant(Restaurant restaurant);
}
