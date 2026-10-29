package com.plazoleta.plazoleta.infrastructure.output.jpa.mapper;

import com.plazoleta.plazoleta.domain.model.Dish;
import com.plazoleta.plazoleta.infrastructure.output.jpa.entity.DishEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface IDishEntityMapper {

    // Las relaciones (categoria y restaurante) las asigna el adaptador con referencias JPA.
    @Mapping(target = "category", ignore = true)
    @Mapping(target = "restaurant", ignore = true)
    DishEntity toEntity(Dish dish);

    @Mapping(target = "categoryId", source = "category.id")
    @Mapping(target = "restaurantId", source = "restaurant.id")
    Dish toDish(DishEntity dishEntity);
}
