package com.sgb.dto;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

/**
 * Datos que se aceptan al crear o actualizar un servicio.
 *
 * Antes el controlador recibia directamente la entidad Servicio, de modo
 * que cualquier campo de la entidad podia llegar desde la peticion
 * (asignacion masiva). Con este DTO solo se aceptan los campos editables;
 * el id siempre lo decide la base de datos o la URL. La respuesta sigue
 * siendo el mismo JSON de antes (la entidad Servicio).
 *
 * Los limites coinciden con las columnas reales de la tabla `servicio`:
 * nombre VARCHAR(100), precio DECIMAL(8,2) (maximo 999999.99).
 *
 * Validaciones de caracteres especiales (Evidencia AA3-EV02):
 *  - nombre: solo letras (con tildes y n con virgulilla), numeros,
 *    espacios y los signos . , ( ) - ; minimo 3 caracteres;
 *  - descripcion: no admite los caracteres < > { } (evita que alguien
 *    guarde codigo HTML o JavaScript que luego se muestre en una pagina).
 */
@Data
public class ServicioSolicitudDTO {

    @NotBlank(message = "El nombre del servicio es obligatorio")
    @Size(min = 3, max = 100, message = "El nombre debe tener entre 3 y 100 caracteres")
    @Pattern(regexp = ValidacionesTexto.NOMBRE_PERMITIDO, message = ValidacionesTexto.MENSAJE_NOMBRE)
    private String nombre;

    @Size(max = 1000, message = "La descripcion admite como maximo 1000 caracteres")
    @Pattern(regexp = ValidacionesTexto.TEXTO_LIBRE_SEGURO, message = ValidacionesTexto.MENSAJE_TEXTO_LIBRE)
    private String descripcion;

    @NotNull(message = "El precio es obligatorio")
    @Positive(message = "El precio debe ser mayor que cero")
    @Digits(integer = 6, fraction = 2, message = "El precio admite hasta 6 enteros y 2 decimales (maximo 999999.99)")
    private BigDecimal precio;

    @NotNull(message = "La duracion en minutos es obligatoria")
    @Positive(message = "La duracion debe ser mayor que cero")
    @Max(value = 1440, message = "La duracion no puede superar 1440 minutos (24 horas)")
    private Integer duracionMin;

    /** Opcional: si no se envia, al crear queda activo y al actualizar no cambia. */
    private Boolean activo;
}
