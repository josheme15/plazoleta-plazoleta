package com.plazoleta.plazoleta.application.handler.impl;

import com.plazoleta.plazoleta.application.dto.request.DishRequestDto;
import com.plazoleta.plazoleta.application.dto.request.DishStatusRequestDto;
import com.plazoleta.plazoleta.application.dto.request.DishUpdateRequestDto;
import com.plazoleta.plazoleta.application.dto.response.DishResponseDto;
import com.plazoleta.plazoleta.application.handler.IDishHandler;
import com.plazoleta.plazoleta.application.mapper.IDishRequestMapper;
import com.plazoleta.plazoleta.application.mapper.IDishResponseMapper;
import com.plazoleta.plazoleta.domain.api.IDishServicePort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
public class DishHandler implements IDishHandler {

    private final IDishServicePort dishServicePort;
    private final IDishRequestMapper dishRequestMapper;
    private final IDishResponseMapper dishResponseMapper;

    @Override
    public DishResponseDto createDish(DishRequestDto dishRequestDto) {
        return dishResponseMapper.toResponse(dishServicePort.createDish(dishRequestMapper.toDish(dishRequestDto)));
    }

    @Override
    public DishResponseDto updateDish(Long dishId, DishUpdateRequestDto dishUpdateRequestDto) {
        return dishResponseMapper.toResponse(
                dishServicePort.updateDish(dishId, dishRequestMapper.toDish(dishUpdateRequestDto)));
    }

    @Override
    public DishResponseDto changeDishStatus(Long dishId, DishStatusRequestDto dishStatusRequestDto) {
        return dishResponseMapper.toResponse(
                dishServicePort.changeDishStatus(dishId, dishStatusRequestDto.getActive()));
    }
}
