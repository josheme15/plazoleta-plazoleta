package com.plazoleta.plazoleta.infrastructure.output.jpa.adapter;

import com.plazoleta.plazoleta.domain.model.Dish;
import com.plazoleta.plazoleta.domain.spi.IDishPersistencePort;
import com.plazoleta.plazoleta.infrastructure.output.jpa.entity.DishEntity;
import com.plazoleta.plazoleta.infrastructure.output.jpa.mapper.IDishEntityMapper;
import com.plazoleta.plazoleta.infrastructure.output.jpa.repository.ICategoryRepository;
import com.plazoleta.plazoleta.infrastructure.output.jpa.repository.IDishRepository;
import com.plazoleta.plazoleta.infrastructure.output.jpa.repository.IRestaurantRepository;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class DishJpaAdapter implements IDishPersistencePort {

    private final IDishRepository dishRepository;
    private final IRestaurantRepository restaurantRepository;
    private final ICategoryRepository categoryRepository;
    private final IDishEntityMapper dishEntityMapper;

    @Override
    public Dish saveDish(Dish dish) {
        DishEntity entity = dishEntityMapper.toEntity(dish);
        entity.setRestaurant(restaurantRepository.getReferenceById(dish.getRestaurantId()));
        entity.setCategory(categoryRepository.getReferenceById(dish.getCategoryId()));
        return dishEntityMapper.toDish(dishRepository.save(entity));
    }
}
