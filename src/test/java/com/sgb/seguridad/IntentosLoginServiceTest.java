package com.sgb.seguridad;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;

/** Limite de intentos fallidos de login, con un reloj controlado por la prueba. */
class IntentosLoginServiceTest {

    /** Reloj que la prueba puede adelantar. */
    static class RelojMovible extends Clock {
        Instant ahora = Instant.parse("2026-10-01T12:00:00Z");

        @Override public ZoneOffset getZone() { return ZoneOffset.UTC; }
        @Override public Clock withZone(java.time.ZoneId zone) { return this; }
        @Override public Instant instant() { return ahora; }
    }

    @Test
    void bloqueaElCorreoTrasCincoFallosYSeLiberaAlVencerLaVentana() {
        RelojMovible reloj = new RelojMovible();
        IntentosLoginService intentos = new IntentosLoginService(reloj);
        for (int i = 0; i < IntentosLoginService.MAX_FALLOS - 1; i++) {
            intentos.registrarFallo("Ana@Correo.com", "10.0.0.1");
        }
        assertThat(intentos.estaBloqueado("ana@correo.com", "10.0.0.2")).isFalse();
        intentos.registrarFallo("ana@correo.com", "10.0.0.1");
        // mismo correo (sin importar mayusculas) desde otra IP: bloqueado
        assertThat(intentos.estaBloqueado("ANA@correo.com", "10.0.0.2")).isTrue();
        // otro correo desde otra IP: libre
        assertThat(intentos.estaBloqueado("otro@correo.com", "10.0.0.3")).isFalse();

        reloj.ahora = reloj.ahora.plus(IntentosLoginService.VENTANA).plus(Duration.ofSeconds(1));
        assertThat(intentos.estaBloqueado("ana@correo.com", "10.0.0.2")).isFalse();
    }

    @Test
    void bloqueaUnaIpQueProbaMuchasCuentas() {
        IntentosLoginService intentos = new IntentosLoginService(new RelojMovible());
        for (int i = 0; i < IntentosLoginService.MAX_FALLOS_IP; i++) {
            intentos.registrarFallo("cuenta" + i + "@correo.com", "10.0.0.9");
        }
        assertThat(intentos.estaBloqueado("cuenta-nueva@correo.com", "10.0.0.9")).isTrue();
    }

    @Test
    void unLoginCorrectoLimpiaLosFallosDeEseCorreo() {
        IntentosLoginService intentos = new IntentosLoginService(new RelojMovible());
        for (int i = 0; i < IntentosLoginService.MAX_FALLOS; i++) {
            intentos.registrarFallo("ana@correo.com", "10.0.0.1");
        }
        intentos.registrarExito("ana@correo.com");
        assertThat(intentos.estaBloqueado("ana@correo.com", "10.0.0.1")).isFalse();
    }
}
