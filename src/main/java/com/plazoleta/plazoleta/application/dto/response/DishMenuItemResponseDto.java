package com.plazoleta.plazoleta.application.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Plato del menu que ve el cliente (HU-10).
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Plato del menu de un restaurante")
public class DishMenuItemResponseDto {

    @Schema(example = "12")
    private Long id;

    @Schema(example = "Bandeja paisa")
    private String name;

    @Schema(example = "32000")
    private Integer price;

    @Schema(example = "Frijoles, arroz, carne molida, chicharron, huevo y aguacate")
    private String description;

    @Schema(example = "https://misitio.com/bandeja-paisa.png")
    private String urlImage;

    @Schema(example = "2", description = "1 Entradas, 2 Platos fuertes, 3 Postres, 4 Bebidas, 5 Acompanamientos")
    private Long categoryId;
}
