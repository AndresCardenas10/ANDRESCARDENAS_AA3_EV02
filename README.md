# Módulos Servicio y Cita — API REST (SGB)
Evidencia **GA7-220501096-AA3-EV01** — Codificación de módulos del software

> Nota sobre la referencia de esta evidencia: este README corresponde a
> **AA3-EV01**. (En la Evidencia anterior, AA2-EV02, el instructor señaló
> que en parte de la documentación seguía apareciendo "EV01" por error;
> aquí se revisó con cuidado que la numeración fuera consistente en todo
> el documento.)

## Qué hace este proyecto

Una **API REST** (con Spring Boot y Spring Data JPA) para los módulos
**Servicio** y **Cita** del proyecto Sistema Gestión Barbería (SGB):

| Módulo   | Operaciones                                                             |
|----------|--------------------------------------------------------------------------|
| Servicio | crear, listar (activos o todos), consultar por id, actualizar, desactivar |
| Cita     | crear, listar (todas / por cliente / por barbero), consultar por id, actualizar, confirmar, cancelar, completar, eliminar |

## Por qué un proyecto nuevo y no una modificación del proyecto Servlets/JSP

El proyecto ya entregado y calificado en la Evidencia **GA7-220501096-AA2-EV01/EV02**
(carpeta con `pom.xml` de tipo `war`) está construido con Servlets + JSP +
JDBC puro, sin ninguna dependencia de Spring. Para aplicar el framework
que se vio en las clases de apoyo (Spring Boot + Spring Data JPA) sin
arriesgar romper ese proyecto que ya funciona, esta evidencia se
construyó como un **proyecto Maven independiente**, dentro del mismo
repositorio, en su propia carpeta (`sgb-servicio-cita-api/`). Ambos
proyectos comparten la misma base de datos MySQL (`sgb`).

## Esquema de datos usado

Las entidades `Servicio` y `Cita` están mapeadas **exactamente** a las
tablas `servicio` y `cita` que ya existen en la base de datos real `sgb`
(ver `sql/sgb.sql` en la Evidencia AA2-EV02, que es un volcado literal de
esa base de datos):

- **`servicio`**: `id_servicio, nombre, descripcion, precio, duracion_min, activo`
- **`cita`**: `id_cita, id_cliente, id_barbero, id_servicio, fecha_hora, estado, expira_en, notas, creado_en`

El proyecto **no crea ni modifica** estas tablas (`spring.jpa.hibernate.ddl-auto=validate`):
solo se conecta a la base de datos que ya existe.

> Aclaración sobre el modelo de datos: durante el análisis del proyecto se
> generaron varias versiones del modelo (el diagrama de clases de la
> Evidencia GA4-220501095-AA2-EV04, con entidades Cliente/Barbero/Factura,
> y el modelo lógico inicial GA4-220501095-AA1-EV02). Ese diseño evolucionó
> en la práctica hacia una tabla `usuario` única con roles, sin módulo de
> facturación, que es la que realmente se creó y se está usando en la base
> de datos. Este proyecto sigue esa versión real para no generar errores
> de conexión, y retoma del diagrama de clases únicamente los nombres de
> los métodos de negocio (`agendar()`, `cancelar()`, `completar()`, etc.),
> que sí se pueden aplicar sin depender de la estructura de tablas.

## Estructura del proyecto

```
sgb-servicio-cita-api/
├── pom.xml
├── README.md                          (este archivo)
├── ENLACE_REPOSITORIO.txt
└── src/main/
    ├── java/com/sgb/
    │   ├── SgbServicioCitaApiApplication.java   (clase principal)
    │   ├── modelo/
    │   │   ├── Servicio.java
    │   │   ├── Cita.java
    │   │   ├── EstadoCita.java
    │   │   └── Usuario.java           (solo lectura; reutiliza la tabla del login)
    │   ├── repositorio/
    │   │   ├── ServicioRepository.java
    │   │   ├── CitaRepository.java
    │   │   └── UsuarioRepository.java
    │   ├── negocio/
    │   │   ├── ServicioService.java
    │   │   └── CitaService.java
    │   ├── dto/
    │   │   ├── CitaSolicitudDTO.java
    │   │   └── CitaRespuestaDTO.java
    │   └── web/
    │       ├── ServicioController.java
    │       └── CitaController.java
    └── resources/application.properties
```

**Convenciones de codificación aplicadas** (las mismas de la Evidencia
AA2-EV01/EV02): clases en `PascalCase`, métodos y variables en
`camelCase`, paquetes en minúscula.

## Endpoints disponibles

> Desde la auditoría de seguridad (octubre 2026) **todos los endpoints exigen
> haber iniciado sesión** y cada operación verifica el rol del usuario. Ver la
> sección "Seguridad" más abajo.

### Autenticación (nuevo)

