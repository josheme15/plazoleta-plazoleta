package com.plazoleta.plazoleta.application.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Solo se pueden editar el precio y la descripcion. Cualquier otro campo enviado se ignora.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Campos editables de un plato. Enviar al menos uno; los demas campos se ignoran.")
public class DishUpdateRequestDto {

    @Schema(example = "28000", description = "Numero entero positivo mayor a 0 (no se aceptan decimales)")
    @Positive(message = "El precio debe ser un numero entero positivo mayor a 0")
    private Integer price;

    @Schema(example = "Frijoles, arroz, carne molida, chicharron, huevo, aguacate y arepa")
    private String description;
}
