package com.sgb.modelo;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Immutable;

/**
 * Entidad de SOLO LECTURA para la tabla `usuario`, que ya existe y ya
 * tiene datos reales (creada por el modulo de Login de la Evidencia
 * GA7-220501096-AA2-EV01/EV02).
 *
 * No se crea aqui ningun ServicioController/Repository de escritura para
 * Usuario: ese modulo (login, registro, etc.) ya esta codificado y
 * probado en el proyecto Servlets+JSP. Esta clase existe UNICAMENTE para
 * que la entidad Cita pueda "apuntar" a un cliente y a un barbero reales
 * de esa misma tabla (relacion @ManyToOne), sin duplicar esa logica.
 *
 * Por eso no se mapean columnas sensibles como contrasena_hash aqui:
 * solo los campos que el modulo de Cita necesita mostrar o validar.
 * (El inicio de sesion lee el hash con una consulta aparte en
 * UsuarioRepository, sin pasar por esta entidad.)
 *
 * @Immutable: Hibernate nunca generara un UPDATE para esta entidad, aunque
 * algun codigo llegue a llamar a un setter por error.
 */
@Entity
@Immutable
@Table(name = "usuario")
@Data
@NoArgsConstructor
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_usuario")
    private Integer idUsuario;

    @Column(name = "id_rol", nullable = false)
    private Integer idRol;

    @Column(nullable = false, length = 100)
    private String nombre;

    @Column(nullable = false, length = 100)
    private String apellido;

    @Column(nullable = false, length = 150)
    private String correo;

    @Column(length = 20)
    private String telefono;

    @Column(nullable = false)
    private Boolean activo;

    /**
     * Nombre completo, util para mostrar en las respuestas de la API de
     * Cita (por ejemplo "cliente": "Laura Martinez") sin tener que armar
     * ese texto en cada controlador.
     */
    public String getNombreCompleto() {
        return nombre + " " + apellido;
    }
}
