package com.sgb.seguridad;

import com.sgb.modelo.Usuario;
import com.sgb.repositorio.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

/**
 * Inicio de sesion contra la tabla `usuario` existente (la misma que usa
 * el modulo de Login de la version Servlets/JSP).
 *
 * Reglas:
 *  - el mensaje de error es SIEMPRE el mismo ("Correo o contrasena
 *    incorrectos.") exista o no el correo y este o no activa la cuenta,
 *    para no revelar que correos estan registrados;
 *  - tras 5 fallos seguidos para un correo (o 20 desde una misma IP) en
 *    15 minutos se responde 429 (ver IntentosLoginService);
 *  - si la contrasena guardada todavia esta en texto plano y coincide, se
 *    reemplaza por su hash BCrypt (migracion transparente).
 */
@Service
@RequiredArgsConstructor
public class AutenticacionService {

    public static final String MENSAJE_CREDENCIALES = "Correo o contrasena incorrectos.";
    public static final String MENSAJE_BLOQUEO =
            "Demasiados intentos fallidos. Espere unos minutos antes de volver a intentarlo.";

    private static final Logger log = LoggerFactory.getLogger(AutenticacionService.class);

    private final UsuarioRepository usuarioRepository;
    private final ContrasenaService contrasenaService;
    private final IntentosLoginService intentosLoginService;

    @Transactional
    public UsuarioSesion autenticar(String correo, String contrasena, String ip) {
        String correoNormalizado = correo == null ? "" : correo.trim();

        if (intentosLoginService.estaBloqueado(correoNormalizado, ip)) {
            log.warn("Inicio de sesion bloqueado temporalmente por exceso de intentos (ip={})", ip);
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, MENSAJE_BLOQUEO);
        }

        Optional<Usuario> encontrado = usuarioRepository.findByCorreo(correoNormalizado);
        if (encontrado.isEmpty()) {
            contrasenaService.verificarSenuelo(contrasena);
            return fallo(correoNormalizado, ip);
        }

        Usuario usuario = encontrado.get();
        String almacenado = usuarioRepository.buscarHashContrasena(usuario.getIdUsuario()).orElse(null);
        if (!contrasenaService.verificar(contrasena, almacenado)) {
            return fallo(correoNormalizado, ip);
        }
        Optional<Rol> rol = Rol.desdeId(usuario.getIdRol());
        if (!Boolean.TRUE.equals(usuario.getActivo()) || rol.isEmpty()) {
            return fallo(correoNormalizado, ip);
        }

        if (contrasenaService.necesitaMigracion(almacenado)) {
            int filas = usuarioRepository.reemplazarHashContrasena(
                    usuario.getIdUsuario(), almacenado, contrasenaService.generarHash(contrasena));
            if (filas == 1) {
                log.info("Contrasena del usuario id={} migrada de texto plano a BCrypt", usuario.getIdUsuario());
            }
        }

        intentosLoginService.registrarExito(correoNormalizado);
        log.info("Inicio de sesion correcto: usuario id={} rol={}", usuario.getIdUsuario(), rol.get());
        return new UsuarioSesion(usuario.getIdUsuario(), usuario.getNombreCompleto(), rol.get());
    }

    private UsuarioSesion fallo(String correo, String ip) {
        intentosLoginService.registrarFallo(correo, ip);
        log.info("Inicio de sesion fallido (ip={})", ip);
        throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, MENSAJE_CREDENCIALES);
    }
}
