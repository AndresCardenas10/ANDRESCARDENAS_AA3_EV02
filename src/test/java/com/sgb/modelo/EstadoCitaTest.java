package com.sgb.modelo;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Transiciones de estado de una cita (las mismas que usa la interfaz web). */
class EstadoCitaTest {

    @Test
    void transicionesPermitidas() {
        assertThat(EstadoCita.pendiente.puedeCambiarA(EstadoCita.confirmada)).isTrue();
        assertThat(EstadoCita.pendiente.puedeCambiarA(EstadoCita.completada)).isTrue();
        assertThat(EstadoCita.pendiente.puedeCambiarA(EstadoCita.cancelada)).isTrue();
        assertThat(EstadoCita.confirmada.puedeCambiarA(EstadoCita.completada)).isTrue();
        assertThat(EstadoCita.confirmada.puedeCambiarA(EstadoCita.cancelada)).isTrue();
    }

    @Test
    void estadosFinalesNoCambian() {
        for (EstadoCita destino : EstadoCita.values()) {
            assertThat(EstadoCita.cancelada.puedeCambiarA(destino)).isFalse();
            assertThat(EstadoCita.completada.puedeCambiarA(destino)).isFalse();
        }
        assertThat(EstadoCita.confirmada.puedeCambiarA(EstadoCita.pendiente)).isFalse();
    }

    @Test
    void laEntidadRechazaCompletarUnaCitaCanceladaYNoCambiaSuEstado() {
        Cita cita = new Cita();
        cita.cancelar();
        assertThatThrownBy(cita::completar).isInstanceOf(Cita.TransicionInvalidaException.class)
                .hasMessageContaining("cancelada");
        assertThat(cita.getEstado()).isEqualTo(EstadoCita.cancelada);
    }
}
