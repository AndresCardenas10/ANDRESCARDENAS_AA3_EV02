package com.sgb.repositorio;

import com.sgb.modelo.Usuario;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

/**
 * Acceso a la tabla `usuario`.
 *
 * La entidad Usuario sigue siendo de SOLO LECTURA (no se crean ni se
 * editan usuarios desde esta API). Se agregan unicamente las consultas
 * que necesita el inicio de sesion:
 *  - buscar por correo;
 *  - leer el hash de la contrasena SIN mapearlo en la entidad (asi nunca
 *    puede terminar serializado en una respuesta JSON);
 *  - reemplazar una contrasena heredada en texto plano por su hash
 *    BCrypt (migracion gradual en el primer login correcto).
 * Todas son consultas parametrizadas (sin concatenar datos del usuario).
 */
public interface UsuarioRepository extends JpaRepository<Usuario, Integer> {

    Optional<Usuario> findByCorreo(String correo);

    @Query(value = "SELECT contrasena_hash FROM usuario WHERE id_usuario = :id", nativeQuery = true)
    Optional<String> buscarHashContrasena(@Param("id") Integer idUsuario);

    /**
     * Actualiza el hash solo si el valor almacenado sigue siendo el que se
     * verifico (evita pisar un cambio concurrente). Devuelve las filas
     * afectadas (0 o 1).
     */
    @Modifying
    @Query(value = "UPDATE usuario SET contrasena_hash = :nuevo "
            + "WHERE id_usuario = :id AND contrasena_hash = :anterior", nativeQuery = true)
    int reemplazarHashContrasena(@Param("id") Integer idUsuario,
                                 @Param("anterior") String hashAnterior,
                                 @Param("nuevo") String hashNuevo);

    /**
     * SELECT ... FOR UPDATE sobre la fila del barbero. Se usa al agendar o
     * reprogramar para que dos peticiones simultaneas no puedan reservar
     * el mismo horario del mismo barbero (la segunda espera a que termine
     * la primera y entonces ve la cita ya creada).
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select u from Usuario u where u.idUsuario = :id")
    Optional<Usuario> bloquearPorId(@Param("id") Integer idUsuario);
}