| Método | Endpoint            | Qué hace                                                        |
|--------|---------------------|------------------------------------------------------------------|
| POST   | `/api/auth/login`   | `{"correo","contrasena"}` → crea la sesión y devuelve `csrfToken` |
| GET    | `/api/auth/sesion`  | Datos del usuario de la sesión actual (y su `csrfToken`)          |
| POST   | `/api/auth/logout`  | Cierra la sesión                                                 |

### Servicio

| Método | Endpoint                  | Qué hace                                   |
|--------|----------------------------|---------------------------------------------|
| GET    | `/api/servicios`           | Lista los servicios activos                 |
| GET    | `/api/servicios?todos=true`| Lista todos, incluidos los desactivados     |
| GET    | `/api/servicios/{id}`      | Consulta un servicio por id                 |
| POST   | `/api/servicios`           | Crea un servicio nuevo                      |
| PUT    | `/api/servicios/{id}`      | Actualiza un servicio                       |
| DELETE | `/api/servicios/{id}`      | Desactiva un servicio (no lo borra)         |

### Cita

| Método | Endpoint                        | Qué hace                                |
|--------|-----------------------------------|-------------------------------------------|
| GET    | `/api/citas`                      | Lista todas las citas                     |
| GET    | `/api/citas/{id}`                 | Consulta una cita por id                  |
| GET    | `/api/citas/cliente/{idCliente}`  | Historial de citas de un cliente          |
| GET    | `/api/citas/barbero/{idBarbero}`  | Agenda de citas de un barbero             |
| POST   | `/api/citas`                      | Agenda una cita nueva (queda "pendiente") |
| PUT    | `/api/citas/{id}`                 | Reprograma una cita                       |
| PUT    | `/api/citas/{id}/confirmar`       | Cambia el estado a "confirmada"           |
| PUT    | `/api/citas/{id}/cancelar`        | Cambia el estado a "cancelada"            |
| PUT    | `/api/citas/{id}/completar`       | Cambia el estado a "completada"           |
| DELETE | `/api/citas/{id}`                 | Elimina la cita de la base de datos       |

## Cómo ejecutarlo (resumen técnico)

La guía completa, paso a paso y sin dar por hecho ningún conocimiento
previo, está en el documento de la Etapa 4 de esta evidencia. En resumen:

1. Tener XAMPP con MySQL corriendo y la base `sgb` ya importada (la misma
   que se usa en la Evidencia AA2-EV02).
2. Abrir esta carpeta como proyecto Maven en NetBeans (u otro IDE).
3. Ejecutar la clase `SgbServicioCitaApiApplication`.
4. Abrir `http://localhost:8081/probador.html` en el navegador: es una
   página de prueba incluida en el proyecto (`src/main/resources/static/probador.html`)
   con botones y formularios para probar los endpoints de Servicio y
   Cita (crear, listar, cambiar estado, etc.) sin instalar Postman ni
   nada adicional. **Primero inicia sesión** en la sección "Sesión" con el
   correo y la contraseña de un usuario de la tabla `usuario`.

La API queda disponible en `http://localhost:8081`.

## Decisiones de diseño (y por qué)

- **Proyecto Spring Boot separado** del proyecto Servlets/JSP existente,
  para no arriesgar lo ya calificado (ver sección anterior).
- **Baja lógica (soft delete) en Servicio**: en vez de borrar la fila,
  `DELETE /api/servicios/{id}` pone `activo = false`. Borrar de verdad
  rompería las citas antiguas que ya usaron ese servicio (la base de
  datos ni lo permite: hay una restricción de llave foránea).
- **DTOs para Cita, entidad directa para Servicio**: `Cita` tiene
  relaciones (`@ManyToOne` a `Usuario` y `Servicio`) que Hibernate no
  carga automáticamente al convertir a JSON (`LazyInitializationException`),
  así que se usa un DTO que copia los datos ya resueltos. `Servicio` no
  tiene relaciones, así que no tiene ese problema y se usa la entidad
  directamente, sin una capa adicional innecesaria.
- **Validaciones de negocio en `CitaService`**: no se puede agendar una
  cita con un servicio desactivado, con un usuario que no exista, con un
  "barbero" que en realidad tenga otro rol, ni con una fecha en el
  pasado. Estas reglas ya estaban anotadas en el análisis del proyecto
  (Evidencia GA2-220501093-AA1-EV04, sección "Modelo de dominio").
- **`ddl-auto=validate` y no `update`**: la base de datos `sgb` ya existe
  con usuarios y roles de prueba reales; no se quiere que Hibernate
  intente alterarla sola.

## Pruebas ejecutadas y resultados

> Esta sección se completa después de ejecutar la guía de pruebas
> (Etapa 4 de esta evidencia) y se actualiza con los resultados reales
> antes de la entrega final — así se corrige, además, la observación
> puntual del instructor en la Evidencia AA2-EV02 sobre documentar en el
> README las pruebas ejecutadas y sus resultados.

