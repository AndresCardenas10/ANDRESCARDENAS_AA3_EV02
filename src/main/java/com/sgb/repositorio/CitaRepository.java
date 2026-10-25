package com.sgb.repositorio;

import com.sgb.modelo.Cita;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Acceso a la tabla `cita`.
 *
 * Los nombres de estos metodos siguen la convencion de Spring Data JPA:
 * "findByCliente_IdUsuario" se traduce, sin escribir SQL, a
 * "SELECT * FROM cita WHERE id_cliente = ?". El guion bajo indica que
 * "IdUsuario" es un campo de la entidad relacionada (Usuario), no de Cita.
 *
 * @EntityGraph: trae cliente, barbero y servicio en la MISMA consulta
 * (JOIN). Antes, listar N citas hacia 1 consulta + hasta 3 consultas
 * extra por cita para armar el DTO (problema "N+1").
 */
public interface CitaRepository extends JpaRepository<Cita, Integer> {

    @Override
    @EntityGraph(attributePaths = {"cliente", "barbero", "servicio"})
    List<Cita> findAll();

    @EntityGraph(attributePaths = {"cliente", "barbero", "servicio"})
    List<Cita> findByCliente_IdUsuario(Integer idCliente);

    @EntityGraph(attributePaths = {"cliente", "barbero", "servicio"})
    List<Cita> findByBarbero_IdUsuario(Integer idBarbero);

    /** Datos minimos de una cita para calcular cruces de horario. */
    interface HorarioCita {
        Integer getIdCita();

        LocalDateTime getFechaHora();

        Integer getDuracionMin();
    }

    /**
     * Citas PENDIENTES o CONFIRMADAS de un barbero que empiezan dentro de la
     * ventana (desde, hasta), con la duracion de su servicio. Se usa para
     * detectar cruces de horario.
     *
     * Es una LECTURA CON BLOQUEO ("LOCK IN SHARE MODE", valido en MariaDB y
     * en MySQL): asi siempre lee la ultima version confirmada de la tabla y
     * no la "foto" que la transaccion tomo al empezar. Sin esto, dos
     * reservas simultaneas podian no verse entre si aunque esperaran el
     * bloqueo del barbero (se comprobo en las pruebas: 10 peticiones
     * simultaneas creaban 9 citas en el mismo horario). Consulta nativa
     * y parametrizada (sin concatenar datos).
     */
    @Query(value = "SELECT c.id_cita AS idCita, c.fecha_hora AS fechaHora, s.duracion_min AS duracionMin "
            + "FROM cita c INNER JOIN servicio s ON s.id_servicio = c.id_servicio "
            + "WHERE c.id_barbero = :idBarbero AND c.estado IN ('pendiente', 'confirmada') "
            + "AND c.fecha_hora > :desde AND c.fecha_hora < :hasta "
            + "LOCK IN SHARE MODE", nativeQuery = true)
    List<HorarioCita> buscarHorariosActivosDelBarbero(@Param("idBarbero") Integer idBarbero,
                                                      @Param("desde") LocalDateTime desde,
                                                      @Param("hasta") LocalDateTime hasta);
}
