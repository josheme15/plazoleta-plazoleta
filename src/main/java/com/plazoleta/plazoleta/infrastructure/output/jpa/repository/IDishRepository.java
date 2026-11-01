package com.plazoleta.plazoleta.infrastructure.output.jpa.repository;

import com.plazoleta.plazoleta.infrastructure.output.jpa.entity.DishEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IDishRepository extends JpaRepository<DishEntity, Long> {

    Page<DishEntity> findByRestaurant_IdAndActiveTrue(Long restaurantId, Pageable pageable);

    Page<DishEntity> findByRestaurant_IdAndCategory_IdAndActiveTrue(Long restaurantId, Long categoryId,
                                                                    Pageable pageable);
}
