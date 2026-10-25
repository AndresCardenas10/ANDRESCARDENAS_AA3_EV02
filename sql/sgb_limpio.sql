-- ============================================================
-- SGB — Sistema Gestión Barbería
-- Base de datos LIMPIA para pruebas
-- Evidencia GA7-220501096-AA3-EV02
-- ============================================================
-- Qué es este archivo:
--   La misma estructura de tablas de la base `sgb` original (volcado de
--   phpMyAdmin usado desde la Evidencia AA2-EV02), pero SIN los datos
--   reales. En su lugar trae usuarios, servicios y citas de PRUEBA.
--
-- Por qué:
--   El volcado original tenía contraseñas reales en texto plano. Aquí
--   todas las contraseñas están cifradas con BCrypt (el mismo algoritmo
--   que usa la API) y son ficticias.
--
-- Usuarios de prueba (contraseña de TODOS: Prueba2026*)
--   id | correo              | nombre          | rol            | activo
--   1  | admin@sgb.com       | Laura Gómez     | Administrador  | sí
--   2  | barbero@sgb.com     | Juan Pérez      | Barbero        | sí
--   3  | barbero2@sgb.com    | Diego Ramírez   | Barbero        | sí
--   4  | cliente@sgb.com     | María López     | Cliente        | sí
--   5  | cliente2@sgb.com    | Sofía Torres    | Cliente        | sí
--   6  | inactivo@sgb.com    | Carlos Ruiz     | Barbero        | NO
--
-- ATENCIÓN: este script BORRA la base `sgb` si ya existe y la vuelve a
-- crear. Antes de importarlo, exporte una copia de su base actual desde
-- phpMyAdmin (pestaña Exportar).
-- ============================================================

DROP DATABASE IF EXISTS `sgb`;
CREATE DATABASE `sgb` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE `sgb`;

SET SQL_MODE = "NO_AUTO_VALUE_ON_ZERO";
START TRANSACTION;
SET time_zone = "+00:00";


/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!40101 SET NAMES utf8mb4 */;

--
-- Base de datos: `sgb`
--

-- --------------------------------------------------------

--
-- Estructura de tabla para la tabla `cita`
--

