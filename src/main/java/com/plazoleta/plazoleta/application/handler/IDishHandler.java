package com.plazoleta.plazoleta.application.handler;

import com.plazoleta.plazoleta.application.dto.request.DishRequestDto;
import com.plazoleta.plazoleta.application.dto.request.DishStatusRequestDto;
import com.plazoleta.plazoleta.application.dto.request.DishUpdateRequestDto;
import com.plazoleta.plazoleta.application.dto.response.DishResponseDto;

public interface IDishHandler {

    DishResponseDto createDish(DishRequestDto dishRequestDto);

    DishResponseDto updateDish(Long dishId, DishUpdateRequestDto dishUpdateRequestDto);

    DishResponseDto changeDishStatus(Long dishId, DishStatusRequestDto dishStatusRequestDto);
}
