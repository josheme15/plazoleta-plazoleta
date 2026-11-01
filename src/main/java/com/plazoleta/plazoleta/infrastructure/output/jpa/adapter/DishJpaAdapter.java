package com.plazoleta.plazoleta.infrastructure.output.jpa.adapter;

import com.plazoleta.plazoleta.domain.model.Dish;
import com.plazoleta.plazoleta.domain.model.PageResult;
import com.plazoleta.plazoleta.domain.spi.IDishPersistencePort;
import com.plazoleta.plazoleta.infrastructure.output.jpa.entity.DishEntity;
import com.plazoleta.plazoleta.infrastructure.output.jpa.mapper.IDishEntityMapper;
import com.plazoleta.plazoleta.infrastructure.output.jpa.repository.ICategoryRepository;
import com.plazoleta.plazoleta.infrastructure.output.jpa.repository.IDishRepository;
import com.plazoleta.plazoleta.infrastructure.output.jpa.repository.IRestaurantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import java.util.Optional;

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

    @Override
    public Optional<Dish> findById(Long dishId) {
        return dishRepository.findById(dishId).map(dishEntityMapper::toDish);
    }

    @Override
    public PageResult<Dish> findActiveByRestaurant(Long restaurantId, Long categoryId, int page, int size) {
        PageRequest pageRequest = PageRequest.of(page, size, Sort.by(Sort.Order.asc("name").ignoreCase()));
        Page<DishEntity> result = categoryId == null
                ? dishRepository.findByRestaurant_IdAndActiveTrue(restaurantId, pageRequest)
                : dishRepository.findByRestaurant_IdAndCategory_IdAndActiveTrue(restaurantId, categoryId, pageRequest);
        return new PageResult<>(
                result.getContent().stream().map(dishEntityMapper::toDish).toList(),
                result.getNumber(), result.getSize(), result.getTotalElements(), result.getTotalPages());
    }
}
