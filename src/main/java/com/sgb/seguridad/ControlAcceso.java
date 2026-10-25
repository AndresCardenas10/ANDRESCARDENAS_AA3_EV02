package com.sgb.seguridad;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/**
 * Comprobaciones de permisos reutilizables (RNF11: "El sistema debe
 * restringir el acceso a funcionalidades segun el rol del usuario").
 *
 * Las decisiones se toman SIEMPRE en el servidor a partir de la sesion,
 * nunca a partir de datos enviados por el cliente.
 */
public final class ControlAcceso {

    public static final String MENSAJE_SIN_PERMISO = "No tiene permiso para realizar esta operacion.";

    private ControlAcceso() {
    }

    public static void exigirAdministrador(UsuarioSesion actor) {
        exigir(actor != null && actor.esAdministrador());
    }

    public static void exigir(boolean condicion) {
        if (!condicion) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, MENSAJE_SIN_PERMISO);
        }
    }
}
