# Módulos Servicio y Cita — API REST (SGB)
Evidencia **GA7-220501096-AA3-EV02** — Módulos de software codificados y probados

Repositorio: https://github.com/AndresCardenas10/ANDRESCARDENAS_AA3_EV02

> **Historial de este proyecto.** El código se entregó por primera vez en la
> Evidencia **GA7-220501096-AA3-EV01** (repositorio
> `ANDRESCARDENAS_AA3_EV01`). En esta evidencia (**AA3-EV02**) se conserva ese
> historial de commits y se agregan: la auditoría de seguridad (inicio de
> sesión, roles, CSRF), validaciones de caracteres especiales, longitudes y
> formatos, el panel web completo del módulo, una base de datos limpia de
> pruebas, **43 pruebas automatizadas JUnit** y una **colección de Postman con
> 56 peticiones y 117 pruebas automáticas**, que atienden la recomendación del
> instructor en AA3-EV01: *"incorpora pruebas automatizadas y ejemplos de
> consumo de los endpoints"*.

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
(la estructura es la del volcado `sgb.sql` de la Evidencia AA2-EV02; en esta
evidencia se entrega `sql/sgb_limpio.sql`, con la misma estructura y datos de
prueba en lugar de los datos reales):

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
├── sql/sgb_limpio.sql                 (base de datos de prueba, sin datos reales)
├── postman/SGB_Servicio_Cita_AA3_EV02.postman_collection.json
├── src/test/java/...                  (43 pruebas JUnit)
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
    │   ├── dto/                       (datos de entrada/salida + ValidacionesTexto)
    │   ├── seguridad/                 (login, contraseñas BCrypt, roles, intentos)
    │   └── web/                       (controladores REST, filtro de seguridad, errores)
    └── resources/
        ├── application.properties
        └── static/probador.html       (panel web del módulo)
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

1. XAMPP con MySQL/MariaDB en el puerto **3307**.
2. Importar `sql/sgb_limpio.sql` en phpMyAdmin (pestaña **Importar**).
   **Ojo:** el script borra y vuelve a crear la base `sgb`; exporte antes una
   copia de la base actual si la necesita.
3. Abrir esta carpeta como proyecto Maven en NetBeans (JDK 17 o superior) y
   ejecutar `SgbServicioCitaApiApplication` (o `mvn spring-boot:run`).
4. Abrir `http://localhost:8081/probador.html` e iniciar sesión con un
   usuario de prueba.

### Usuarios de prueba (contraseña de todos: `Prueba2026*`)

| id | Correo | Nombre | Rol |
|----|--------|--------|-----|
| 1 | admin@sgb.com | Laura Gómez | Administrador |
| 2 | barbero@sgb.com | Juan Pérez | Barbero |
| 3 | barbero2@sgb.com | Diego Ramírez | Barbero |
| 4 | cliente@sgb.com | María López | Cliente |
| 5 | cliente2@sgb.com | Sofía Torres | Cliente |
| 6 | inactivo@sgb.com | Carlos Ruiz | Barbero **inactivo** (para probar validaciones) |

Servicios: 1 a 4 activos; el **5 (Mascarilla facial) está desactivado** a
propósito. Las citas de ejemplo se crean con fechas relativas al día de la
importación.

### Panel web del módulo (`probador.html`)

Cada sección corresponde a una historia de usuario: **RF03** agendar,
**RF08** agenda del barbero / historial del cliente, **RF11** reprogramar,
**RF09** confirmar (y completar, cancelar, eliminar), además del catálogo de
servicios. El panel muestra, junto a cada acción, la **petición enviada** y la
**respuesta JSON** de la API, y no bloquea datos inválidos para que se vea la
validación que hace el servidor.

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

Ejecutadas el 25 de octubre de 2026 contra MariaDB con `sql/sgb_limpio.sql`.

| Tipo | Herramienta | Resultado |
|------|-------------|-----------|
| Pruebas unitarias y de controlador | JUnit 5 + Mockito + MockMvc (`mvn test`) | **43 de 43 aprobadas** |
| Pruebas de la API funcionando | Postman / Newman (`postman/…collection.json`) | **56 peticiones, 117 de 117 aserciones aprobadas** |
| Pruebas desde la interfaz | Panel `probador.html` | 20 pantallazos de historias + 25 de validaciones (documento Word) |

