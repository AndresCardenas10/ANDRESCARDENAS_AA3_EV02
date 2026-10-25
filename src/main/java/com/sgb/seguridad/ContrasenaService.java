package com.sgb.seguridad;

import org.mindrot.jbcrypt.BCrypt;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/**
 * Hash y verificacion de contrasenas con BCrypt.
 *
 * CONTEXTO: la columna usuario.contrasena_hash de la base real guarda hoy
 * las contrasenas en TEXTO PLANO (asi lo documenta UsuarioDAO del modulo
 * Servlets/JSP). Para no dejar a nadie sin acceso, este servicio acepta
 * los dos formatos:
 *   - hash BCrypt ($2a$ / $2b$ / $2y$): se verifica con BCrypt;
 *   - texto plano heredado: se compara en tiempo constante y, si coincide,
 *     quien llama debe reemplazarlo por un hash BCrypt (migracion gradual,
 *     ver {@link #necesitaMigracion(String)}).
 *
 * Asi, cada usuario queda migrado a BCrypt la primera vez que inicia
 * sesion correctamente, sin pedirle que cambie su contrasena.
 */
@Component
public class ContrasenaService {

    /** Factor de costo de BCrypt (2^12 iteraciones, ~0,2-0,3 s por verificacion). */
    static final int COSTO = 12;

    /** BCrypt solo usa los primeros 72 bytes; mas alla se rechaza para no truncar en silencio. */
    public static final int MAX_BYTES = 72;

    /**
     * Hash "señuelo" que se verifica cuando el correo no existe, para que el
     * tiempo de respuesta sea parecido exista o no la cuenta (evita
     * descubrir correos registrados midiendo tiempos).
     */
    private final String hashSenuelo = BCrypt.hashpw("senuelo-sin-uso", BCrypt.gensalt(COSTO));

    public String generarHash(String contrasenaPlana) {
        if (contrasenaPlana == null || contrasenaPlana.isEmpty()) {
            throw new IllegalArgumentException("La contrasena no puede estar vacia");
        }
        if (contrasenaPlana.getBytes(StandardCharsets.UTF_8).length > MAX_BYTES) {
            throw new IllegalArgumentException("La contrasena supera el maximo de " + MAX_BYTES + " bytes");
        }
        return BCrypt.hashpw(contrasenaPlana, BCrypt.gensalt(COSTO));
    }

    /**
     * @return true solo si la contrasena coincide con lo almacenado.
     *         Nunca lanza excepcion por un valor almacenado mal formado.
     */
    public boolean verificar(String contrasenaPlana, String almacenado) {
        if (contrasenaPlana == null || almacenado == null || almacenado.isEmpty()) {
            return false;
        }
        if (esHashBcrypt(almacenado)) {
            try {
                return BCrypt.checkpw(contrasenaPlana, almacenado);
            } catch (IllegalArgumentException hashCorrupto) {
                return false;
            }
        }
        // Formato heredado (texto plano): comparacion en tiempo constante.
        return MessageDigest.isEqual(
                contrasenaPlana.getBytes(StandardCharsets.UTF_8),
                almacenado.getBytes(StandardCharsets.UTF_8));
    }

    /** Consume el mismo tiempo que una verificacion real (usar cuando el usuario no existe). */
    public void verificarSenuelo(String contrasenaPlana) {
        verificar(contrasenaPlana == null ? "" : contrasenaPlana, hashSenuelo);
    }

    /** true si el valor almacenado todavia no es un hash BCrypt y debe migrarse. */
    public boolean necesitaMigracion(String almacenado) {
        return almacenado != null && !esHashBcrypt(almacenado);
    }

    static boolean esHashBcrypt(String valor) {
        return valor != null
                && valor.length() == 60
                && (valor.startsWith("$2a$") || valor.startsWith("$2b$") || valor.startsWith("$2y$"));
    }
}
