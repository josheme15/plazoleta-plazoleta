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
@Schema(description = "Pedido con todos sus campos")
public class OrderResponseDto {

    @Schema(example = "1")
    private Long id;

    @Schema(example = "30", description = "Id del cliente que hizo el pedido")
    private Long clientId;

    @Schema(example = "1")
    private Long restaurantId;

    @Schema(example = "2026-11-01T12:30:00")
    private LocalDateTime date;

    @Schema(example = "PENDIENTE", description = "PENDIENTE, EN_PREPARACION, LISTO, ENTREGADO o CANCELADO")
    private String status;

    @Schema(example = "null", description = "Id del empleado que atiende el pedido (null mientras no se asigne)")
    private Long employeeId;

    private List<OrderDishResponseDto> dishes;
}
