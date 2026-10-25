package com.sgb.modelo;

import java.util.EnumSet;
import java.util.Set;

/**
 * Estados posibles de una cita.
 *
 * OJO: los nombres de estas constantes estan en minuscula (pendiente,
 * confirmada, cancelada, completada) en vez de la convencion normal de
 * Java para enums (MAYUSCULAS). Esto es intencional: la columna `estado`
 * de la tabla `cita` en la base de datos real es un ENUM de MySQL con
 * esos mismos valores en minuscula. Como se mapea con
 * @Enumerated(EnumType.STRING) en la entidad Cita, el nombre de la
 * constante de Java tiene que coincidir letra por letra con el valor
 * guardado en MySQL, o Hibernate lanzaria un error al leer o guardar una
 * cita.
 *
 * TRANSICIONES PERMITIDAS: son las mismas que ya aplica la interfaz web
 * del proyecto (js/dashboard-barber.js y js/dashboard-client.js):
 *   pendiente  -> confirmada | completada | cancelada
 *   confirmada -> completada | cancelada
 *   cancelada y completada son estados FINALES (no cambian mas).
 * Antes la API permitia, por ejemplo, "completar" una cita cancelada.
 */
public enum EstadoCita {
    pendiente,
    confirmada,
    cancelada,
    completada;

    /** Estados que todavia ocupan el horario del barbero. */
    public static final Set<EstadoCita> ACTIVOS = EnumSet.of(pendiente, confirmada);

    public boolean esActivo() {
        return ACTIVOS.contains(this);
    }

    public boolean puedeCambiarA(EstadoCita destino) {
        return switch (this) {
            case pendiente -> destino == confirmada || destino == completada || destino == cancelada;
            case confirmada -> destino == completada || destino == cancelada;
            case cancelada, completada -> false;
        };
    }
}
