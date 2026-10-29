package com.plazoleta.plazoleta.domain.usecase;

import com.plazoleta.plazoleta.domain.api.IDishServicePort;
import com.plazoleta.plazoleta.domain.exception.CategoryNotFoundException;
import com.plazoleta.plazoleta.domain.exception.InvalidPriceException;
import com.plazoleta.plazoleta.domain.exception.NotRestaurantOwnerException;
import com.plazoleta.plazoleta.domain.exception.RequiredFieldException;
import com.plazoleta.plazoleta.domain.exception.RestaurantNotFoundException;
import com.plazoleta.plazoleta.domain.model.Dish;
import com.plazoleta.plazoleta.domain.model.Restaurant;
import com.plazoleta.plazoleta.domain.spi.IAuthenticatedUserPort;
import com.plazoleta.plazoleta.domain.spi.ICategoryPersistencePort;
import com.plazoleta.plazoleta.domain.spi.IDishPersistencePort;
import com.plazoleta.plazoleta.domain.spi.IRestaurantPersistencePort;

public class DishUseCase implements IDishServicePort {

    private final IDishPersistencePort dishPersistencePort;
    private final IRestaurantPersistencePort restaurantPersistencePort;
    private final ICategoryPersistencePort categoryPersistencePort;
    private final IAuthenticatedUserPort authenticatedUserPort;

    public DishUseCase(IDishPersistencePort dishPersistencePort,
                       IRestaurantPersistencePort restaurantPersistencePort,
                       ICategoryPersistencePort categoryPersistencePort,
                       IAuthenticatedUserPort authenticatedUserPort) {
        this.dishPersistencePort = dishPersistencePort;
        this.restaurantPersistencePort = restaurantPersistencePort;
        this.categoryPersistencePort = categoryPersistencePort;
        this.authenticatedUserPort = authenticatedUserPort;
    }

    @Override
    public Dish createDish(Dish dish) {
        validateRequiredFields(dish);
        if (dish.getPrice() <= 0) {
            throw new InvalidPriceException();
        }

        Restaurant restaurant = restaurantPersistencePort.findById(dish.getRestaurantId())
                .orElseThrow(RestaurantNotFoundException::new);
        if (!restaurant.getOwnerId().equals(authenticatedUserPort.getAuthenticatedUserId())) {
            throw new NotRestaurantOwnerException();
        }
        if (!categoryPersistencePort.existsById(dish.getCategoryId())) {
            throw new CategoryNotFoundException();
        }

        dish.setActive(true);
        return dishPersistencePort.saveDish(dish);
    }

    private void validateRequiredFields(Dish dish) {
        requireText(dish.getName(), "nombre");
        if (dish.getPrice() == null) {
            throw new RequiredFieldException("precio");
        }
        requireText(dish.getDescription(), "descripcion");
        requireText(dish.getUrlImage(), "urlImagen");
        if (dish.getCategoryId() == null) {
            throw new RequiredFieldException("categoria");
        }
        if (dish.getRestaurantId() == null) {
            throw new RequiredFieldException("idRestaurante");
        }
    }

    private void requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new RequiredFieldException(fieldName);
        }
    }
}