CREATE TABLE `cita` (
  `id_cita` int(11) NOT NULL,
  `id_cliente` int(11) NOT NULL,
  `id_barbero` int(11) NOT NULL,
  `id_servicio` int(11) NOT NULL,
  `fecha_hora` datetime NOT NULL,
  `estado` enum('pendiente','confirmada','cancelada','completada') NOT NULL DEFAULT 'pendiente',
  `expira_en` datetime DEFAULT NULL COMMENT 'Límite para confirmar; NULL = no expira',
  `notas` text DEFAULT NULL,
  `creado_en` timestamp NOT NULL DEFAULT current_timestamp()
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Citas programadas entre clientes y barberos';

-- --------------------------------------------------------

--
-- Estructura de tabla para la tabla `cita_servicio`
--

CREATE TABLE `cita_servicio` (
  `id_cita` int(11) NOT NULL,
  `id_servicio` int(11) NOT NULL,
  `precio_aplicado` decimal(8,2) NOT NULL COMMENT 'Precio del servicio al momento de la cita'
) ;

-- --------------------------------------------------------

--
-- Estructura de tabla para la tabla `horario_barbero`
--

CREATE TABLE `horario_barbero` (
  `id_horario` int(11) NOT NULL,
  `id_barbero` int(11) NOT NULL,
  `dia_semana` tinyint(4) NOT NULL COMMENT '1=Lun, 2=Mar, ..., 7=Dom',
  `hora_inicio` time NOT NULL,
  `hora_fin` time NOT NULL
) ;

-- --------------------------------------------------------

--
-- Estructura de tabla para la tabla `notificacion`
--

CREATE TABLE `notificacion` (
  `id_notificacion` int(11) NOT NULL,
  `id_usuario` int(11) NOT NULL,
  `tipo` varchar(50) NOT NULL COMMENT 'cita | pago | recordatorio | sistema',
  `mensaje` text NOT NULL,
  `leida` tinyint(1) NOT NULL DEFAULT 0,
  `creado_en` timestamp NOT NULL DEFAULT current_timestamp()
) ;

-- --------------------------------------------------------

--
-- Estructura de tabla para la tabla `pago`
--

CREATE TABLE `pago` (
  `id_pago` int(11) NOT NULL,
  `id_cita` int(11) NOT NULL,
  `monto` decimal(8,2) NOT NULL,
  `metodo_pago` enum('efectivo','transferencia') NOT NULL,
  `url_comprobante` varchar(500) DEFAULT NULL COMMENT 'URL del comprobante; solo aplica para transferencia',
  `estado_pago` enum('pendiente','verificado','rechazado') NOT NULL DEFAULT 'pendiente',
  `registrado_en` timestamp NOT NULL DEFAULT current_timestamp()
) ;

-- --------------------------------------------------------

--
-- Estructura de tabla para la tabla `producto`
--

CREATE TABLE `producto` (
  `id_producto` int(11) NOT NULL,
  `nombre` varchar(100) NOT NULL,
  `descripcion` text DEFAULT NULL,
  `precio` decimal(8,2) NOT NULL,
  `stock` int(11) NOT NULL DEFAULT 0,
  `activo` tinyint(1) NOT NULL DEFAULT 1
) ;

-- --------------------------------------------------------

--
-- Estructura de tabla para la tabla `resena`
--

CREATE TABLE `resena` (
  `id_resena` int(11) NOT NULL,
  `id_cita` int(11) NOT NULL,
  `calificacion` tinyint(4) NOT NULL COMMENT '1 a 5 estrellas',
  `comentario` text DEFAULT NULL,
  `creado_en` timestamp NOT NULL DEFAULT current_timestamp()
) ;

-- --------------------------------------------------------

--
-- Estructura de tabla para la tabla `rol`
--

CREATE TABLE `rol` (
  `id_rol` int(11) NOT NULL,
  `nombre_rol` varchar(50) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Roles de usuario del sistema';

--
-- Volcado de datos para la tabla `rol`
--


-- --------------------------------------------------------

--
-- Estructura de tabla para la tabla `servicio`
--

CREATE TABLE `servicio` (
  `id_servicio` int(11) NOT NULL,
  `nombre` varchar(100) NOT NULL,
  `descripcion` text DEFAULT NULL,
  `precio` decimal(8,2) NOT NULL,
  `duracion_min` int(11) NOT NULL,
  `activo` tinyint(1) NOT NULL DEFAULT 1
) ;

-- --------------------------------------------------------

--
-- Estructura de tabla para la tabla `usuario`
--

CREATE TABLE `usuario` (
  `id_usuario` int(11) NOT NULL,
  `id_rol` int(11) NOT NULL,
  `nombre` varchar(100) NOT NULL,
  `apellido` varchar(100) NOT NULL,
  `correo` varchar(150) NOT NULL,
  `contrasena_hash` varchar(255) NOT NULL,
  `telefono` varchar(20) DEFAULT NULL,
  `token_recuperacion` varchar(255) DEFAULT NULL,
  `expira_token` datetime DEFAULT NULL,
  `activo` tinyint(1) NOT NULL DEFAULT 1,
  `creado_en` timestamp NOT NULL DEFAULT current_timestamp()
) ;

--
-- Volcado de datos para la tabla `usuario`
--


--
-- Índices para tablas volcadas
--

--
-- Indices de la tabla `cita`
--
ALTER TABLE `cita`
  ADD PRIMARY KEY (`id_cita`),
  ADD KEY `fk_cita_cliente` (`id_cliente`),
  ADD KEY `fk_cita_barbero` (`id_barbero`),
  ADD KEY `fk_cita_servicio` (`id_servicio`);

--
-- Indices de la tabla `cita_servicio`
--
ALTER TABLE `cita_servicio`
  ADD PRIMARY KEY (`id_cita`,`id_servicio`),
  ADD KEY `fk_cs_servicio` (`id_servicio`);

--
-- Indices de la tabla `horario_barbero`
--
ALTER TABLE `horario_barbero`
  ADD PRIMARY KEY (`id_horario`),
  ADD KEY `fk_horario_barbero` (`id_barbero`);

--
-- Indices de la tabla `notificacion`
--
ALTER TABLE `notificacion`
  ADD PRIMARY KEY (`id_notificacion`),
  ADD KEY `fk_notificacion_usuario` (`id_usuario`);

--
-- Indices de la tabla `pago`
--
ALTER TABLE `pago`
  ADD PRIMARY KEY (`id_pago`),
  ADD KEY `fk_pago_cita` (`id_cita`);

--
-- Indices de la tabla `producto`
--
ALTER TABLE `producto`
  ADD PRIMARY KEY (`id_producto`);

--
-- Indices de la tabla `resena`
--
ALTER TABLE `resena`
  ADD PRIMARY KEY (`id_resena`),
  ADD UNIQUE KEY `uq_resena_cita` (`id_cita`);

--
-- Indices de la tabla `rol`
--
ALTER TABLE `rol`
  ADD PRIMARY KEY (`id_rol`),
  ADD UNIQUE KEY `uq_rol_nombre` (`nombre_rol`);

--
-- Indices de la tabla `servicio`
--
ALTER TABLE `servicio`
  ADD PRIMARY KEY (`id_servicio`);

--
-- Indices de la tabla `usuario`
--
ALTER TABLE `usuario`
  ADD PRIMARY KEY (`id_usuario`),
  ADD UNIQUE KEY `uq_usuario_correo` (`correo`),
  ADD KEY `fk_usuario_rol` (`id_rol`);

--
-- AUTO_INCREMENT de las tablas volcadas
--

--
-- AUTO_INCREMENT de la tabla `cita`
--
ALTER TABLE `cita`
  MODIFY `id_cita` int(11) NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT de la tabla `horario_barbero`
--
ALTER TABLE `horario_barbero`
  MODIFY `id_horario` int(11) NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT de la tabla `notificacion`
--
ALTER TABLE `notificacion`
  MODIFY `id_notificacion` int(11) NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT de la tabla `pago`
--
ALTER TABLE `pago`
  MODIFY `id_pago` int(11) NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT de la tabla `producto`
--
ALTER TABLE `producto`
  MODIFY `id_producto` int(11) NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT de la tabla `resena`
--
ALTER TABLE `resena`
  MODIFY `id_resena` int(11) NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT de la tabla `rol`
--
ALTER TABLE `rol`
  MODIFY `id_rol` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=4;

--
-- AUTO_INCREMENT de la tabla `servicio`
--
ALTER TABLE `servicio`
  MODIFY `id_servicio` int(11) NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT de la tabla `usuario`
--
ALTER TABLE `usuario`
  MODIFY `id_usuario` int(11) NOT NULL AUTO_INCREMENT;

--
-- Restricciones para tablas volcadas
--

--
-- Filtros para la tabla `cita`
--
ALTER TABLE `cita`
  ADD CONSTRAINT `fk_cita_barbero` FOREIGN KEY (`id_barbero`) REFERENCES `usuario` (`id_usuario`) ON UPDATE CASCADE,
  ADD CONSTRAINT `fk_cita_cliente` FOREIGN KEY (`id_cliente`) REFERENCES `usuario` (`id_usuario`) ON UPDATE CASCADE,
  ADD CONSTRAINT `fk_cita_servicio` FOREIGN KEY (`id_servicio`) REFERENCES `servicio` (`id_servicio`) ON UPDATE CASCADE;

--
-- Filtros para la tabla `cita_servicio`
--
ALTER TABLE `cita_servicio`
  ADD CONSTRAINT `fk_cs_cita` FOREIGN KEY (`id_cita`) REFERENCES `cita` (`id_cita`) ON DELETE CASCADE ON UPDATE CASCADE,
  ADD CONSTRAINT `fk_cs_servicio` FOREIGN KEY (`id_servicio`) REFERENCES `servicio` (`id_servicio`) ON UPDATE CASCADE;

--
-- Filtros para la tabla `horario_barbero`
--
ALTER TABLE `horario_barbero`
  ADD CONSTRAINT `fk_horario_barbero` FOREIGN KEY (`id_barbero`) REFERENCES `usuario` (`id_usuario`) ON DELETE CASCADE ON UPDATE CASCADE;

--
-- Filtros para la tabla `notificacion`
--
ALTER TABLE `notificacion`
  ADD CONSTRAINT `fk_notificacion_usuario` FOREIGN KEY (`id_usuario`) REFERENCES `usuario` (`id_usuario`) ON DELETE CASCADE ON UPDATE CASCADE;

--
-- Filtros para la tabla `pago`
--
ALTER TABLE `pago`
  ADD CONSTRAINT `fk_pago_cita` FOREIGN KEY (`id_cita`) REFERENCES `cita` (`id_cita`) ON UPDATE CASCADE;

--
-- Filtros para la tabla `resena`
--
ALTER TABLE `resena`
  ADD CONSTRAINT `fk_resena_cita` FOREIGN KEY (`id_cita`) REFERENCES `cita` (`id_cita`) ON DELETE CASCADE ON UPDATE CASCADE;

--
-- Filtros para la tabla `usuario`
--
ALTER TABLE `usuario`
  ADD CONSTRAINT `fk_usuario_rol` FOREIGN KEY (`id_rol`) REFERENCES `rol` (`id_rol`) ON UPDATE CASCADE;

-- ============================================================
-- Datos de prueba
-- ============================================================
-- Se usa la hora de Colombia para que las fechas relativas (CURDATE)
-- coincidan con la hora con la que trabaja la API.
SET time_zone = '-05:00';

INSERT INTO `rol` (`id_rol`, `nombre_rol`) VALUES
(1, 'Administrador'),
(2, 'Barbero'),
(3, 'Cliente');

INSERT INTO `usuario` (`id_usuario`, `id_rol`, `nombre`, `apellido`, `correo`, `contrasena_hash`, `telefono`, `activo`) VALUES
(1, 1, 'Laura', 'Gómez', 'admin@sgb.com', '$2a$12$OmLFF5jTb8dbT8eR4kdqvONmk9c1uUx6U0Upbb94JAUKhN05SKKee', '3000000001', 1),
(2, 2, 'Juan', 'Pérez', 'barbero@sgb.com', '$2a$12$ISx0Ym9S9Ka4Bi7eL7MACeV2kao1nGzHpW8TaD9Q53Uqpm5o34Nfa', '3000000002', 1),
(3, 2, 'Diego', 'Ramírez', 'barbero2@sgb.com', '$2a$12$FJ1q6kKNiUcdBjgGLy1PEOJQleDLX0aDaMEftSHAsjl599.RModai', '3000000003', 1),
(4, 3, 'María', 'López', 'cliente@sgb.com', '$2a$12$9LD0vPPw2Ny/gr5JyJTlWukn43uP/hINXskJoLwAV2IJisSzfOZhq', '3000000004', 1),
(5, 3, 'Sofía', 'Torres', 'cliente2@sgb.com', '$2a$12$O8eaLjtQmM/cehZkoFwote7PutlEcn6ALY0b5zLg7DycUyfW/sj7K', '3000000005', 1),
(6, 2, 'Carlos', 'Ruiz', 'inactivo@sgb.com', '$2a$12$Wekv7dkeIuWfADBPSfrVyOSHQvTOidKw3iIQQrTvPFjQ2qogUTi4.', '3000000006', 0);

INSERT INTO `servicio` (`id_servicio`, `nombre`, `descripcion`, `precio`, `duracion_min`, `activo`) VALUES
(1, 'Corte de cabello', 'Corte clásico a máquina y tijera', 20000, 30, 1),
(2, 'Afeitado clásico', 'Afeitado con toalla caliente y navaja', 15000, 20, 1),
(3, 'Corte + barba', 'Corte de cabello y arreglo de barba', 30000, 45, 1),
(4, 'Tinte de cabello', 'Aplicación de tinte y lavado', 40000, 60, 1),
(5, 'Mascarilla facial', 'Servicio desactivado (para probar la validación)', 25000, 30, 0);

-- Citas de ejemplo con fechas relativas al día en que se importa el script.
INSERT INTO `cita` (`id_cita`, `id_cliente`, `id_barbero`, `id_servicio`, `fecha_hora`, `estado`, `notas`) VALUES
(1, 4, 2, 1, TIMESTAMP(CURDATE() + INTERVAL 2 DAY, '09:00:00'), 'pendiente', 'Cita de ejemplo'),
(2, 5, 2, 3, TIMESTAMP(CURDATE() + INTERVAL 2 DAY, '15:00:00'), 'confirmada', 'Cita de ejemplo'),
(3, 4, 3, 2, TIMESTAMP(CURDATE() - INTERVAL 3 DAY, '11:00:00'), 'completada', 'Cita pasada (historial)');

ALTER TABLE `usuario` AUTO_INCREMENT = 7;
ALTER TABLE `servicio` AUTO_INCREMENT = 6;
ALTER TABLE `cita` AUTO_INCREMENT = 4;

COMMIT;
