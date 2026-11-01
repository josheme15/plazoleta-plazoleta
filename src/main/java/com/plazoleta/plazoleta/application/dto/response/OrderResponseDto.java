package com.plazoleta.plazoleta.application.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class OrderResponseDto {

    @Schema(example = "1")
    private Long id;

    @Schema(example = "1")
    private Long restaurantId;

    @Schema(example = "PENDIENTE")
    private String status;

    @Schema(example = "2026-11-01T12:30:00")
    private LocalDateTime date;

    private List<OrderDishResponseDto> dishes;
}
