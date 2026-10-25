package com.sgb.negocio;

import com.sgb.dto.CitaRespuestaDTO;
import com.sgb.dto.CitaSolicitudDTO;
import com.sgb.modelo.Cita;
import com.sgb.modelo.EstadoCita;
import com.sgb.modelo.Servicio;
import com.sgb.modelo.Usuario;
import com.sgb.repositorio.CitaRepository;
import com.sgb.repositorio.ServicioRepository;
import com.sgb.repositorio.UsuarioRepository;
import com.sgb.seguridad.ControlAcceso;
import com.sgb.seguridad.Rol;
import com.sgb.seguridad.UsuarioSesion;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.function.Consumer;

/**
 * Logica de negocio del modulo Cita.
 *
 * Aqui es donde se valida todo lo que el simple CRUD no puede garantizar
 * por si solo: que el cliente, el barbero y el servicio existan, que el
 * barbero de verdad tenga el rol de Barbero, y que la fecha de la cita no
 * sea en el pasado. Estas reglas ya estaban documentadas como parte del
 * analisis del proyecto (ver Evidencia GA2-220501093-AA1-EV04, seccion
 * "Modelo de dominio").
 *
 * Reglas agregadas en la auditoria (todas con respaldo en el proyecto):
 *  - el cliente debe tener rol Cliente y el barbero rol Barbero, ambos
 *    activos (el formulario de citas del modulo Servlets/JSP solo ofrece
 *    usuarios activos de esos roles);
 *  - un barbero no puede tener dos citas pendientes/confirmadas que se
 *    crucen en el tiempo, usando la duracion del servicio (el modelo
 *    entidad-relacion indica "validar conflictos de agenda", RF09);
 *  - los cambios de estado siguen las transiciones de EstadoCita;
 *  - solo se reprograman citas pendientes o confirmadas.
 *
 * Permisos por rol (RNF11; historias HU03, HU07, HU10 y los paneles web):
 *  - Administrador: todo.
 *  - Cliente: agenda citas PARA SI MISMO, ve su historial, reprograma y
 *    cancela sus propias citas.
 *  - Barbero: ve su agenda y confirma, completa o cancela las citas que
 *    tiene asignadas.
 *  Para no revelar que existe una cita ajena, consultarla devuelve 404.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class CitaService {

    /** Ventana hacia atras para buscar cruces: ningun servicio dura mas de 24 h (ver ServicioSolicitudDTO). */
    private static final int MAX_DURACION_MIN = 24 * 60;

    private final CitaRepository citaRepository;
    private final ServicioRepository servicioRepository;
    private final UsuarioRepository usuarioRepository;
    private final EntityManager entityManager;
    private final Clock reloj;

    // ------------------------------------------------------------------
    // Consultas
    // ------------------------------------------------------------------

    @Transactional(readOnly = true)
    public List<CitaRespuestaDTO> listarTodas(UsuarioSesion actor) {
        ControlAcceso.exigirAdministrador(actor);
        return citaRepository.findAll().stream()
                .map(CitaRespuestaDTO::desde)
                .toList();
    }

    @Transactional(readOnly = true)
    public CitaRespuestaDTO buscarPorId(Integer id, UsuarioSesion actor) {
        Cita cita = buscarEntidadPorId(id);
        if (!puedeVer(actor, cita)) {
            throw noExiste(id);
        }
        return CitaRespuestaDTO.desde(cita);
    }

    /** Todas las citas de un cliente especifico (su "historial"). */
    @Transactional(readOnly = true)
    public List<CitaRespuestaDTO> listarPorCliente(Integer idCliente, UsuarioSesion actor) {
        ControlAcceso.exigir(actor.esAdministrador() || (actor.esCliente() && actor.esElUsuario(idCliente)));
        return citaRepository.findByCliente_IdUsuario(idCliente).stream()
                .map(CitaRespuestaDTO::desde)
                .toList();
    }

    /** Todas las citas asignadas a un barbero especifico (su "agenda"). */
    @Transactional(readOnly = true)
    public List<CitaRespuestaDTO> listarPorBarbero(Integer idBarbero, UsuarioSesion actor) {
        ControlAcceso.exigir(actor.esAdministrador() || (actor.esBarbero() && actor.esElUsuario(idBarbero)));
        return citaRepository.findByBarbero_IdUsuario(idBarbero).stream()
                .map(CitaRespuestaDTO::desde)
                .toList();
    }

    // ------------------------------------------------------------------
    // Escrituras
    // ------------------------------------------------------------------

    public CitaRespuestaDTO crear(CitaSolicitudDTO solicitud, UsuarioSesion actor) {
        // Un cliente solo puede agendar a su propio nombre; el barbero no agenda (HU03).
        ControlAcceso.exigir(actor.esAdministrador()
                || (actor.esCliente() && actor.esElUsuario(solicitud.getIdCliente())));

        // Primero se bloquea al barbero: las reservas simultaneas de su
        // agenda quedan en fila y se validan una despues de otra.
        Usuario barbero = bloquearBarbero(solicitud.getIdBarbero());
        Usuario cliente = buscarUsuarioConRol(solicitud.getIdCliente(), Rol.CLIENTE, "cliente");
        Servicio servicio = buscarServicioActivo(solicitud.getIdServicio());
        validarFechaFutura(solicitud.getFechaHora());
        validarSinCruce(barbero.getIdUsuario(), solicitud.getFechaHora(), servicio, null);

        Cita cita = new Cita();
        cita.setCliente(cliente);
        cita.setBarbero(barbero);
        cita.setServicio(servicio);
        cita.setFechaHora(solicitud.getFechaHora());
        cita.setNotas(solicitud.getNotas());
        cita.setEstado(EstadoCita.pendiente);

        Cita guardada = citaRepository.saveAndFlush(cita);
        // Relee la fila para traer creado_en (lo llena MySQL con DEFAULT
        // current_timestamp); antes la respuesta de creacion lo devolvia null.
        entityManager.refresh(guardada);
        return CitaRespuestaDTO.desde(guardada);
    }

    public CitaRespuestaDTO actualizar(Integer id, CitaSolicitudDTO solicitud, UsuarioSesion actor) {
        Cita cita = buscarEntidadPorId(id);
        if (!puedeVer(actor, cita)) {
            throw noExiste(id);
        }
        // Reprogramar: Administrador, o el cliente duenio de la cita (HU10),
        // que ademas no puede pasarle la cita a otro cliente.
        ControlAcceso.exigir(actor.esAdministrador()
                || (actor.esCliente() && actor.esElUsuario(cita.getCliente().getIdUsuario())
                    && actor.esElUsuario(solicitud.getIdCliente())));
        if (!cita.getEstado().esActivo()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Solo se pueden reprogramar citas pendientes o confirmadas (estado actual: '"
                            + cita.getEstado() + "')");
        }

        Usuario barbero = bloquearBarbero(solicitud.getIdBarbero());
        Usuario cliente = buscarUsuarioConRol(solicitud.getIdCliente(), Rol.CLIENTE, "cliente");
        Servicio servicio = buscarServicioActivo(solicitud.getIdServicio());
        validarFechaFutura(solicitud.getFechaHora());
        validarSinCruce(barbero.getIdUsuario(), solicitud.getFechaHora(), servicio, cita.getIdCita());

        cita.setCliente(cliente);
        cita.setBarbero(barbero);
        cita.setServicio(servicio);
        cita.setFechaHora(solicitud.getFechaHora());
        cita.setNotas(solicitud.getNotas());

        return CitaRespuestaDTO.desde(citaRepository.save(cita));
    }

    public CitaRespuestaDTO confirmar(Integer id, UsuarioSesion actor) {
        return cambiarEstado(id, actor, false, Cita::confirmar);
    }

    public CitaRespuestaDTO cancelar(Integer id, UsuarioSesion actor) {
        // Cancelar tambien lo puede hacer el cliente duenio (panel del cliente).
        return cambiarEstado(id, actor, true, Cita::cancelar);
    }

    public CitaRespuestaDTO completar(Integer id, UsuarioSesion actor) {
        return cambiarEstado(id, actor, false, Cita::completar);
    }

    /**
     * Elimina la fila de la base de datos de verdad. Se deja disponible
     * porque la guia de la actividad pide las 4 operaciones basicas
     * (insertar, consultar, actualizar, eliminar), pero en la practica es
     * preferible usar cancelar() para no perder el historial de la cita.
     * Solo el Administrador puede hacerlo.
     */
    public void eliminar(Integer id, UsuarioSesion actor) {
        ControlAcceso.exigirAdministrador(actor);
        Cita cita = buscarEntidadPorId(id);
        citaRepository.delete(cita);
    }

    // ---- Apoyo ----

    private CitaRespuestaDTO cambiarEstado(Integer id, UsuarioSesion actor, boolean clientePuede,
                                           Consumer<Cita> transicion) {
        Cita cita = buscarEntidadPorId(id);
        if (!puedeVer(actor, cita)) {
            throw noExiste(id);
        }
        boolean esSuBarbero = actor.esBarbero() && actor.esElUsuario(cita.getBarbero().getIdUsuario());
        boolean esSuCliente = actor.esCliente() && actor.esElUsuario(cita.getCliente().getIdUsuario());
        ControlAcceso.exigir(actor.esAdministrador() || esSuBarbero || (clientePuede && esSuCliente));
        try {
            transicion.accept(cita);
        } catch (Cita.TransicionInvalidaException e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, e.getMessage());
        }
        return CitaRespuestaDTO.desde(citaRepository.save(cita));
    }

    private boolean puedeVer(UsuarioSesion actor, Cita cita) {
        return actor.esAdministrador()
                || (actor.esCliente() && actor.esElUsuario(cita.getCliente().getIdUsuario()))
                || (actor.esBarbero() && actor.esElUsuario(cita.getBarbero().getIdUsuario()));
    }

    private Cita buscarEntidadPorId(Integer id) {
        return citaRepository.findById(id).orElseThrow(() -> noExiste(id));
    }

    private static ResponseStatusException noExiste(Integer id) {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, "No existe una cita con id " + id);
    }

    // ---- Validaciones privadas ----

    private Usuario buscarUsuarioConRol(Integer id, Rol rolEsperado, String rolTexto) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "No existe un usuario (" + rolTexto + ") con id " + id));
        validarRolYActivo(usuario, rolEsperado, rolTexto);
        return usuario;
    }

    /**
     * Busca el barbero BLOQUEANDO su fila (SELECT ... FOR UPDATE) hasta el
     * final de la transaccion: dos reservas simultaneas para el mismo
     * barbero se atienden una detras de otra, de modo que la validacion de
     * cruce de horario no puede ser "saltada" por una condicion de carrera.
     */
    private Usuario bloquearBarbero(Integer id) {
        Usuario barbero = usuarioRepository.bloquearPorId(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "No existe un usuario (barbero) con id " + id));
        validarRolYActivo(barbero, Rol.BARBERO, "barbero");
        return barbero;
    }

    private static void validarRolYActivo(Usuario usuario, Rol rolEsperado, String rolTexto) {
        if (rolEsperado.getIdRol() != (usuario.getIdRol() == null ? -1 : usuario.getIdRol())) {
            String detalle = rolEsperado == Rol.BARBERO
                    ? " no tiene el rol de Barbero, no se le pueden asignar citas"
                    : " no tiene el rol de Cliente";
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "El usuario con id " + usuario.getIdUsuario() + detalle);
        }
        if (!Boolean.TRUE.equals(usuario.getActivo())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "El " + rolTexto + " con id " + usuario.getIdUsuario() + " esta inactivo");
        }
    }

    private Servicio buscarServicioActivo(Integer id) {
        Servicio servicio = servicioRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "No existe un servicio con id " + id));
        if (!Boolean.TRUE.equals(servicio.getActivo())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "El servicio con id " + id + " esta desactivado, no se puede agendar");
        }
        return servicio;
    }

    private void validarFechaFutura(LocalDateTime fechaHora) {
        if (fechaHora.isBefore(LocalDateTime.now(reloj))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "La fecha y hora de la cita no puede ser en el pasado");
        }
    }

    /** Rechaza la cita si se cruza con otra pendiente/confirmada del mismo barbero. */
    private void validarSinCruce(Integer idBarbero, LocalDateTime inicio, Servicio servicio, Integer idCitaExcluida) {
        LocalDateTime fin = inicio.plusMinutes(servicio.getDuracionMin());
        List<CitaRepository.HorarioCita> cercanas = citaRepository.buscarHorariosActivosDelBarbero(
                idBarbero, inicio.minusMinutes(MAX_DURACION_MIN), fin);
        for (CitaRepository.HorarioCita otra : cercanas) {
            if (otra.getIdCita().equals(idCitaExcluida)) {
                continue;
            }
            LocalDateTime otraFin = otra.getFechaHora().plusMinutes(otra.getDuracionMin());
            boolean seCruzan = inicio.isBefore(otraFin) && otra.getFechaHora().isBefore(fin);
            if (seCruzan) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "El barbero ya tiene una cita en ese horario (" + otra.getFechaHora()
                                + " a " + otraFin.toLocalTime() + ")");
            }
        }
    }
}
