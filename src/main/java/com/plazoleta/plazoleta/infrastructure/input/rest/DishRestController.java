package com.plazoleta.plazoleta.infrastructure.input.rest;

import com.plazoleta.plazoleta.application.dto.request.DishRequestDto;
import com.plazoleta.plazoleta.application.dto.request.DishStatusRequestDto;
import com.plazoleta.plazoleta.application.dto.request.DishUpdateRequestDto;
import com.plazoleta.plazoleta.application.dto.response.DishResponseDto;
import com.plazoleta.plazoleta.application.handler.IDishHandler;
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
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/dishes")
@RequiredArgsConstructor
@Tag(name = "Platos", description = "Gestion de los platos del menu de cada restaurante")
public class DishRestController {

    private final IDishHandler dishHandler;

    @Operation(summary = "Crear plato",
            description = "Crea un plato en el menu de un restaurante. Solo el PROPIETARIO de ese restaurante "
                    + "puede hacerlo. El plato queda activo por defecto.",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Plato creado (activo = true)",
                    content = @Content(schema = @Schema(implementation = DishResponseDto.class))),
            @ApiResponse(responseCode = "400",
                    description = "Datos invalidos: campos obligatorios o precio que no es un entero mayor a 0",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Token ausente, invalido o expirado",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403",
                    description = "El usuario no tiene rol PROPIETARIO o no es el propietario del restaurante",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "No existe el restaurante o la categoria indicados",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping
    public ResponseEntity<DishResponseDto> createDish(@Valid @RequestBody DishRequestDto dishRequestDto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(dishHandler.createDish(dishRequestDto));
    }

    @Operation(summary = "Editar plato",
            description = "Actualiza el precio y/o la descripcion de un plato. Solo el PROPIETARIO del restaurante "
                    + "al que pertenece el plato puede hacerlo. Cualquier otro campo enviado se ignora.",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Plato actualizado",
                    content = @Content(schema = @Schema(implementation = DishResponseDto.class))),
            @ApiResponse(responseCode = "400",
                    description = "No se envio precio ni descripcion, el precio no es un entero mayor a 0, "
                            + "la descripcion esta vacia o el id no es numerico",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Token ausente, invalido o expirado",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403",
                    description = "El usuario no tiene rol PROPIETARIO o no es el propietario del restaurante",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "No existe un plato con el id indicado",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PatchMapping("/{id}")
    public ResponseEntity<DishResponseDto> updateDish(@PathVariable Long id,
                                                      @Valid @RequestBody DishUpdateRequestDto dishUpdateRequestDto) {
        return ResponseEntity.ok(dishHandler.updateDish(id, dishUpdateRequestDto));
    }

    @Operation(summary = "Activar o desactivar plato",
            description = "Cambia el estado del plato: active=true lo habilita y active=false lo deshabilita del menu. "
                    + "Solo el PROPIETARIO del restaurante al que pertenece el plato puede hacerlo.",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Estado del plato actualizado",
                    content = @Content(schema = @Schema(implementation = DishResponseDto.class))),
            @ApiResponse(responseCode = "400", description = "No se envio el campo active o el id no es numerico",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Token ausente, invalido o expirado",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403",
                    description = "El usuario no tiene rol PROPIETARIO o el plato es de otro restaurante",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "No existe un plato con el id indicado",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PatchMapping("/{id}/status")
    public ResponseEntity<DishResponseDto> changeDishStatus(@PathVariable Long id,
                                                            @Valid @RequestBody DishStatusRequestDto dishStatusRequestDto) {
        return ResponseEntity.ok(dishHandler.changeDishStatus(id, dishStatusRequestDto));
    }
}
