package com.plazoleta.plazoleta.application.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
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
@Schema(description = "Datos para crear un plato. El plato se crea siempre activo.")
public class DishRequestDto {

    @Schema(example = "Bandeja paisa")
    @NotBlank(message = "El nombre es obligatorio")
    private String name;

    @Schema(example = "25000", description = "Numero entero positivo mayor a 0 (no se aceptan decimales)")
    @NotNull(message = "El precio es obligatorio")
    @Positive(message = "El precio debe ser un numero entero positivo mayor a 0")
    private Integer price;

    @Schema(example = "Frijoles, arroz, carne molida, chicharron, huevo y aguacate")
    @NotBlank(message = "La descripcion es obligatoria")
    private String description;

    @Schema(example = "https://misitio.com/bandeja-paisa.png")
    @NotBlank(message = "La URL de la imagen es obligatoria")
    private String urlImage;

    @Schema(example = "2", description = "Id de la categoria: 1 Entradas, 2 Platos fuertes, 3 Postres, "
            + "4 Bebidas, 5 Acompanamientos")
    @NotNull(message = "La categoria es obligatoria")
    @Positive(message = "El id de la categoria debe ser positivo")
    private Long categoryId;

    @Schema(example = "1", description = "Id de un restaurante del propietario autenticado")
    @NotNull(message = "El id del restaurante es obligatorio")
    @Positive(message = "El id del restaurante debe ser positivo")
    private Long restaurantId;
}
