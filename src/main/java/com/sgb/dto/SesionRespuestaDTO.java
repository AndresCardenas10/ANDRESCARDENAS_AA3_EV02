package com.sgb.dto;

/**
 * Respuesta de /api/auth/login y /api/auth/sesion.
 *
 * csrfToken: valor que el cliente debe enviar en la cabecera
 * "X-CSRF-Token" en toda peticion que modifique datos (POST, PUT, DELETE).
 */
public record SesionRespuestaDTO(Integer idUsuario, String nombre, String rol, String csrfToken) {
}
