package com.sgb.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/** Cuerpo de POST /api/auth/login. */
@Data
public class LoginSolicitudDTO {

    @NotBlank(message = "El correo es obligatorio")
    @Size(max = 150, message = "El correo admite como maximo 150 caracteres")
    private String correo;

    @NotBlank(message = "La contrasena es obligatoria")
    @Size(max = 200, message = "La contrasena admite como maximo 200 caracteres")
    private String contrasena;

    /** Nunca incluir la contrasena en logs ni en mensajes. */
    @Override
    public String toString() {
        return "LoginSolicitudDTO{correo=[oculto], contrasena=[oculta]}";
    }
}
