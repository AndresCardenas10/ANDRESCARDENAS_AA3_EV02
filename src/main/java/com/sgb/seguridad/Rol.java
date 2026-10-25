package com.sgb.seguridad;

import java.util.Optional;

/**
 * Roles del sistema, con los mismos identificadores que tiene la tabla
 * `rol` de la base de datos real (ver sgb.sql, INSERT INTO rol):
 * 1 = Administrador, 2 = Barbero, 3 = Cliente.
 *
 * Antes el numero 2 (Barbero) estaba escrito directamente en CitaService;
 * se centraliza aqui para que todas las reglas de acceso usen la misma
 * fuente y no queden numeros "magicos" repartidos por el codigo.
 */
public enum Rol {
    ADMINISTRADOR(1),
    BARBERO(2),
    CLIENTE(3);

    private final int idRol;

    Rol(int idRol) {
        this.idRol = idRol;
    }

    public int getIdRol() {
        return idRol;
    }

    public static Optional<Rol> desdeId(Integer idRol) {
        if (idRol == null) {
            return Optional.empty();
        }
        for (Rol rol : values()) {
            if (rol.idRol == idRol) {
                return Optional.of(rol);
            }
        }
        return Optional.empty();
    }
}
