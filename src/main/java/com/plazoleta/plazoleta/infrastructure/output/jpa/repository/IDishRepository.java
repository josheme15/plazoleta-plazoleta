package com.plazoleta.plazoleta.infrastructure.output.jpa.repository;

import com.plazoleta.plazoleta.infrastructure.output.jpa.entity.DishEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IDishRepository extends JpaRepository<DishEntity, Long> {
}
