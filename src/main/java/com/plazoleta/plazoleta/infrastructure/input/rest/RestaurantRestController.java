package com.plazoleta.plazoleta.infrastructure.input.rest;

import com.plazoleta.plazoleta.application.dto.request.RestaurantRequestDto;
import com.plazoleta.plazoleta.application.dto.response.RestaurantResponseDto;
import com.plazoleta.plazoleta.application.handler.IRestaurantHandler;
import com.plazoleta.plazoleta.infrastructure.exceptionhandler.ErrorResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/restaurants")
@RequiredArgsConstructor
@Tag(name = "Restaurantes", description = "Gestion de restaurantes de la plazoleta")
public class RestaurantRestController {

    private final IRestaurantHandler restaurantHandler;

    @Operation(summary = "Crear restaurante",
            description = "Crea un restaurante a cargo de un usuario con rol PROPIETARIO. "
                    + "Solo disponible para el rol ADMINISTRADOR.",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Restaurante creado",
                    content = @Content(schema = @Schema(implementation = RestaurantResponseDto.class))),
            @ApiResponse(responseCode = "400",
                    description = "Datos invalidos: campos obligatorios, nombre solo numerico, NIT o telefono "
                            + "con formato invalido, o el usuario indicado no tiene rol propietario",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Token ausente, invalido o expirado",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "El usuario autenticado no es ADMINISTRADOR",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "No existe un usuario con el id de propietario indicado",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "Ya existe un restaurante con ese NIT",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "503", description = "El servicio de usuarios no esta disponible",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping
    public ResponseEntity<RestaurantResponseDto> createRestaurant(
            @Valid @RequestBody RestaurantRequestDto restaurantRequestDto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(restaurantHandler.createRestaurant(restaurantRequestDto));
    }
}
