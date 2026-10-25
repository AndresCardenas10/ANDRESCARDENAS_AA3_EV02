package com.sgb.negocio;

import com.sgb.dto.CitaSolicitudDTO;
import com.sgb.modelo.Cita;
import com.sgb.modelo.EstadoCita;
import com.sgb.modelo.Servicio;
import com.sgb.modelo.Usuario;
import com.sgb.repositorio.CitaRepository;
import com.sgb.repositorio.ServicioRepository;
import com.sgb.repositorio.UsuarioRepository;
import com.sgb.seguridad.Rol;
import com.sgb.seguridad.UsuarioSesion;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Reglas de negocio y permisos de CitaService con repositorios simulados
 * (Mockito): NO se conecta a ninguna base de datos, asi que es seguro
 * ejecutarla en cualquier maquina con "mvn test".
 */
@ExtendWith(MockitoExtension.class)
class CitaServiceTest {

    private static final ZoneId ZONA = ZoneId.of("America/Bogota");
    private static final LocalDateTime AHORA = LocalDateTime.of(2026, 10, 1, 8, 0);

    @Mock CitaRepository citaRepository;
    @Mock ServicioRepository servicioRepository;
    @Mock UsuarioRepository usuarioRepository;
    @Mock EntityManager entityManager;

    CitaService servicio;

    final UsuarioSesion admin = new UsuarioSesion(1, "Admin Demo", Rol.ADMINISTRADOR);
    final UsuarioSesion clienteLaura = new UsuarioSesion(6, "Laura Martinez", Rol.CLIENTE);
    final UsuarioSesion barberoCarlos = new UsuarioSesion(4, "Carlos Ramirez", Rol.BARBERO);

    Usuario laura, santiago, carlos;
    Servicio corte;

    @BeforeEach
    void preparar() {
        Clock reloj = Clock.fixed(AHORA.atZone(ZONA).toInstant(), ZONA);
        servicio = new CitaService(citaRepository, servicioRepository, usuarioRepository, entityManager, reloj);
        laura = usuario(6, Rol.CLIENTE);
        santiago = usuario(7, Rol.CLIENTE);
        carlos = usuario(4, Rol.BARBERO);
        corte = new Servicio();
        corte.setIdServicio(1);
        corte.setNombre("Corte de cabello");
        corte.setPrecio(new BigDecimal("20000"));
        corte.setDuracionMin(30);
        corte.setActivo(true);
        lenient().when(usuarioRepository.findById(6)).thenReturn(Optional.of(laura));
        lenient().when(usuarioRepository.findById(4)).thenReturn(Optional.of(carlos));
        lenient().when(usuarioRepository.bloquearPorId(4)).thenReturn(Optional.of(carlos));
        lenient().when(usuarioRepository.bloquearPorId(6)).thenReturn(Optional.of(laura));
        lenient().when(servicioRepository.findById(1)).thenReturn(Optional.of(corte));
        lenient().when(citaRepository.saveAndFlush(any(Cita.class))).thenAnswer(inv -> {
            Cita c = inv.getArgument(0);
            c.setIdCita(50);
            return c;
        });
        lenient().when(citaRepository.save(any(Cita.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void unClienteNoPuedeAgendarANombreDeOtro() {
        CitaSolicitudDTO s = solicitud(7, 4, AHORA.plusDays(1));
        assertThatThrownBy(() -> servicio.crear(s, clienteLaura))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN));
        verify(citaRepository, never()).saveAndFlush(any());
    }

    @Test
    void unClienteAgendaParaSiMismoYQuedaPendiente() {
        when(citaRepository.buscarHorariosActivosDelBarbero(anyInt(), any(), any())).thenReturn(List.of());
        var r = servicio.crear(solicitud(6, 4, AHORA.plusDays(1)), clienteLaura);
        assertThat(r.getEstado()).isEqualTo("pendiente");
        assertThat(r.getIdCliente()).isEqualTo(6);
        verify(entityManager).refresh(any(Cita.class));
    }

    @Test
    void rechazaFechasEnElPasado() {
        assertThatThrownBy(() -> servicio.crear(solicitud(6, 4, AHORA.minusMinutes(1)), admin))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST));
    }

