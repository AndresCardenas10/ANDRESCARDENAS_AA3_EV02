package com.sgb.web;

import com.sgb.dto.CitaRespuestaDTO;
import com.sgb.dto.CitaSolicitudDTO;
import com.sgb.negocio.CitaService;
import com.sgb.seguridad.UsuarioSesion;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Expone el modulo Cita como una API REST.
 *
 * Reutiliza el modulo de Usuario/login ya construido en la Evidencia
 * AA2-EV02 (solo lee de la tabla `usuario`, no la modifica) y el modulo
 * Servicio de esta misma evidencia.
 *
 * Las rutas, metodos HTTP y formatos de JSON son los mismos de antes. Lo
 * nuevo es que todas exigen una sesion iniciada (ver AuthController y
 * FiltroSeguridad) y que cada operacion verifica el rol del usuario en
 * CitaService. El usuario autenticado llega como atributo de la peticion
 * (lo coloca FiltroSeguridad), nunca desde datos que envie el cliente.
 */
@RestController
@RequestMapping("/api/citas")
@RequiredArgsConstructor
public class CitaController {

    private final CitaService citaService;

    /** GET /api/citas -> lista todas las citas (solo Administrador). */
    @GetMapping
    public List<CitaRespuestaDTO> listar(@RequestAttribute(UsuarioSesion.ATRIBUTO) UsuarioSesion actor) {
        return citaService.listarTodas(actor);
    }

    /** GET /api/citas/{id} */
    @GetMapping("/{id}")
    public CitaRespuestaDTO consultar(@PathVariable Integer id,
                                      @RequestAttribute(UsuarioSesion.ATRIBUTO) UsuarioSesion actor) {
        return citaService.buscarPorId(id, actor);
    }

    /** GET /api/citas/cliente/{idCliente} -> historial de citas de un cliente. */
    @GetMapping("/cliente/{idCliente}")
    public List<CitaRespuestaDTO> listarPorCliente(@PathVariable Integer idCliente,
                                                   @RequestAttribute(UsuarioSesion.ATRIBUTO) UsuarioSesion actor) {
        return citaService.listarPorCliente(idCliente, actor);
    }

    /** GET /api/citas/barbero/{idBarbero} -> agenda de citas de un barbero. */
    @GetMapping("/barbero/{idBarbero}")
    public List<CitaRespuestaDTO> listarPorBarbero(@PathVariable Integer idBarbero,
                                                   @RequestAttribute(UsuarioSesion.ATRIBUTO) UsuarioSesion actor) {
        return citaService.listarPorBarbero(idBarbero, actor);
    }

    /** POST /api/citas -> agenda una cita nueva (queda en estado "pendiente"). */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CitaRespuestaDTO crear(@Valid @RequestBody CitaSolicitudDTO solicitud,
                                  @RequestAttribute(UsuarioSesion.ATRIBUTO) UsuarioSesion actor) {
        return citaService.crear(solicitud, actor);
    }

    /** PUT /api/citas/{id} -> reprograma una cita (fecha, notas, etc.). */
    @PutMapping("/{id}")
    public CitaRespuestaDTO actualizar(@PathVariable Integer id, @Valid @RequestBody CitaSolicitudDTO solicitud,
                                       @RequestAttribute(UsuarioSesion.ATRIBUTO) UsuarioSesion actor) {
        return citaService.actualizar(id, solicitud, actor);
    }

    /** PUT /api/citas/{id}/confirmar -> pasa la cita a estado "confirmada". */
    @PutMapping("/{id}/confirmar")
    public CitaRespuestaDTO confirmar(@PathVariable Integer id,
                                      @RequestAttribute(UsuarioSesion.ATRIBUTO) UsuarioSesion actor) {
        return citaService.confirmar(id, actor);
    }

    /** PUT /api/citas/{id}/cancelar -> pasa la cita a estado "cancelada". */
    @PutMapping("/{id}/cancelar")
    public CitaRespuestaDTO cancelar(@PathVariable Integer id,
                                     @RequestAttribute(UsuarioSesion.ATRIBUTO) UsuarioSesion actor) {
        return citaService.cancelar(id, actor);
    }

    /** PUT /api/citas/{id}/completar -> pasa la cita a estado "completada". */
    @PutMapping("/{id}/completar")
    public CitaRespuestaDTO completar(@PathVariable Integer id,
                                      @RequestAttribute(UsuarioSesion.ATRIBUTO) UsuarioSesion actor) {
        return citaService.completar(id, actor);
    }

    /**
     * DELETE /api/citas/{id} -> borra la cita de verdad. Se deja
     * disponible por completitud (la guia pide la operacion "eliminar"),
     * pero para el uso normal del sistema es mejor usar
     * PUT /api/citas/{id}/cancelar. Solo Administrador.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Integer id,
                                         @RequestAttribute(UsuarioSesion.ATRIBUTO) UsuarioSesion actor) {
        citaService.eliminar(id, actor);
        return ResponseEntity.noContent().build();
    }
}
