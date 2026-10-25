package com.sgb.seguridad;

import java.io.Serializable;

/**
 * Datos MINIMOS del usuario autenticado que se guardan en la sesion HTTP.
 *
 * A proposito no contiene la contrasena (ni su hash), el correo ni el
 * telefono: solo lo necesario para decidir permisos (id y rol) y para
 * mostrar el nombre. Es inmutable (record) para que ningun controlador
 * pueda "cambiarse" el rol a mitad de una peticion.
 */
public record UsuarioSesion(Integer idUsuario, String nombreCompleto, Rol rol) implements Serializable {

    /** Nombre del atributo con el que FiltroSeguridad deja este objeto en cada peticion. */
    public static final String ATRIBUTO = "usuarioSesion";

    public boolean esAdministrador() {
        return rol == Rol.ADMINISTRADOR;
    }

    public boolean esBarbero() {
        return rol == Rol.BARBERO;
    }

    public boolean esCliente() {
        return rol == Rol.CLIENTE;
    }

    public boolean esElUsuario(Integer otroId) {
        return idUsuario != null && idUsuario.equals(otroId);
    }
}
