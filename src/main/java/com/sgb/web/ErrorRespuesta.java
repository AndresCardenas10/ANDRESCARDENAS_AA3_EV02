package com.sgb.web;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;

import java.time.OffsetDateTime;
import java.util.Map;

/**
 * Formato UNICO de los errores de la API.
 *
 * Conserva los campos que ya devolvia Spring Boot por defecto
 * (timestamp, status, error, path) para no romper a quien ya los lea, y
 * agrega:
 *  - mensaje: texto claro para el usuario (RNF10). Nunca contiene SQL,
 *    trazas, rutas internas ni datos sensibles;
 *  - idCorrelacion: el mismo identificador que aparece en el log del
 *    servidor, para encontrar el detalle tecnico del error;
 *  - campos: solo en errores de validacion, campo -> mensaje.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorRespuesta(String timestamp, int status, String error, String mensaje, String path,
                             String idCorrelacion, Map<String, String> campos) {

    public static ErrorRespuesta de(HttpStatus estado, String mensaje, String path) {
        return de(estado, mensaje, path, null);
    }

    public static ErrorRespuesta de(HttpStatus estado, String mensaje, String path, Map<String, String> campos) {
        return new ErrorRespuesta(OffsetDateTime.now().toString(), estado.value(), estado.getReasonPhrase(),
                mensaje, path, MDC.get(FiltroSeguridad.MDC_CORRELACION), campos);
    }
}
