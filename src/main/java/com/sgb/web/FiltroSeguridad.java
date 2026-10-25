package com.sgb.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sgb.seguridad.UsuarioSesion;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Set;
import java.util.UUID;

/**
 * Filtro que se ejecuta ANTES de cualquier controlador. En este orden:
 *
 *  1. Identificador de correlacion: genera un id unico por peticion, lo
 *     pone en el log (MDC) y lo devuelve en la cabecera X-Correlation-Id.
 *  2. Cabeceras de seguridad en todas las respuestas (nosniff, no iframe,
 *     politica de contenido, sin cache para la API).
 *  3. Limite de tamano del cuerpo de las peticiones a /api (64 KB).
 *  4. Autenticacion: toda ruta /api/** exige sesion iniciada, salvo
 *     POST /api/auth/login. Sin sesion -> 401.
 *  5. CSRF: toda peticion que modifica datos (POST, PUT, PATCH, DELETE)
 *     debe traer la cabecera X-CSRF-Token igual al token de la sesion.
 *     Si falta o no coincide -> 403.
 *
 * Se implementa con un filtro de Servlet (sin Spring Security) para no
 * agregar dependencias nuevas que habria que descargar: todo lo que usa
 * ya viene con spring-boot-starter-web.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class FiltroSeguridad extends OncePerRequestFilter {

    public static final String MDC_CORRELACION = "idCorrelacion";
    public static final String CABECERA_CSRF = "X-CSRF-Token";
    static final long MAX_CUERPO_BYTES = 64 * 1024;

    private static final Set<String> METODOS_SEGUROS = Set.of("GET", "HEAD", "OPTIONS");
    private static final String RUTA_LOGIN = "/api/auth/login";

    private final ObjectMapper objectMapper;

    public FiltroSeguridad(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String idCorrelacion = UUID.randomUUID().toString();
        MDC.put(MDC_CORRELACION, idCorrelacion);
        try {
            response.setHeader("X-Correlation-Id", idCorrelacion);
            String ruta = rutaNormalizada(request);
            // Comparacion sin distinguir mayusculas y sobre la ruta ya
            // decodificada y normalizada por Tomcat: variantes como
            // "/api;x=1/servicios" o "/%61pi/servicios" tambien se protegen
            // (las pruebas mostraron que, comparando la URI cruda, esas
            // variantes saltaban el filtro).
            boolean esApi = ruta.toLowerCase(java.util.Locale.ROOT).startsWith("/api");
            agregarCabecerasSeguridad(response, esApi);

            if (!esApi) {
                chain.doFilter(request, response);
                return;
            }

            String metodo = request.getMethod().toUpperCase();
            boolean modifica = !METODOS_SEGUROS.contains(metodo);

            if (modifica && !cuerpoDentroDelLimite(request)) {
                escribirError(response, request, HttpStatus.PAYLOAD_TOO_LARGE,
                        "El cuerpo de la peticion supera el tamano maximo permitido (64 KB).");
                return;
            }

            if (modifica && RUTA_LOGIN.equals(ruta)) {
                chain.doFilter(request, response);
                return;
            }

            HttpSession sesion = request.getSession(false);
            Object usuario = sesion == null ? null : sesion.getAttribute(UsuarioSesion.ATRIBUTO);
            if (!(usuario instanceof UsuarioSesion usuarioSesion)) {
                escribirError(response, request, HttpStatus.UNAUTHORIZED,
                        "Debe iniciar sesion (POST /api/auth/login) para usar la API.");
                return;
            }

            if (modifica && !csrfValido(request, sesion)) {
                escribirError(response, request, HttpStatus.FORBIDDEN,
                        "Falta la cabecera " + CABECERA_CSRF + " o no es valida para esta sesion.");
                return;
            }

            request.setAttribute(UsuarioSesion.ATRIBUTO, usuarioSesion);
            chain.doFilter(request, response);
        } finally {
            MDC.remove(MDC_CORRELACION);
        }
    }

    /**
     * Ruta de la peticion tal como la resuelve el contenedor: decodificada
     * (%61 -> a), sin parametros de ruta (";x=1") y sin "." ni "//".
     */
    static String rutaNormalizada(HttpServletRequest request) {
        String servletPath = request.getServletPath() == null ? "" : request.getServletPath();
        String pathInfo = request.getPathInfo() == null ? "" : request.getPathInfo();
        String ruta = servletPath + pathInfo;
        return ruta.isEmpty() ? "/" : ruta;
    }

    private static void agregarCabecerasSeguridad(HttpServletResponse response, boolean esApi) {
        response.setHeader("X-Content-Type-Options", "nosniff");
        response.setHeader("X-Frame-Options", "DENY");
        response.setHeader("Referrer-Policy", "no-referrer");
        if (esApi) {
            response.setHeader("Cache-Control", "no-store");
            response.setHeader("Content-Security-Policy", "default-src 'none'; frame-ancestors 'none'");
        } else {
            // probador.html usa estilos y scripts en linea; se permite solo
            // eso y conexiones al mismo origen.
            response.setHeader("Content-Security-Policy",
                    "default-src 'self'; script-src 'self' 'unsafe-inline'; style-src 'self' 'unsafe-inline'; "
                            + "img-src 'self' data:; connect-src 'self'; frame-ancestors 'none'; "
                            + "base-uri 'none'; form-action 'self'");
        }
    }

    /**
     * Rechaza cuerpos declarados de mas de 64 KB y cuerpos sin longitud
     * declarada (transferencia "chunked"), porque su tamano no se puede
     * controlar antes de leerlos. Los navegadores y Postman siempre envian
     * Content-Length para un cuerpo JSON.
     */
    private static boolean cuerpoDentroDelLimite(HttpServletRequest request) {
        long longitud = request.getContentLengthLong();
        if (longitud > MAX_CUERPO_BYTES) {
            return false;
        }
        String transferencia = request.getHeader("Transfer-Encoding");
        return longitud >= 0 || transferencia == null;
    }

    private static boolean csrfValido(HttpServletRequest request, HttpSession sesion) {
        Object esperado = sesion.getAttribute(AuthController.ATRIBUTO_CSRF);
        String recibido = request.getHeader(CABECERA_CSRF);
        if (!(esperado instanceof String token) || recibido == null) {
            return false;
        }
        return MessageDigest.isEqual(token.getBytes(StandardCharsets.UTF_8),
                recibido.getBytes(StandardCharsets.UTF_8));
    }

    private void escribirError(HttpServletResponse response, HttpServletRequest request, HttpStatus estado,
                               String mensaje) throws IOException {
        response.setStatus(estado.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        objectMapper.writeValue(response.getOutputStream(),
                ErrorRespuesta.de(estado, mensaje, request.getRequestURI()));
    }
}
