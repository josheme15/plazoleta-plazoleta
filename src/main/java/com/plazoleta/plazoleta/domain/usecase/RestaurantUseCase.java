package com.plazoleta.plazoleta.domain.usecase;

import com.plazoleta.plazoleta.domain.api.IRestaurantServicePort;
import com.plazoleta.plazoleta.domain.exception.InvalidNitException;
import com.plazoleta.plazoleta.domain.exception.InvalidPaginationException;
import com.plazoleta.plazoleta.domain.exception.InvalidPhoneException;
import com.plazoleta.plazoleta.domain.exception.InvalidRestaurantNameException;
import com.plazoleta.plazoleta.domain.exception.NitAlreadyExistsException;
import com.plazoleta.plazoleta.domain.exception.OwnerNotFoundException;
import com.plazoleta.plazoleta.domain.exception.RequiredFieldException;
import com.plazoleta.plazoleta.domain.exception.RestaurantNotFoundException;
import com.plazoleta.plazoleta.domain.exception.UserIsNotOwnerException;
import com.plazoleta.plazoleta.domain.model.PageResult;
import com.plazoleta.plazoleta.domain.model.Restaurant;
import com.plazoleta.plazoleta.domain.spi.IRestaurantPersistencePort;
import com.plazoleta.plazoleta.domain.spi.IUserClientPort;

import java.util.regex.Pattern;

public class RestaurantUseCase implements IRestaurantServicePort {

    public static final String OWNER_ROLE = "OWNER";

    private static final Pattern NIT_PATTERN = Pattern.compile("^\\d+$");
    // Maximo 13 caracteres: '+' opcional seguido de hasta 12 digitos, o hasta 13 digitos sin '+'.
    private static final Pattern PHONE_PATTERN = Pattern.compile("^(\\+\\d{1,12}|\\d{1,13})$");
    // Debe contener al menos una letra (puede tener numeros, pero no solo numeros).
    private static final Pattern NAME_WITH_LETTER_PATTERN = Pattern.compile(".*\\p{L}.*");

    private final IRestaurantPersistencePort restaurantPersistencePort;
    private final IUserClientPort userClientPort;

    public RestaurantUseCase(IRestaurantPersistencePort restaurantPersistencePort, IUserClientPort userClientPort) {
        this.restaurantPersistencePort = restaurantPersistencePort;
        this.userClientPort = userClientPort;
    }

    @Override
    public Restaurant createRestaurant(Restaurant restaurant) {
        validateRequiredFields(restaurant);
        validateName(restaurant.getName());
        validateNit(restaurant.getNit());
        validatePhone(restaurant.getPhone());

        if (restaurantPersistencePort.existsByNit(restaurant.getNit())) {
            throw new NitAlreadyExistsException();
        }
        validateOwner(restaurant.getOwnerId());

        return restaurantPersistencePort.saveRestaurant(restaurant);
    }

    @Override
    public Restaurant getRestaurantById(Long restaurantId) {
        if (restaurantId == null) {
            throw new RequiredFieldException("idRestaurante");
        }
        return restaurantPersistencePort.findById(restaurantId).orElseThrow(RestaurantNotFoundException::new);
    }

    @Override
    public PageResult<Restaurant> listRestaurants(Integer page, Integer size) {
        if (page == null || size == null || page < 0 || size <= 0) {
            throw new InvalidPaginationException();
        }
        return restaurantPersistencePort.findAllOrderedByName(page, size);
    }

    private void validateRequiredFields(Restaurant restaurant) {
        requireText(restaurant.getName(), "nombre");
        requireText(restaurant.getNit(), "nit");
        requireText(restaurant.getAddress(), "direccion");
        requireText(restaurant.getPhone(), "telefono");
        requireText(restaurant.getUrlLogo(), "urlLogo");
        if (restaurant.getOwnerId() == null) {
            throw new RequiredFieldException("idPropietario");
        }
    }

    private void requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new RequiredFieldException(fieldName);
        }
    }

    private void validateName(String name) {
        if (!NAME_WITH_LETTER_PATTERN.matcher(name).matches()) {
            throw new InvalidRestaurantNameException();
        }
    }

    private void validateNit(String nit) {
        if (!NIT_PATTERN.matcher(nit).matches()) {
            throw new InvalidNitException();
        }
    }

    private void validatePhone(String phone) {
        if (!PHONE_PATTERN.matcher(phone).matches()) {
            throw new InvalidPhoneException();
        }
    }

    private void validateOwner(Long ownerId) {
        String role = userClientPort.findUserRoleById(ownerId)
                .orElseThrow(OwnerNotFoundException::new);
        if (!OWNER_ROLE.equals(role)) {
            throw new UserIsNotOwnerException();
        }
    }
}