    @Test
    void rechazaUnaCitaQueSeCruzaConOtraDelMismoBarbero() {
        when(citaRepository.buscarHorariosActivosDelBarbero(anyInt(), any(), any()))
                .thenReturn(List.of(horario(10, AHORA.plusDays(1), 30)));
        assertThatThrownBy(() -> servicio.crear(solicitud(6, 4, AHORA.plusDays(1).plusMinutes(15)), admin))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.CONFLICT));
    }

    @Test
    void permiteUnaCitaQueEmpiezaJustoCuandoTerminaLaAnterior() {
        when(citaRepository.buscarHorariosActivosDelBarbero(anyInt(), any(), any()))
                .thenReturn(List.of(horario(10, AHORA.plusDays(1), 30)));
        var r = servicio.crear(solicitud(6, 4, AHORA.plusDays(1).plusMinutes(30)), admin);
        assertThat(r.getIdCita()).isEqualTo(50);
    }

    @Test
    void rechazaQueElClienteSeaUnBarbero() {
        CitaSolicitudDTO s = solicitud(4, 4, AHORA.plusDays(1));
        assertThatThrownBy(() -> servicio.crear(s, admin))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        e -> assertThat(e.getReason()).contains("Cliente"));
    }

    @Test
    void completarUnaCitaCanceladaDevuelveConflicto() {
        Cita c = cita(11, laura, carlos, EstadoCita.cancelada, AHORA.plusDays(1));
        when(citaRepository.findById(11)).thenReturn(Optional.of(c));
        assertThatThrownBy(() -> servicio.completar(11, admin))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.CONFLICT));
        assertThat(c.getEstado()).isEqualTo(EstadoCita.cancelada);
    }

    @Test
    void elBarberoAsignadoConfirmaPeroOtroUsuarioNiSiquieraVeLaCita() {
        Cita c = cita(12, laura, carlos, EstadoCita.pendiente, AHORA.plusDays(1));
        when(citaRepository.findById(12)).thenReturn(Optional.of(c));
        assertThat(servicio.confirmar(12, barberoCarlos).getEstado()).isEqualTo("confirmada");

        Cita otra = cita(13, santiago, carlos, EstadoCita.pendiente, AHORA.plusDays(2));
        when(citaRepository.findById(13)).thenReturn(Optional.of(otra));
        assertThatThrownBy(() -> servicio.cancelar(13, clienteLaura))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));
    }

    @Test
    void soloElAdministradorListaTodasYElimina() {
        assertThatThrownBy(() -> servicio.listarTodas(clienteLaura)).isInstanceOf(ResponseStatusException.class);
        assertThatThrownBy(() -> servicio.eliminar(1, barberoCarlos)).isInstanceOf(ResponseStatusException.class);
        verify(citaRepository, never()).delete(any());
    }

    // ---- utilidades ----

    private static CitaRepository.HorarioCita horario(int id, LocalDateTime inicio, int duracion) {
        return new CitaRepository.HorarioCita() {
            public Integer getIdCita() { return id; }
            public LocalDateTime getFechaHora() { return inicio; }
            public Integer getDuracionMin() { return duracion; }
        };
    }

    private static Usuario usuario(int id, Rol rol) {
        Usuario u = new Usuario();
        u.setIdUsuario(id);
        u.setIdRol(rol.getIdRol());
        u.setNombre("Nombre" + id);
        u.setApellido("Apellido" + id);
        u.setCorreo("u" + id + "@test.local");
        u.setActivo(true);
        return u;
    }

    private Cita cita(int id, Usuario cliente, Usuario barbero, EstadoCita estado, LocalDateTime fecha) {
        Cita c = new Cita();
        c.setIdCita(id);
        c.setCliente(cliente);
        c.setBarbero(barbero);
        c.setServicio(corte);
        c.setEstado(estado);
        c.setFechaHora(fecha);
        return c;
    }

    private static CitaSolicitudDTO solicitud(int idCliente, int idBarbero, LocalDateTime fecha) {
        CitaSolicitudDTO s = new CitaSolicitudDTO();
        s.setIdCliente(idCliente);
        s.setIdBarbero(idBarbero);
        s.setIdServicio(1);
        s.setFechaHora(fecha);
        return s;
    }
}
