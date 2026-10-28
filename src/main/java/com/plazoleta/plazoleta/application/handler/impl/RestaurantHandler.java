package com.plazoleta.plazoleta.application.handler.impl;

import com.plazoleta.plazoleta.application.dto.request.RestaurantRequestDto;
import com.plazoleta.plazoleta.application.dto.response.RestaurantResponseDto;
import com.plazoleta.plazoleta.application.handler.IRestaurantHandler;
import com.plazoleta.plazoleta.application.mapper.IRestaurantRequestMapper;
import com.plazoleta.plazoleta.application.mapper.IRestaurantResponseMapper;
import com.plazoleta.plazoleta.domain.api.IRestaurantServicePort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
public class RestaurantHandler implements IRestaurantHandler {

    private final IRestaurantServicePort restaurantServicePort;
    private final IRestaurantRequestMapper restaurantRequestMapper;
    private final IRestaurantResponseMapper restaurantResponseMapper;

    @Override
    public RestaurantResponseDto createRestaurant(RestaurantRequestDto restaurantRequestDto) {
        return restaurantResponseMapper.toResponse(
                restaurantServicePort.createRestaurant(restaurantRequestMapper.toRestaurant(restaurantRequestDto)));
    }
}
