package com.sgb.web;

import com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.CannotAcquireLockException;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.transaction.CannotCreateTransactionException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestValueException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Traduce TODAS las excepciones a respuestas JSON coherentes
 * ({@link ErrorRespuesta}) con el codigo HTTP adecuado.
 *
 * Antes:
 *  - los mensajes de negocio ("No existe un servicio con id 9") no le
 *    llegaban al cliente, porque Spring Boot oculta los mensajes por
 *    defecto: el usuario solo veia "Not Found";
 *  - errores previsibles (precio demasiado grande, notas demasiado largas,
 *    problemas de la base de datos) terminaban en 500 "Internal Server
 *    Error" sin explicacion.
 * Ahora el usuario recibe un mensaje claro y seguro, y el detalle tecnico
 * queda SOLO en el log del servidor, junto al idCorrelacion.
 */
@RestControllerAdvice
public class ManejadorErrores {

    private static final Logger log = LoggerFactory.getLogger(ManejadorErrores.class);

    /** Errores de negocio y de permisos lanzados por los servicios (mensajes ya controlados). */
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ErrorRespuesta> negocio(ResponseStatusException e, HttpServletRequest req) {
        HttpStatus estado = HttpStatus.resolve(e.getStatusCode().value());
        if (estado == null) {
            estado = HttpStatus.INTERNAL_SERVER_ERROR;
        }
        String mensaje = e.getReason() != null ? e.getReason() : estado.getReasonPhrase();
        return responder(estado, mensaje, req, null);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorRespuesta> validacion(MethodArgumentNotValidException e, HttpServletRequest req) {
        Map<String, String> campos = new LinkedHashMap<>();
        for (FieldError error : e.getBindingResult().getFieldErrors()) {
            campos.putIfAbsent(error.getField(), error.getDefaultMessage());
        }
        return responder(HttpStatus.BAD_REQUEST, "Hay datos invalidos en la peticion.", req, campos);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorRespuesta> jsonInvalido(HttpMessageNotReadableException e, HttpServletRequest req) {
        String mensaje = "El cuerpo de la peticion no es un JSON valido o tiene valores con formato incorrecto.";
        if (e.getCause() instanceof UnrecognizedPropertyException campo) {
            mensaje = "El campo '" + campo.getPropertyName() + "' no esta permitido en esta peticion.";
        }
        return responder(HttpStatus.BAD_REQUEST, mensaje, req, null);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorRespuesta> tipoInvalido(MethodArgumentTypeMismatchException e, HttpServletRequest req) {
        return responder(HttpStatus.BAD_REQUEST, "El parametro '" + e.getName() + "' tiene un formato invalido.",
                req, null);
    }

    @ExceptionHandler(MissingRequestValueException.class)
    public ResponseEntity<ErrorRespuesta> faltaValor(MissingRequestValueException e, HttpServletRequest req) {
        return responder(HttpStatus.BAD_REQUEST, "Falta un dato obligatorio en la peticion.", req, null);
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ErrorRespuesta> tipoContenido(HttpMediaTypeNotSupportedException e, HttpServletRequest req) {
        return responder(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "Envie el cuerpo como JSON (Content-Type: application/json).",
                req, null);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorRespuesta> metodo(HttpRequestMethodNotSupportedException e, HttpServletRequest req) {
        return responder(HttpStatus.METHOD_NOT_ALLOWED, "Metodo HTTP no permitido en esta ruta.", req, null);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErrorRespuesta> rutaInexistente(NoResourceFoundException e, HttpServletRequest req) {
        return responder(HttpStatus.NOT_FOUND, "La ruta solicitada no existe.", req, null);
    }

    /** Violacion de una restriccion de la BD (llave foranea, unica, NOT NULL, rango). */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorRespuesta> integridad(DataIntegrityViolationException e, HttpServletRequest req) {
        log.warn("Violacion de integridad en {} {}: {}", req.getMethod(), req.getRequestURI(),
                e.getMostSpecificCause().getClass().getSimpleName());
        return responder(HttpStatus.CONFLICT,
                "La operacion no se pudo completar porque viola una regla de integridad de los datos.", req, null);
    }

    /** Bloqueo o espera de bloqueo agotada (dos operaciones simultaneas sobre lo mismo). */
    @ExceptionHandler({PessimisticLockingFailureException.class, CannotAcquireLockException.class})
    public ResponseEntity<ErrorRespuesta> bloqueo(RuntimeException e, HttpServletRequest req) {
        log.warn("Conflicto de concurrencia en {} {}", req.getMethod(), req.getRequestURI());
        return responder(HttpStatus.CONFLICT, "Otra operacion estaba modificando los mismos datos. Intente de nuevo.",
                req, null);
    }

    /** La base de datos no responde o no hay conexiones disponibles. */
    @ExceptionHandler({CannotCreateTransactionException.class, DataAccessResourceFailureException.class})
    public ResponseEntity<ErrorRespuesta> sinBaseDeDatos(RuntimeException e, HttpServletRequest req) {
        log.error("Base de datos no disponible en {} {}: {}", req.getMethod(), req.getRequestURI(),
                e.getClass().getSimpleName());
        return responder(HttpStatus.SERVICE_UNAVAILABLE,
                "El servicio de datos no esta disponible en este momento. Intente mas tarde.", req, null);
    }

    /** Cualquier otro error: se registra completo en el log y al cliente solo se le da el idCorrelacion. */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorRespuesta> inesperado(Exception e, HttpServletRequest req) {
        log.error("Error inesperado en {} {}", req.getMethod(), req.getRequestURI(), e);
        return responder(HttpStatus.INTERNAL_SERVER_ERROR,
                "Ocurrio un error interno. Si persiste, informe el idCorrelacion al administrador.", req, null);
    }

    private static ResponseEntity<ErrorRespuesta> responder(HttpStatus estado, String mensaje,
                                                            HttpServletRequest req, Map<String, String> campos) {
        return ResponseEntity.status(estado).body(ErrorRespuesta.de(estado, mensaje, req.getRequestURI(), campos));
    }
}
