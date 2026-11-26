package cl.duoc.tacticaldev.dto;

import jakarta.validation.constraints.*;

public record PuntoVentaRequest(
        @NotBlank(message = "El nombre es obligatorio") @Size(max = 100) String nombre,
        @NotBlank(message = "La dirección es obligatoria") @Size(max = 150) String direccion,
        @NotBlank(message = "La comuna es obligatoria") @Size(max = 60) String comuna,
        @NotNull(message = "La latitud es obligatoria")
        @DecimalMin(value = "-90.0", message = "Latitud inválida") @DecimalMax(value = "90.0", message = "Latitud inválida")
        Double latitud,
        @NotNull(message = "La longitud es obligatoria")
        @DecimalMin(value = "-180.0", message = "Longitud inválida") @DecimalMax(value = "180.0", message = "Longitud inválida")
        Double longitud,
        @NotNull(message = "El radio es obligatorio")
        @Min(value = 20, message = "El radio mínimo es 20 m") @Max(value = 1000, message = "El radio máximo es 1000 m")
        Integer radioMetros,
        @NotNull(message = "El cliente es obligatorio") Long clienteId
) {}
