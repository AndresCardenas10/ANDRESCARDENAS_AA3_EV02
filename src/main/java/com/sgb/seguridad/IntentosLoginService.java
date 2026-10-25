package com.sgb.seguridad;

import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Limita los intentos fallidos de inicio de sesion (proteccion contra
 * fuerza bruta). Se cuentan por separado por CORREO ({@value #MAX_FALLOS}
 * fallos) y por DIRECCION IP ({@value #MAX_FALLOS_IP} fallos, un limite
 * mas alto porque en un entorno local todos los usuarios comparten la IP
 * 127.0.0.1) dentro de la ventana de {@link #VENTANA}. Al superar
 * cualquiera de los dos limites se bloquea hasta que esos fallos salgan de
 * la ventana.
 *
 * Limitacion conocida: el conteo vive en memoria, asi que se reinicia al
 * reiniciar la aplicacion y no se comparte entre varias instancias. Para
 * el entorno local de este proyecto (una sola instancia) es suficiente.
 */
@Component
public class IntentosLoginService {

    static final int MAX_FALLOS = 5;
    static final int MAX_FALLOS_IP = 20;
    static final Duration VENTANA = Duration.ofMinutes(15);
    /** Tope de claves en memoria para que un atacante no pueda agotar la RAM. */
    private static final int MAX_CLAVES = 10_000;

    private final Map<String, Deque<Instant>> fallos = new ConcurrentHashMap<>();
    private final Clock reloj;

    public IntentosLoginService() {
        this(Clock.systemUTC());
    }

    IntentosLoginService(Clock reloj) {
        this.reloj = reloj;
    }

    public boolean estaBloqueado(String correo, String ip) {
        return contar(claveCorreo(correo)) >= MAX_FALLOS || contar(claveIp(ip)) >= MAX_FALLOS_IP;
    }

    public void registrarFallo(String correo, String ip) {
        if (fallos.size() > MAX_CLAVES) {
            limpiarVencidos();
        }
        agregar(claveCorreo(correo));
        agregar(claveIp(ip));
    }

    /**
     * Un login correcto limpia los fallos de ESE correo. Los fallos por IP
     * no se limpian (solo vencen con el tiempo) para que alguien con una
     * cuenta valida no pueda "reiniciar" el contador mientras prueba
     * contrasenas de otras cuentas desde la misma IP.
     */
    public void registrarExito(String correo) {
        fallos.remove(claveCorreo(correo));
    }

    private int contar(String clave) {
        Deque<Instant> marcas = fallos.get(clave);
        if (marcas == null) {
            return 0;
        }
        synchronized (marcas) {
            descartarVencidas(marcas);
            return marcas.size();
        }
    }

    private void agregar(String clave) {
        Deque<Instant> marcas = fallos.computeIfAbsent(clave, k -> new ArrayDeque<>());
        synchronized (marcas) {
            descartarVencidas(marcas);
            marcas.addLast(reloj.instant());
            while (marcas.size() > MAX_FALLOS_IP * 2) {
                marcas.removeFirst();
            }
        }
    }

    private void descartarVencidas(Deque<Instant> marcas) {
        Instant limite = reloj.instant().minus(VENTANA);
        while (!marcas.isEmpty() && marcas.peekFirst().isBefore(limite)) {
            marcas.removeFirst();
        }
    }

    private void limpiarVencidos() {
        Iterator<Map.Entry<String, Deque<Instant>>> it = fallos.entrySet().iterator();
        while (it.hasNext()) {
            Deque<Instant> marcas = it.next().getValue();
            synchronized (marcas) {
                descartarVencidas(marcas);
                if (marcas.isEmpty()) {
                    it.remove();
                }
            }
        }
    }

    private static String claveCorreo(String correo) {
        return "c:" + (correo == null ? "" : correo.trim().toLowerCase(Locale.ROOT));
    }

    private static String claveIp(String ip) {
        return "i:" + (ip == null ? "" : ip);
    }
}