| # | Prueba | Esperado | Obtenido |
|---|--------|----------|----------|
| 1 | `POST /api/auth/login` con contraseña incorrecta | 401 | 401 ✔ |
| 2 | `GET /api/servicios` (servicios activos) | 200, solo activos | 200 ✔ |
| 3 | `POST /api/servicios` válido | 201, activo | 201 ✔ |
| 4 | Nombre de servicio `Corte@Premium#1` | 400, campo `nombre` | 400 ✔ |
| 5 | Precio `"veinte mil"` / `-5000` / `20000.555` | 400, campo `precio` | 400 ✔ |
| 6 | Duración `12.5` / `1441` | 400, campo `duracionMin` | 400 ✔ |
| 7 | Descripción `<script>…</script>` | 400, campo `descripcion` | 400 ✔ |
| 8 | **RF03** `POST /api/citas` válido (cliente) | 201, `pendiente` | 201 ✔ |
| 9 | Fecha en el pasado | 400 | 400 ✔ |
| 10 | Fecha `31/12/2026 10:00` / `2027-02-30T10:00` | 400, campo `fechaHora` | 400 ✔ |
| 11 | Servicio inexistente / desactivado / barbero inactivo | 404 / 400 / 400 | ✔ |
| 12 | Notas de 1001 caracteres / con `< >` | 400, campo `notas` | 400 ✔ |
| 13 | Cliente agenda a nombre de otro | 403 | 403 ✔ |
| 14 | **RF11** `PUT /api/citas/{id}` (reprogramar) | 200, nueva fecha | 200 ✔ |
| 15 | Otra cita del mismo barbero a la misma hora | 409 | 409 ✔ |
| 16 | **RF08** `GET /api/citas/barbero/2` (barbero) | 200, solo sus citas | 200 ✔ |
| 17 | **RF09** `PUT /api/citas/{id}/confirmar` | 200, `confirmada` | 200 ✔ |
| 18 | Confirmar / reprogramar una cita completada | 409 | 409 ✔ |
| 19 | Escritura sin cabecera `X-CSRF-Token` | 403 | 403 ✔ |
| 20 | Cualquier endpoint sin sesión | 401 | 401 ✔ |

## Ejemplos de consumo de la API

La colección `postman/SGB_Servicio_Cita_AA3_EV02.postman_collection.json` trae
un ejemplo de **cada endpoint**, con casos correctos y de error, y pruebas
automáticas en cada petición. Importarla en Postman (**Import**) y ejecutarla
con **Run collection** (en orden). También se puede correr por consola:

```bash
npx newman run postman/SGB_Servicio_Cita_AA3_EV02.postman_collection.json
```

**1. Iniciar sesión**

```http
POST /api/auth/login
Content-Type: application/json

{ "correo": "cliente@sgb.com", "contrasena": "Prueba2026*" }
```
```json
200 OK
{ "idUsuario": 4, "nombre": "María López", "rol": "CLIENTE", "csrfToken": "sYu0z0…" }
```

**2. Agendar una cita (RF03)** — la cookie de sesión la envía el navegador o
Postman; el token va en la cabecera.

```http
POST /api/citas
Content-Type: application/json
X-CSRF-Token: sYu0z0…

{ "idCliente": 4, "idBarbero": 2, "idServicio": 1, "fechaHora": "2026-12-01T10:00:00", "notas": "Primera vez" }
```
```json
201 Created
{ "idCita": 4, "cliente": "María López", "barbero": "Juan Pérez", "servicio": "Corte de cabello",
  "precioServicio": 20000, "fechaHora": "2026-12-01T10:00:00", "estado": "pendiente", "notas": "Primera vez", … }
```

**3. Error de validación** (fecha mal escrita)

```json
400 Bad Request
{ "status": 400, "mensaje": "Hay datos con formato incorrecto en la peticion.",
  "campos": { "fechaHora": "El campo 'fechaHora' debe ser una fecha y hora valida con el formato AAAA-MM-DDTHH:MM (ejemplo: 2026-12-01T10:30)" },
  "path": "/api/citas", "idCorrelacion": "b70475ef-…" }
```

### Validaciones implementadas

| Campo | Regla | Mensaje |
|-------|-------|---------|
| `nombre` (servicio) | obligatorio, 3–100 caracteres, solo letras, números, espacios y `. , ( ) -` | por campo |
| `descripcion`, `notas` | máx. 1000 caracteres, sin `< > { }` | por campo |
| `precio` | número > 0, máx. 6 enteros y 2 decimales | por campo |
| `duracionMin` | entero (sin decimales) entre 1 y 1440 | por campo |
| `idCliente`, `idBarbero`, `idServicio` | obligatorios, enteros positivos | por campo |
| `fechaHora` | obligatoria, formato `AAAA-MM-DDTHH:MM`, fecha real, no en el pasado, sin cruce con otra cita del barbero | por campo o mensaje de negocio |
| `correo` (login) | obligatorio, formato `nombre@dominio.com`, máx. 150 | por campo |

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

`mvn test` ejecuta **43 pruebas JUnit** (`src/test/java`) que **no se conectan a
ninguna base de datos** (usan Mockito y MockMvc), así que es seguro correrlas
en cualquier máquina: contraseñas, límite de intentos, transiciones de estado,
reglas y permisos de `CitaService`, filtro de seguridad, manejo de errores y,
desde AA3-EV02, validaciones de caracteres especiales, longitudes, números y
fechas (`ValidacionesDtoTest`, `FormatoDatosWebTest`).

### Ejemplo con curl

```bash
curl -c c.txt -H "Content-Type: application/json" \
     -d '{"correo":"cliente@sgb.com","contrasena":"Prueba2026*"}' http://localhost:8081/api/auth/login
# copie el csrfToken de la respuesta
curl -b c.txt -H "Content-Type: application/json" -H "X-CSRF-Token: <csrfToken>" \
     -d '{"idCliente":4,"idBarbero":2,"idServicio":1,"fechaHora":"2026-12-01T10:00:00"}' http://localhost:8081/api/citas
```
