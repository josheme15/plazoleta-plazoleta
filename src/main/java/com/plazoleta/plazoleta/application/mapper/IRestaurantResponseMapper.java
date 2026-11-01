package com.plazoleta.plazoleta.application.mapper;

import com.plazoleta.plazoleta.application.dto.response.RestaurantListItemResponseDto;
import com.plazoleta.plazoleta.application.dto.response.RestaurantResponseDto;
import com.plazoleta.plazoleta.domain.model.Restaurant;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface IRestaurantResponseMapper {

    RestaurantResponseDto toResponse(Restaurant restaurant);

    RestaurantListItemResponseDto toListItem(Restaurant restaurant);
}
