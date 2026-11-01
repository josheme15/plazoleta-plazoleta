package com.plazoleta.plazoleta.infrastructure.input.rest;

import com.plazoleta.plazoleta.application.dto.response.DishMenuItemResponseDto;
import com.plazoleta.plazoleta.application.dto.response.PageResponseDto;
import com.plazoleta.plazoleta.application.handler.IDishHandler;
import com.plazoleta.plazoleta.infrastructure.exceptionhandler.ErrorResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/restaurants")
@RequiredArgsConstructor
@Tag(name = "Menu", description = "Menu de cada restaurante para los clientes")
public class RestaurantMenuRestController {

    private final IDishHandler dishHandler;

    @Operation(summary = "Platos por restaurante",
            description = "Lista los platos activos del menu de un restaurante, paginados y ordenados por nombre. "
                    + "Se puede filtrar por categoria. Solo disponible para el rol CLIENTE.",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Pagina de platos del menu"),
            @ApiResponse(responseCode = "400",
                    description = "page negativo, size menor o igual a 0, o parametros no numericos",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Token ausente, invalido o expirado",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "El usuario autenticado no es CLIENTE",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "No existe el restaurante o la categoria indicados",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/{restaurantId}/dishes")
    public ResponseEntity<PageResponseDto<DishMenuItemResponseDto>> listDishesByRestaurant(
            @PathVariable Long restaurantId,
            @Parameter(description = "Filtra por categoria (opcional): 1 Entradas, 2 Platos fuertes, 3 Postres, "
                    + "4 Bebidas, 5 Acompanamientos", example = "2")
            @RequestParam(required = false) Long categoryId,
            @Parameter(description = "Numero de pagina, desde 0", example = "0")
            @RequestParam(defaultValue = "0") Integer page,
            @Parameter(description = "Cantidad de platos por pagina", example = "10")
            @RequestParam(defaultValue = "10") Integer size) {
        return ResponseEntity.ok(dishHandler.listDishesByRestaurant(restaurantId, categoryId, page, size));
    }
}
