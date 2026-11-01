package com.plazoleta.plazoleta.application.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Pedido: restaurante y lista de platos (todos de ese restaurante) con su cantidad")
public class OrderRequestDto {

    @Schema(example = "1")
    @NotNull(message = "El id del restaurante es obligatorio")
    private Long restaurantId;

    @NotEmpty(message = "El pedido debe tener al menos un plato")
    private List<@Valid OrderDishRequestDto> dishes;
}
