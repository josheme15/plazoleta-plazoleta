package com.plazoleta.plazoleta.application.handler;

import com.plazoleta.plazoleta.application.dto.request.DishRequestDto;
import com.plazoleta.plazoleta.application.dto.response.DishResponseDto;

public interface IDishHandler {

    DishResponseDto createDish(DishRequestDto dishRequestDto);
}
