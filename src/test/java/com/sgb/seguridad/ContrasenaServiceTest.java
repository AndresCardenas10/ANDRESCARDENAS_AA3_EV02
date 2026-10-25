package com.sgb.seguridad;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Pruebas unitarias del hash y la verificacion de contrasenas (no usan base de datos). */
class ContrasenaServiceTest {

    private final ContrasenaService servicio = new ContrasenaService();

    @Test
    void generaHashBcryptQueVerificaYNoContieneLaContrasena() {
        String hash = servicio.generarHash("Clave-Segura-123");
        assertThat(hash).startsWith("$2a$12$").hasSize(60).doesNotContain("Clave-Segura-123");
        assertThat(servicio.verificar("Clave-Segura-123", hash)).isTrue();
        assertThat(servicio.verificar("clave-segura-123", hash)).isFalse();
        assertThat(servicio.necesitaMigracion(hash)).isFalse();
    }

    @Test
    void dosHashesDeLaMismaContrasenaSonDistintos() {
        assertThat(servicio.generarHash("igual")).isNotEqualTo(servicio.generarHash("igual"));
    }

    @Test
    void aceptaContrasenaHeredadaEnTextoPlanoYPideMigrarla() {
        assertThat(servicio.verificar("texto-plano-1", "texto-plano-1")).isTrue();
        assertThat(servicio.verificar("texto-plano-2", "texto-plano-1")).isFalse();
        assertThat(servicio.necesitaMigracion("texto-plano-1")).isTrue();
    }

    @Test
    void valoresNulosVaciosOCorruptosNuncaAutentican() {
        assertThat(servicio.verificar(null, "x")).isFalse();
        assertThat(servicio.verificar("x", null)).isFalse();
        assertThat(servicio.verificar("x", "")).isFalse();
        assertThat(servicio.verificar("x", "$2a$12$" + "!".repeat(53))).isFalse();
    }

    @Test
    void rechazaContrasenasVaciasOMayoresA72Bytes() {
        assertThatThrownBy(() -> servicio.generarHash("")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> servicio.generarHash("a".repeat(73))).isInstanceOf(IllegalArgumentException.class);
    }
}