| # | Endpoint probado | Resultado esperado | Resultado obtenido |
|---|-------------------|---------------------|----------------------|
| 1 | `GET /api/servicios` | Lista de servicios de prueba | *(pendiente)* |
| 2 | `POST /api/servicios` | Crea un servicio, responde 201 | *(pendiente)* |
| 3 | `POST /api/citas` | Agenda una cita, responde 201, estado "pendiente" | *(pendiente)* |
| 4 | `PUT /api/citas/{id}/confirmar` | Cambia el estado a "confirmada" | *(pendiente)* |
| 5 | `POST /api/citas` con id de servicio inexistente | Responde 404 con mensaje claro | *(pendiente)* |

## Seguridad (auditoría de octubre 2026)

### Qué cambió y por qué

| Antes | Ahora |
|-------|-------|
| La API no pedía autenticación: cualquiera podía leer, crear, modificar o borrar citas y cambiar precios. | Inicio de sesión (`/api/auth/login`) con la tabla `usuario`; sin sesión → **401**. |
| No había permisos por rol. | Permisos según RNF11 y las historias de usuario (tabla abajo); sin permiso → **403** (o **404** si la cita es de otra persona). |
| Contraseñas en texto plano. | BCrypt. Las contraseñas existentes en texto plano se **migran solas** al primer login correcto, sin pedir cambio. |
| Sin protección CSRF. | Las peticiones que modifican datos exigen la cabecera `X-CSRF-Token` (la devuelve el login) y la cookie es `SameSite=Strict` + `HttpOnly`. |
| Sin límite de intentos de login. | 5 fallos por correo (o 20 por IP) en 15 min → **429** temporal. |
| Se podía agendar dos citas del mismo barbero a la misma hora, completar una cita cancelada, usar un cliente como barbero, etc. | Validación de cruces de horario (con bloqueo en BD para peticiones simultáneas), transiciones de estado, roles y estado activo. |
| Errores previsibles terminaban en **500** y los mensajes de negocio no llegaban al cliente. | Respuestas JSON claras con `mensaje`, `campos` e `idCorrelacion`; nunca trazas ni SQL. |
| Credenciales escritas en `application.properties`, `show-sql=true`. | Variables de entorno con los mismos valores por defecto; `show-sql` apagado. |

### Permisos por rol

| Operación | Administrador | Barbero | Cliente |
|-----------|:---:|:---:|:---:|
| Listar servicios activos / consultar servicio | ✔ | ✔ | ✔ |
| `?todos=true`, crear, actualizar, desactivar servicio | ✔ | ✘ | ✘ |
| `GET /api/citas` (todas) / eliminar cita | ✔ | ✘ | ✘ |
| Consultar una cita | ✔ | si es suya | si es suya |
| `/cliente/{id}` | ✔ | ✘ | solo su id |
| `/barbero/{id}` | ✔ | solo su id | ✘ |
| Agendar (`POST /api/citas`) | ✔ | ✘ | solo a su nombre |
| Reprogramar (`PUT /api/citas/{id}`) | ✔ | ✘ | sus citas |
| Confirmar / completar | ✔ | sus citas | ✘ |
| Cancelar | ✔ | sus citas | sus citas |

### Configuración por variables de entorno

| Variable | Por defecto |
|----------|-------------|
| `SGB_DB_URL` | `jdbc:mysql://localhost:3307/sgb?...&serverTimezone=America/Bogota&connectTimeout=5000&socketTimeout=30000` |
| `SGB_DB_USER` | `root` |
| `SGB_DB_PASSWORD` | *(vacía)* |
| `SGB_SHOW_SQL` | `false` |

Recomendado: un usuario de base de datos propio con permisos mínimos (ejecutar
en phpMyAdmin como `root`, cambiando la contraseña):

```sql
CREATE USER 'sgb_app'@'localhost' IDENTIFIED BY 'cambie-esta-clave';
GRANT SELECT, INSERT, UPDATE, DELETE ON sgb.* TO 'sgb_app'@'localhost';
```

y luego, en NetBeans (Run → Set Project Configuration → Customize → Run →
Environment) o en la terminal: `SGB_DB_USER=sgb_app`, `SGB_DB_PASSWORD=...`.

### Pruebas automáticas

`mvn test` ejecuta 27 pruebas JUnit (`src/test/java`) que **no se conectan a
ninguna base de datos** (usan Mockito y MockMvc), así que es seguro correrlas
en cualquier máquina: contraseñas, límite de intentos, transiciones de estado,
reglas y permisos de `CitaService`, filtro de seguridad y manejo de errores.

### Ejemplo con curl

```bash
curl -c c.txt -H "Content-Type: application/json" \
     -d '{"correo":"usuario@correo.com","contrasena":"..."}' http://localhost:8081/api/auth/login
# copie el csrfToken de la respuesta
curl -b c.txt -H "Content-Type: application/json" -H "X-CSRF-Token: <csrfToken>" \
     -d '{"idCliente":6,"idBarbero":4,"idServicio":1,"fechaHora":"2026-12-01T10:00:00"}' http://localhost:8081/api/citas
```
