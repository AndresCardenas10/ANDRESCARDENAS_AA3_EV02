package com.sgb.dto;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pruebas automatizadas de las validaciones de los DTO (Evidencia
 * GA7-220501096-AA3-EV02): textos, caracteres especiales, longitudes,
 * numeros y correo. No usan base de datos ni levantan Spring: solo el
 * validador de Jakarta Bean Validation (Hibernate Validator).
 */
class ValidacionesDtoTest {

    private static ValidatorFactory fabrica;
    private static Validator validador;

    @BeforeAll
    static void crearValidador() {
        fabrica = Validation.buildDefaultValidatorFactory();
        validador = fabrica.getValidator();
    }

    @AfterAll
    static void cerrarValidador() {
        fabrica.close();
    }

    /** Devuelve los nombres de los campos que no pasaron la validacion. */
    private static <T> Set<String> camposInvalidos(T objeto) {
        Set<ConstraintViolation<T>> errores = validador.validate(objeto);
        return errores.stream().map(e -> e.getPropertyPath().toString()).collect(Collectors.toSet());
    }

    private static ServicioSolicitudDTO servicioValido() {
        ServicioSolicitudDTO s = new ServicioSolicitudDTO();
        s.setNombre("Corte y barba (clásico) - Ñoño");
        s.setDescripcion("Corte de cabello, arreglo de barba y toalla caliente.");
        s.setPrecio(new BigDecimal("30000.50"));
        s.setDuracionMin(45);
        return s;
    }

    private static CitaSolicitudDTO citaValida() {
        CitaSolicitudDTO c = new CitaSolicitudDTO();
        c.setIdCliente(4);
        c.setIdBarbero(2);
        c.setIdServicio(1);
        c.setFechaHora(LocalDateTime.of(2030, 1, 15, 10, 0));
        c.setNotas("Primera vez; prefiere tijera. ¡Gracias!");
        return c;
    }

    // ---------------- Servicio ----------------

    @Test
    void unServicioConTildesEnieYSignosPermitidosEsValido() {
        assertThat(camposInvalidos(servicioValido())).isEmpty();
    }

    @Test
    void elNombreRechazaCaracteresEspeciales() {
        for (String nombre : new String[]{"Corte@Premium", "Corte#1", "Corte<b>", "Corte $$$", "Corte; DROP TABLE"}) {
            ServicioSolicitudDTO s = servicioValido();
            s.setNombre(nombre);
            assertThat(camposInvalidos(s)).as("nombre '%s'", nombre).containsExactly("nombre");
        }
    }

    @Test
    void elNombreExigeEntre3Y100Caracteres() {
        ServicioSolicitudDTO corto = servicioValido();
        corto.setNombre("Co");
        assertThat(camposInvalidos(corto)).containsExactly("nombre");

        ServicioSolicitudDTO largo = servicioValido();
        largo.setNombre("a".repeat(101));
        assertThat(camposInvalidos(largo)).containsExactly("nombre");

        ServicioSolicitudDTO limite = servicioValido();
        limite.setNombre("a".repeat(100));
        assertThat(camposInvalidos(limite)).isEmpty();
    }

    @Test
    void elNombreVacioOSoloEspaciosEsObligatorio() {
        ServicioSolicitudDTO s = servicioValido();
        s.setNombre("   ");
        assertThat(camposInvalidos(s)).containsExactly("nombre");
    }

    @Test
    void laDescripcionRechazaCodigoHtmlYLlaves() {
        ServicioSolicitudDTO s = servicioValido();
        s.setDescripcion("<script>alert('hola')</script>");
        assertThat(camposInvalidos(s)).containsExactly("descripcion");

        s.setDescripcion("Precio {especial}");
        assertThat(camposInvalidos(s)).containsExactly("descripcion");
    }

    @Test
    void elPrecioDebeSerPositivoYConMaximoDosDecimales() {
        ServicioSolicitudDTO negativo = servicioValido();
        negativo.setPrecio(new BigDecimal("-5000"));
        assertThat(camposInvalidos(negativo)).containsExactly("precio");

        ServicioSolicitudDTO cero = servicioValido();
        cero.setPrecio(BigDecimal.ZERO);
        assertThat(camposInvalidos(cero)).containsExactly("precio");

        ServicioSolicitudDTO tresDecimales = servicioValido();
        tresDecimales.setPrecio(new BigDecimal("20000.555"));
        assertThat(camposInvalidos(tresDecimales)).containsExactly("precio");

        ServicioSolicitudDTO muyGrande = servicioValido();
        muyGrande.setPrecio(new BigDecimal("1000000"));
        assertThat(camposInvalidos(muyGrande)).containsExactly("precio");
    }

    @Test
    void laDuracionDebeEstarEntre1Y1440Minutos() {
        ServicioSolicitudDTO cero = servicioValido();
        cero.setDuracionMin(0);
        assertThat(camposInvalidos(cero)).containsExactly("duracionMin");

        ServicioSolicitudDTO excesiva = servicioValido();
        excesiva.setDuracionMin(1441);
        assertThat(camposInvalidos(excesiva)).containsExactly("duracionMin");
    }

    // ---------------- Cita ----------------

    @Test
    void unaCitaCompletaEsValida() {
        assertThat(camposInvalidos(citaValida())).isEmpty();
    }

    @Test
    void lasNotasRechazanCaracteresDeCodigoYMasDe1000Caracteres() {
        CitaSolicitudDTO conCodigo = citaValida();
        conCodigo.setNotas("<img src=x onerror=alert(1)>");
        assertThat(camposInvalidos(conCodigo)).containsExactly("notas");

        CitaSolicitudDTO larga = citaValida();
        larga.setNotas("x".repeat(CitaSolicitudDTO.MAX_NOTAS + 1));
        assertThat(camposInvalidos(larga)).containsExactly("notas");
    }

    @Test
    void losIdsYLaFechaSonObligatoriosYLosIdsPositivos() {
        CitaSolicitudDTO vacia = new CitaSolicitudDTO();
        assertThat(camposInvalidos(vacia))
                .containsExactlyInAnyOrder("idCliente", "idBarbero", "idServicio", "fechaHora");

        CitaSolicitudDTO negativa = citaValida();
        negativa.setIdServicio(-1);
        assertThat(camposInvalidos(negativa)).containsExactly("idServicio");
    }

    // ---------------- Login ----------------

    @Test
    void elCorreoDelLoginDebeTenerFormatoValido() {
        for (String correo : new String[]{"cliente", "cliente@", "cliente@sgb", "cli ente@sgb.com"}) {
            LoginSolicitudDTO login = new LoginSolicitudDTO();
            login.setCorreo(correo);
            login.setContrasena("Prueba2026*");
            assertThat(camposInvalidos(login)).as("correo '%s'", correo).containsExactly("correo");
        }
        LoginSolicitudDTO bien = new LoginSolicitudDTO();
        bien.setCorreo("cliente@sgb.com");
        bien.setContrasena("Prueba2026*");
        assertThat(camposInvalidos(bien)).isEmpty();
    }
}
