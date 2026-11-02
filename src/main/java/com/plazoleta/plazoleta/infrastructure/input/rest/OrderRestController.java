package com.plazoleta.plazoleta.infrastructure.input.rest;

import com.plazoleta.plazoleta.application.dto.request.OrderRequestDto;
import com.plazoleta.plazoleta.application.dto.response.OrderResponseDto;
import com.plazoleta.plazoleta.application.dto.response.PageResponseDto;
import com.plazoleta.plazoleta.application.handler.IOrderHandler;
import com.plazoleta.plazoleta.domain.model.OrderStatus;
import com.plazoleta.plazoleta.infrastructure.exceptionhandler.ErrorResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/orders")
@RequiredArgsConstructor
@Tag(name = "Pedidos", description = "Pedidos de los clientes")
public class OrderRestController {

    private final IOrderHandler orderHandler;

    @Operation(summary = "Realizar pedido",
            description = "El cliente pide platos de un mismo restaurante, con la cantidad de cada uno. El pedido queda "
                    + "en estado PENDIENTE. No se permite un nuevo pedido si el cliente tiene uno en proceso "
                    + "(pendiente, en preparacion o listo). Solo disponible para el rol CLIENTE.",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Pedido creado en estado PENDIENTE",
                    content = @Content(schema = @Schema(implementation = OrderResponseDto.class))),
            @ApiResponse(responseCode = "400",
                    description = "Datos invalidos: sin restaurante o sin platos, cantidad menor o igual a 0, plato "
                            + "repetido, plato de otro restaurante o plato no disponible",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Token ausente, invalido o expirado",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "El usuario autenticado no es CLIENTE",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "No existe el restaurante o alguno de los platos",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "El cliente ya tiene un pedido en proceso",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping
    public ResponseEntity<OrderResponseDto> createOrder(@Valid @RequestBody OrderRequestDto orderRequestDto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(orderHandler.createOrder(orderRequestDto));
    }

    @Operation(summary = "Listar pedidos del restaurante (filtro por estado)",
            description = "Devuelve los pedidos del restaurante al que pertenece el empleado autenticado (sale de su "
                    + "token), paginados y del mas antiguo al mas reciente, con todos sus campos. Se puede filtrar "
                    + "por estado. Solo disponible para el rol EMPLEADO. Cubre el listado de las HU-12 y HU-13: el "
                    + "empleado busca los pedidos PENDIENTE para asignarse uno con PATCH /orders/{id}/assign.",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Pagina de pedidos del restaurante"),
            @ApiResponse(responseCode = "400",
                    description = "Estado invalido, page negativo, size menor o igual a 0 o parametros no numericos",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Token ausente, invalido o expirado",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403",
                    description = "El usuario no es EMPLEADO o no tiene un restaurante asignado",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping
    public ResponseEntity<PageResponseDto<OrderResponseDto>> listRestaurantOrders(
            @Parameter(description = "Filtra por estado (opcional): PENDIENTE, EN_PREPARACION, LISTO, ENTREGADO o "
                    + "CANCELADO", example = "PENDIENTE")
            @RequestParam(required = false) OrderStatus status,
            @Parameter(description = "Numero de pagina, desde 0", example = "0")
            @RequestParam(defaultValue = "0") Integer page,
            @Parameter(description = "Cantidad de pedidos por pagina", example = "10")
            @RequestParam(defaultValue = "10") Integer size) {
        return ResponseEntity.ok(orderHandler.listRestaurantOrders(status, page, size));
    }

    @Operation(summary = "Asignarse un pedido (pasa a EN_PREPARACION)",
            description = "El empleado autenticado se asigna un pedido PENDIENTE de su restaurante: el campo "
                    + "employeeId queda con su id (sale de su token) y el estado pasa a EN_PREPARACION. "
                    + "Solo disponible para el rol EMPLEADO.",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Pedido asignado y en preparacion",
                    content = @Content(schema = @Schema(implementation = OrderResponseDto.class))),
            @ApiResponse(responseCode = "400", description = "El id del pedido no es numerico",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Token ausente, invalido o expirado",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403",
                    description = "El usuario no es EMPLEADO, no tiene restaurante o el pedido es de otro restaurante",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "No existe un pedido con el id indicado",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "El pedido no esta PENDIENTE (ya fue asignado o avanzo)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PatchMapping("/{id}/assign")
    public ResponseEntity<OrderResponseDto> assignOrder(@PathVariable Long id) {
        return ResponseEntity.ok(orderHandler.assignOrder(id));
    }
}
