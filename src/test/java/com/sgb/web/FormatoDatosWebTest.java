package com.sgb.web;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.sgb.negocio.CitaService;
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
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Pruebas automatizadas de los mensajes de error cuando un dato llega con
 * el TIPO equivocado (Evidencia GA7-220501096-AA3-EV02): letras en un
 * numero, decimales en un entero, fechas mal escritas y caracteres
 * especiales. Verifican que la API responda 400 indicando el campo y el
 * formato esperado, sin llamar nunca a la logica de negocio.
 */
class FormatoDatosWebTest {

    private static final String TOKEN = "token-de-prueba";

    private MockMvc mvc;
    private ServicioService servicioService;
    private CitaService citaService;

    @BeforeEach
    void preparar() {
        // Misma configuracion de Jackson que application.properties.
        ObjectMapper mapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, true)
                .configure(DeserializationFeature.ACCEPT_FLOAT_AS_INT, false);
        servicioService = Mockito.mock(ServicioService.class);
        citaService = Mockito.mock(CitaService.class);
        LocalValidatorFactoryBean validador = new LocalValidatorFactoryBean();
        validador.afterPropertiesSet();
        mvc = MockMvcBuilders.standaloneSetup(new ServicioController(servicioService), new CitaController(citaService))
                .setControllerAdvice(new ManejadorErrores())
                .setMessageConverters(new MappingJackson2HttpMessageConverter(mapper))
                .setValidator(validador)
                .addFilters(new FiltroSeguridad(mapper))
                .build();
    }

    private static MockHttpSession sesion(Rol rol) {
        MockHttpSession s = new MockHttpSession();
        s.setAttribute(UsuarioSesion.ATRIBUTO, new UsuarioSesion(1, "Prueba", rol));
        s.setAttribute(AuthController.ATRIBUTO_CSRF, TOKEN);
        return s;
    }

    private static MockHttpServletRequestBuilder conSesion(MockHttpServletRequestBuilder peticion, String json) {
        return peticion.session(sesion(Rol.ADMINISTRADOR))
                .header(FiltroSeguridad.CABECERA_CSRF, TOKEN)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json);
    }

    @Test
    void letrasEnElPrecioIndicanQueSeEsperaUnNumero() throws Exception {
        mvc.perform(conSesion(post("/api/servicios"),
                        "{\"nombre\":\"Corte\",\"precio\":\"veinte mil\",\"duracionMin\":30}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos.precio").value(org.hamcrest.Matchers.containsString("numero")));
        Mockito.verifyNoInteractions(servicioService);
    }

    @Test
    void decimalesEnUnCampoEnteroSeRechazanEnVezDeRecortarse() throws Exception {
        mvc.perform(conSesion(post("/api/servicios"),
                        "{\"nombre\":\"Corte\",\"precio\":20000,\"duracionMin\":12.5}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos.duracionMin").value(org.hamcrest.Matchers.containsString("entero")));
        Mockito.verifyNoInteractions(servicioService);
    }

    @Test
    void unaFechaMalEscritaIndicaElFormatoEsperado() throws Exception {
        for (String fecha : new String[]{"31/12/2026 10:00", "2026-13-01T10:00:00", "2026-02-30T10:00:00", "manana"}) {
            mvc.perform(conSesion(post("/api/citas"),
                            "{\"idCliente\":4,\"idBarbero\":2,\"idServicio\":1,\"fechaHora\":\"" + fecha + "\"}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.campos.fechaHora").value(org.hamcrest.Matchers.containsString("AAAA-MM-DD")));
        }
        Mockito.verifyNoInteractions(citaService);
    }

    @Test
    void letrasEnUnIdIndicanQueSeEsperaUnEntero() throws Exception {
        mvc.perform(conSesion(put("/api/citas/5"),
                        "{\"idCliente\":4,\"idBarbero\":\"dos\",\"idServicio\":1,\"fechaHora\":\"2030-01-01T10:00:00\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos.idBarbero").value(org.hamcrest.Matchers.containsString("entero")));
        Mockito.verifyNoInteractions(citaService);
    }

    @Test
    void caracteresEspecialesEnElNombreDevuelvenElMensajeDelCampo() throws Exception {
        mvc.perform(conSesion(post("/api/servicios"),
                        "{\"nombre\":\"Corte@#$\",\"precio\":20000,\"duracionMin\":30}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos.nombre").value(org.hamcrest.Matchers.containsString("solo admite")));
        Mockito.verifyNoInteractions(servicioService);
    }
}
