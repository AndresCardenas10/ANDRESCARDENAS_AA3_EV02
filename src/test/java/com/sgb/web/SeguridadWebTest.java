package com.sgb.web;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.sgb.dto.ServicioSolicitudDTO;
import com.sgb.modelo.Servicio;
import com.sgb.negocio.ServicioService;
import com.sgb.seguridad.Rol;
import com.sgb.seguridad.UsuarioSesion;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Pruebas del filtro de seguridad y del manejo de errores con MockMvc en
 * modo "standalone": no levanta el contexto completo de Spring ni se
 * conecta a la base de datos (el servicio esta simulado con Mockito).
 */
class SeguridadWebTest {

    private MockMvc mvc;
    private ServicioService servicioService;
    private final ObjectMapper mapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, true);

    @BeforeEach
    void preparar() {
        servicioService = Mockito.mock(ServicioService.class);
        LocalValidatorFactoryBean validador = new LocalValidatorFactoryBean();
        validador.afterPropertiesSet();
        mvc = MockMvcBuilders.standaloneSetup(new ServicioController(servicioService))
                .setControllerAdvice(new ManejadorErrores())
                .setMessageConverters(new MappingJackson2HttpMessageConverter(mapper))
                .setValidator(validador)
                .addFilters(new FiltroSeguridad(mapper))
                .build();
    }

    private MockHttpSession sesion(Rol rol) {
        MockHttpSession s = new MockHttpSession();
        s.setAttribute(UsuarioSesion.ATRIBUTO, new UsuarioSesion(1, "Prueba", rol));
        s.setAttribute(AuthController.ATRIBUTO_CSRF, "token-de-prueba");
        return s;
    }

    private static final String SERVICIO_OK = "{\"nombre\":\"Corte\",\"precio\":20000,\"duracionMin\":30}";

    @Test
    void sinSesionResponde401ConCabecerasDeSeguridad() throws Exception {
        mvc.perform(get("/api/servicios"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string("X-Content-Type-Options", "nosniff"))
                .andExpect(header().string("X-Frame-Options", "DENY"))
                .andExpect(header().exists("X-Correlation-Id"));
    }

    @Test
    void escrituraSinTokenCsrfResponde403YNoLlegaAlServicio() throws Exception {
        mvc.perform(post("/api/servicios").session(sesion(Rol.ADMINISTRADOR))
                        .contentType(MediaType.APPLICATION_JSON).content(SERVICIO_OK))
                .andExpect(status().isForbidden());
        Mockito.verifyNoInteractions(servicioService);
    }

    @Test
    void escrituraConSesionYTokenCorrectoResponde201() throws Exception {
        Servicio creado = new Servicio();
        creado.setIdServicio(9);
        creado.setNombre("Corte");
        creado.setPrecio(new BigDecimal("20000"));
        creado.setDuracionMin(30);
        when(servicioService.crear(any(ServicioSolicitudDTO.class), any(UsuarioSesion.class))).thenReturn(creado);
        mvc.perform(post("/api/servicios").session(sesion(Rol.ADMINISTRADOR))
                        .header(FiltroSeguridad.CABECERA_CSRF, "token-de-prueba")
                        .contentType(MediaType.APPLICATION_JSON).content(SERVICIO_OK))
                .andExpect(status().isCreated());
    }

    @Test
    void camposNoPermitidosYJsonMalformadoResponden400() throws Exception {
        String conCampoExtra = "{\"nombre\":\"Corte\",\"precio\":20000,\"duracionMin\":30,\"idServicio\":1}";
        String respuesta = mvc.perform(post("/api/servicios").session(sesion(Rol.ADMINISTRADOR))
                        .header(FiltroSeguridad.CABECERA_CSRF, "token-de-prueba")
                        .contentType(MediaType.APPLICATION_JSON).content(conCampoExtra))
                .andExpect(status().isBadRequest())
                .andReturn().getResponse().getContentAsString();
        assertThat(respuesta).contains("idServicio").doesNotContain("Exception");

        mvc.perform(post("/api/servicios").session(sesion(Rol.ADMINISTRADOR))
                        .header(FiltroSeguridad.CABECERA_CSRF, "token-de-prueba")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"nombre\":"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void validacionDevuelveElDetallePorCampo() throws Exception {
        String invalido = "{\"nombre\":\" \",\"precio\":123456789,\"duracionMin\":0}";
        String respuesta = mvc.perform(post("/api/servicios").session(sesion(Rol.ADMINISTRADOR))
                        .header(FiltroSeguridad.CABECERA_CSRF, "token-de-prueba")
                        .contentType(MediaType.APPLICATION_JSON).content(invalido))
                .andExpect(status().isBadRequest())
                .andReturn().getResponse().getContentAsString();
        assertThat(respuesta).contains("\"nombre\"").contains("\"precio\"").contains("\"duracionMin\"");
    }

    @Test
    void parametroNoNumericoResponde400() throws Exception {
        mvc.perform(get("/api/servicios/abc").session(sesion(Rol.CLIENTE)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void cuerpoDeMasDe64KbResponde413() throws Exception {
        String enorme = "{\"nombre\":\"" + "x".repeat(70_000) + "\",\"precio\":1,\"duracionMin\":1}";
        mvc.perform(post("/api/servicios").session(sesion(Rol.ADMINISTRADOR))
                        .header(FiltroSeguridad.CABECERA_CSRF, "token-de-prueba")
                        .contentType(MediaType.APPLICATION_JSON).content(enorme))
                .andExpect(status().isPayloadTooLarge());
    }
}
