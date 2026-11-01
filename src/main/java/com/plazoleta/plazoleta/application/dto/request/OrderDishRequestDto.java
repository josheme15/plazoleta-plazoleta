package com.plazoleta.plazoleta.application.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Plato del pedido con su cantidad")
public class OrderDishRequestDto {

    @Schema(example = "12")
    @NotNull(message = "El id del plato es obligatorio")
    private Long dishId;

    @Schema(example = "2", description = "Numero entero mayor a 0")
    @NotNull(message = "La cantidad es obligatoria")
    @Positive(message = "La cantidad debe ser un numero entero mayor a 0")
    private Integer quantity;
}
