package com.plazoleta.plazoleta.application.handler;

import com.plazoleta.plazoleta.application.dto.request.DishRequestDto;
import com.plazoleta.plazoleta.application.dto.request.DishStatusRequestDto;
import com.plazoleta.plazoleta.application.dto.request.DishUpdateRequestDto;
import com.plazoleta.plazoleta.application.dto.response.DishMenuItemResponseDto;
import com.plazoleta.plazoleta.application.dto.response.DishResponseDto;
import com.plazoleta.plazoleta.application.dto.response.PageResponseDto;

public interface IDishHandler {

    DishResponseDto createDish(DishRequestDto dishRequestDto);

    DishResponseDto updateDish(Long dishId, DishUpdateRequestDto dishUpdateRequestDto);

    DishResponseDto changeDishStatus(Long dishId, DishStatusRequestDto dishStatusRequestDto);

    PageResponseDto<DishMenuItemResponseDto> listDishesByRestaurant(Long restaurantId, Long categoryId,
                                                                    Integer page, Integer size);
}
