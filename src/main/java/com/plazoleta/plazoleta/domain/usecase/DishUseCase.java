package com.plazoleta.plazoleta.domain.usecase;

import com.plazoleta.plazoleta.domain.api.IDishServicePort;
import com.plazoleta.plazoleta.domain.exception.CategoryNotFoundException;
import com.plazoleta.plazoleta.domain.exception.DishNotFoundException;
import com.plazoleta.plazoleta.domain.exception.InvalidDescriptionException;
import com.plazoleta.plazoleta.domain.exception.InvalidPriceException;
import com.plazoleta.plazoleta.domain.exception.NoDishFieldsToUpdateException;
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

        validateRestaurantOwner(dish.getRestaurantId());
        if (!categoryPersistencePort.existsById(dish.getCategoryId())) {
            throw new CategoryNotFoundException();
        }

        dish.setActive(true);
        return dishPersistencePort.saveDish(dish);
    }

    @Override
    public Dish updateDish(Long dishId, Dish changes) {
        if (dishId == null) {
            throw new RequiredFieldException("idPlato");
        }
        Integer newPrice = changes.getPrice();
        String newDescription = changes.getDescription();
        if (newPrice == null && newDescription == null) {
            throw new NoDishFieldsToUpdateException();
        }
        if (newPrice != null && newPrice <= 0) {
            throw new InvalidPriceException();
        }
        if (newDescription != null && newDescription.isBlank()) {
            throw new InvalidDescriptionException();
        }

        Dish dish = dishPersistencePort.findById(dishId).orElseThrow(DishNotFoundException::new);
        validateRestaurantOwner(dish.getRestaurantId());

        // Solo se pueden editar el precio y la descripcion.
        if (newPrice != null) {
            dish.setPrice(newPrice);
        }
        if (newDescription != null) {
            dish.setDescription(newDescription);
        }
        return dishPersistencePort.saveDish(dish);
    }

    @Override
    public Dish changeDishStatus(Long dishId, Boolean active) {
        if (dishId == null) {
            throw new RequiredFieldException("idPlato");
        }
        if (active == null) {
            throw new RequiredFieldException("activo");
        }

        Dish dish = dishPersistencePort.findById(dishId).orElseThrow(DishNotFoundException::new);
        // No se permite modificar platos de restaurantes de otro propietario.
        validateRestaurantOwner(dish.getRestaurantId());

        dish.setActive(active);
        return dishPersistencePort.saveDish(dish);
    }

    private void validateRestaurantOwner(Long restaurantId) {
        Restaurant restaurant = restaurantPersistencePort.findById(restaurantId)
                .orElseThrow(RestaurantNotFoundException::new);
        if (!restaurant.getOwnerId().equals(authenticatedUserPort.getAuthenticatedUserId())) {
            throw new NotRestaurantOwnerException();
        }
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
