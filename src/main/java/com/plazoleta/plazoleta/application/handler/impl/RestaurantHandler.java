package com.plazoleta.plazoleta.application.handler.impl;

import com.plazoleta.plazoleta.application.dto.request.RestaurantRequestDto;
import com.plazoleta.plazoleta.application.dto.response.PageResponseDto;
import com.plazoleta.plazoleta.application.dto.response.RestaurantListItemResponseDto;
import com.plazoleta.plazoleta.application.dto.response.RestaurantResponseDto;
import com.plazoleta.plazoleta.application.handler.IRestaurantHandler;
import com.plazoleta.plazoleta.application.mapper.IRestaurantRequestMapper;
import com.plazoleta.plazoleta.application.mapper.IRestaurantResponseMapper;
import com.plazoleta.plazoleta.domain.api.IRestaurantServicePort;
import com.plazoleta.plazoleta.domain.model.PageResult;
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

    @Override
    @Transactional(readOnly = true)
    public RestaurantResponseDto getRestaurantById(Long restaurantId) {
        return restaurantResponseMapper.toResponse(restaurantServicePort.getRestaurantById(restaurantId));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponseDto<RestaurantListItemResponseDto> listRestaurants(Integer page, Integer size) {
        PageResult<RestaurantListItemResponseDto> result =
                restaurantServicePort.listRestaurants(page, size).map(restaurantResponseMapper::toListItem);
        return new PageResponseDto<>(result.getContent(), result.getPage(), result.getSize(),
                result.getTotalElements(), result.getTotalPages());
    }
}
