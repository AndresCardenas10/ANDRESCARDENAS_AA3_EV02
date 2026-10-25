package com.sgb.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * DTO ("Data Transfer Object") = un objeto simple que solo transporta
 * datos, sin logica. Este es el que se recibe en el "body" de la
 * peticion HTTP cuando alguien crea o actualiza una cita desde la API
 * (por ejemplo, desde Postman o desde el front-end).
 *
 * Se usa un DTO en vez de recibir directamente la entidad Cita porque
 * quien llama a la API no manda objetos Usuario/Servicio completos, sino
 * solo los IDs (numeros) de cliente, barbero y servicio. El servicio
 * (CitaService) es quien busca esos IDs en la base de datos y arma la
 * entidad Cita real.
 *
 * Validaciones de formato (las de negocio estan en CitaService):
 *  - ids obligatorios y positivos;
 *  - notas de maximo 1000 caracteres (antes un texto de 100.000
 *    caracteres provocaba un error 500 al no caber en la columna TEXT).
 */
@Data
public class CitaSolicitudDTO {

    public static final int MAX_NOTAS = 1000;

    @NotNull(message = "idCliente es obligatorio")
    @Positive(message = "idCliente debe ser un numero positivo")
    private Integer idCliente;

    @NotNull(message = "idBarbero es obligatorio")
    @Positive(message = "idBarbero debe ser un numero positivo")
    private Integer idBarbero;

    @NotNull(message = "idServicio es obligatorio")
    @Positive(message = "idServicio debe ser un numero positivo")
    private Integer idServicio;

    @NotNull(message = "fechaHora es obligatoria (formato: 2026-10-05T14:30:00)")
    private LocalDateTime fechaHora;

    /** Opcional: comentario libre sobre la cita. */
    @Size(max = MAX_NOTAS, message = "notas admite como maximo " + MAX_NOTAS + " caracteres")
    private String notas;
}
