package com.sgb.web;

import com.sgb.dto.LoginSolicitudDTO;
import com.sgb.dto.SesionRespuestaDTO;
import com.sgb.seguridad.AutenticacionService;
import com.sgb.seguridad.UsuarioSesion;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.SecureRandom;
import java.util.Base64;

/**
 * Inicio y cierre de sesion de la API (NUEVO en la auditoria).
 *
 * Antes la API no pedia ningun tipo de autenticacion: cualquiera que
 * llegara al puerto 8081 podia leer, crear, modificar o borrar citas y
 * cambiar precios. Ahora:
 *
 *   POST /api/auth/login   {"correo": "...", "contrasena": "..."}
 *        -> 200 {idUsuario, nombre, rol, csrfToken} y cookie de sesion
 *        -> 401 credenciales incorrectas | 429 demasiados intentos
 *   GET  /api/auth/sesion  -> datos de la sesion actual (401 si no hay)
 *   POST /api/auth/logout  -> 204, invalida la sesion
 *
 * La cookie de sesion es HttpOnly y SameSite=Strict. Las peticiones que
 * modifican datos deben enviar la cabecera X-CSRF-Token con el valor
 * csrfToken (ver FiltroSeguridad).
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    public static final String ATRIBUTO_CSRF = "csrfToken";
    private static final SecureRandom ALEATORIO = new SecureRandom();

    private final AutenticacionService autenticacionService;

    @PostMapping("/login")
    public SesionRespuestaDTO login(@Valid @RequestBody LoginSolicitudDTO datos, HttpServletRequest request) {
        UsuarioSesion usuario = autenticacionService.autenticar(
                datos.getCorreo(), datos.getContrasena(), request.getRemoteAddr());

        // Proteccion contra fijacion de sesion: se descarta cualquier
        // sesion previa y se crea una nueva con un identificador nuevo.
        HttpSession anterior = request.getSession(false);
        if (anterior != null) {
            anterior.invalidate();
        }
        HttpSession sesion = request.getSession(true);
        sesion.setAttribute(UsuarioSesion.ATRIBUTO, usuario);
        String csrf = generarToken();
        sesion.setAttribute(ATRIBUTO_CSRF, csrf);
        return respuesta(usuario, csrf);
    }

    @GetMapping("/sesion")
    public SesionRespuestaDTO sesion(@RequestAttribute(UsuarioSesion.ATRIBUTO) UsuarioSesion usuario,
                                     HttpServletRequest request) {
        return respuesta(usuario, (String) request.getSession(false).getAttribute(ATRIBUTO_CSRF));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest request) {
        HttpSession sesion = request.getSession(false);
        if (sesion != null) {
            sesion.invalidate();
        }
        return ResponseEntity.noContent().build();
    }

    private static SesionRespuestaDTO respuesta(UsuarioSesion u, String csrf) {
        return new SesionRespuestaDTO(u.idUsuario(), u.nombreCompleto(), u.rol().name(), csrf);
    }

    private static String generarToken() {
        byte[] bytes = new byte[32];
        ALEATORIO.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
