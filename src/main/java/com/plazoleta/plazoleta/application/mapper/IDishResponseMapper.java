package com.plazoleta.plazoleta.application.mapper;

import com.plazoleta.plazoleta.application.dto.response.DishMenuItemResponseDto;
import com.plazoleta.plazoleta.application.dto.response.DishResponseDto;
import com.plazoleta.plazoleta.domain.model.Dish;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface IDishResponseMapper {

    DishResponseDto toResponse(Dish dish);

    DishMenuItemResponseDto toMenuItem(Dish dish);
}
