package com.plazoleta.plazoleta.application.handler;

import com.plazoleta.plazoleta.application.dto.request.RestaurantRequestDto;
import com.plazoleta.plazoleta.application.dto.response.PageResponseDto;
import com.plazoleta.plazoleta.application.dto.response.RestaurantListItemResponseDto;
import com.plazoleta.plazoleta.application.dto.response.RestaurantResponseDto;

public interface IRestaurantHandler {

    RestaurantResponseDto createRestaurant(RestaurantRequestDto restaurantRequestDto);

    RestaurantResponseDto getRestaurantById(Long restaurantId);

    PageResponseDto<RestaurantListItemResponseDto> listRestaurants(Integer page, Integer size);
}
