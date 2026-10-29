package com.plazoleta.plazoleta.infrastructure.output.jpa.adapter;

import com.plazoleta.plazoleta.domain.spi.ICategoryPersistencePort;
import com.plazoleta.plazoleta.infrastructure.output.jpa.repository.ICategoryRepository;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class CategoryJpaAdapter implements ICategoryPersistencePort {

    private final ICategoryRepository categoryRepository;

    @Override
    public boolean existsById(Long categoryId) {
        return categoryRepository.existsById(categoryId);
    }
}
