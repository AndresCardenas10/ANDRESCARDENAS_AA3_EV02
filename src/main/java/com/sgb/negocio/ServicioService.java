package com.sgb.negocio;

import com.sgb.dto.ServicioSolicitudDTO;
import com.sgb.modelo.Servicio;
import com.sgb.repositorio.ServicioRepository;
import com.sgb.seguridad.ControlAcceso;
import com.sgb.seguridad.UsuarioSesion;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

/**
 * Logica de negocio del modulo Servicio. El controlador (ServicioController)
 * NO habla directamente con el repositorio: siempre pasa por esta clase,
 * que es la unica responsable de decidir que es una operacion valida.
 *
 * Permisos (RNF11, y la interfaz web donde solo el administrador gestiona
 * servicios):
 *  - consultar servicios activos: cualquier usuario autenticado;
 *  - ver tambien los desactivados, crear, actualizar y desactivar: solo
 *    Administrador.
 */
@Service
@RequiredArgsConstructor // Lombok genera el constructor con este campo final -> Spring lo inyecta solo.
@Transactional
public class ServicioService {

    private final ServicioRepository servicioRepository;

    @Transactional(readOnly = true)
    public List<Servicio> listar(boolean incluirInactivos, UsuarioSesion actor) {
        if (incluirInactivos) {
            ControlAcceso.exigirAdministrador(actor);
            return servicioRepository.findAll();
        }
        return servicioRepository.findByActivoTrue();
    }

    @Transactional(readOnly = true)
    public Servicio buscarPorId(Integer id) {
        return servicioRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "No existe un servicio con id " + id));
    }

    public Servicio crear(ServicioSolicitudDTO datos, UsuarioSesion actor) {
        ControlAcceso.exigirAdministrador(actor);
        // El id lo genera la base de datos (AUTO_INCREMENT): el DTO ni
        // siquiera tiene campo id, asi que no se puede sobrescribir un
        // servicio existente desde la peticion.
        Servicio nuevo = new Servicio();
        copiar(datos, nuevo);
        nuevo.setActivo(datos.getActivo() == null ? Boolean.TRUE : datos.getActivo());
        return servicioRepository.save(nuevo);
    }

    public Servicio actualizar(Integer id, ServicioSolicitudDTO datos, UsuarioSesion actor) {
        ControlAcceso.exigirAdministrador(actor);
        Servicio existente = buscarPorId(id);
        copiar(datos, existente);
        if (datos.getActivo() != null) {
            existente.setActivo(datos.getActivo());
        }
        return servicioRepository.save(existente);
    }

    /**
     * "Elimina" un servicio SIN borrar la fila de la base de datos:
     * solo lo marca como inactivo (activo = false). Si se borrara de
     * verdad, cualquier cita antigua que ya haya usado ese servicio
     * quedaria con una referencia rota (id_servicio apuntando a nada), lo
     * que MySQL ademas impide por la restriccion de llave foranea
     * fk_cita_servicio definida en sgb.sql.
     */
    public void desactivar(Integer id, UsuarioSesion actor) {
        ControlAcceso.exigirAdministrador(actor);
        Servicio existente = buscarPorId(id);
        existente.setActivo(false);
        servicioRepository.save(existente);
    }

    private static void copiar(ServicioSolicitudDTO datos, Servicio destino) {
        destino.setNombre(datos.getNombre().trim());
        destino.setDescripcion(datos.getDescripcion() == null ? null : datos.getDescripcion().trim());
        destino.setPrecio(datos.getPrecio());
        destino.setDuracionMin(datos.getDuracionMin());
    }
}
