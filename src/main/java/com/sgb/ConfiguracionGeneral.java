package com.sgb;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

/**
 * Beans de uso general.
 *
 * El reloj se inyecta (en vez de llamar LocalDateTime.now() directamente)
 * para que las pruebas automaticas puedan fijar "la hora actual" al
 * validar que una cita no quede en el pasado. En ejecucion normal usa la
 * zona horaria del sistema, igual que antes.
 */
@Configuration
public class ConfiguracionGeneral {

    @Bean
    public Clock reloj() {
        return Clock.systemDefaultZone();
    }
}
