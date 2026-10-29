package com.plazoleta.plazoleta.application.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DishResponseDto {

    private Long id;
    private String name;
    private Integer price;
    private String description;
    private String urlImage;
    private Long categoryId;
    private Long restaurantId;
    private Boolean active;
}
