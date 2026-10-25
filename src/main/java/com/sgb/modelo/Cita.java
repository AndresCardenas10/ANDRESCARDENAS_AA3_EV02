package com.sgb.modelo;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Entidad que representa una cita agendada entre un cliente y un barbero
 * para un servicio especifico.
 *
 * Las columnas coinciden con la tabla `cita` que ya existe en la base de
 * datos real `sgb` (ver sql/sgb.sql de la Evidencia AA2-EV02):
 * id_cita, id_cliente, id_barbero, id_servicio, fecha_hora, estado,
 * expira_en, notas, creado_en.
 *
 * En el proyecto Servlets/JSP de la Evidencia AA2-EV02, `CitaServlet`
 * maneja cada fila de la tabla `cita` como un simple Map<String,Object> y
 * escribe el SQL a mano (una decision de diseno documentada a proposito
 * en ese README, como version "basica"). Aqui, en cambio, se usa una
 * entidad JPA real con relaciones (@ManyToOne), que es la mejora natural
 * que ese mismo README dejaba planteada para una siguiente evidencia.
 */
@Entity
@Table(name = "cita")
@Data
@NoArgsConstructor
public class Cita {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_cita")
    private Integer idCita;

    /**
     * FetchType.LAZY: Hibernate NO trae los datos del cliente desde la
     * base de datos hasta que alguien realmente los pida (cita.getCliente()).
     * Evita cargar informacion de mas cuando solo se necesita, por
     * ejemplo, la fecha de la cita.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_cliente", nullable = false)
    private Usuario cliente;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_barbero", nullable = false)
    private Usuario barbero;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_servicio", nullable = false)
    private Servicio servicio;

    @Column(name = "fecha_hora", nullable = false)
    private LocalDateTime fechaHora;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EstadoCita estado = EstadoCita.pendiente;

    /**
     * Limite de tiempo para confirmar la cita antes de que expire.
     * Puede quedar en null (una cita no necesariamente expira).
     */
    @Column(name = "expira_en")
    private LocalDateTime expiraEn;

    @Column(columnDefinition = "TEXT")
    private String notas;

    /**
     * insertable=false, updatable=false: esta columna la llena MySQL solo
     * (DEFAULT current_timestamp() en sgb.sql), Java nunca la escribe ni
     * la actualiza a mano, solo la lee.
     */
    @Column(name = "creado_en", insertable = false, updatable = false)
    private LocalDateTime creadoEn;

    // ---- Metodos de negocio ----
    // Inspirados en los metodos que ya se habian definido para la clase
    // Cita en el diagrama de clases de la Evidencia GA4-220501095-AA2-EV04
    // (agendar(), cancelar(), completar()). Se adaptan aqui a los valores
    // reales del enum EstadoCita en vez de los nombres en mayuscula que
    // tenia ese diagrama, por la misma razon explicada en EstadoCita.java.
    //
    // Ahora cada metodo VALIDA la transicion (ver EstadoCita.puedeCambiarA):
    // si no es valida lanza TransicionInvalidaException y la cita no cambia.

    /** Pasa la cita de "pendiente" a "confirmada". */
    public void confirmar() {
        cambiarEstado(EstadoCita.confirmada);
    }

    /** Cancela la cita (no la borra de la base de datos). */
    public void cancelar() {
        cambiarEstado(EstadoCita.cancelada);
    }

    /** Marca la cita como atendida. */
    public void completar() {
        cambiarEstado(EstadoCita.completada);
    }

    private void cambiarEstado(EstadoCita destino) {
        EstadoCita actual = this.estado == null ? EstadoCita.pendiente : this.estado;
        if (!actual.puedeCambiarA(destino)) {
            throw new TransicionInvalidaException(actual, destino);
        }
        this.estado = destino;
    }

    /** Error de negocio: el cambio de estado pedido no esta permitido. */
    public static class TransicionInvalidaException extends RuntimeException {
        public TransicionInvalidaException(EstadoCita actual, EstadoCita destino) {
            super("No se puede pasar una cita de '" + actual + "' a '" + destino + "'");
        }
    }
}
