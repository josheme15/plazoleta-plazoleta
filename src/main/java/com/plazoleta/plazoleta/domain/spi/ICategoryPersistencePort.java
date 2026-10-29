package com.plazoleta.plazoleta.domain.spi;

public interface ICategoryPersistencePort {

    boolean existsById(Long categoryId);
}
