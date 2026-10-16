-- ============================================================================
-- SIGECAP J&V - DISENO FISICO
-- Sistema de gestion de cursos de capacitacion del personal de Operaciones de J&V Resguardo
-- Curso Integrador I: Sistemas Software
-- Motor: MySQL
-- Juego de caracteres: utf8mb4 | Motor de almacenamiento: InnoDB
-- ============================================================================

CREATE DATABASE IF NOT EXISTS sigecap_jv
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE sigecap_jv;

-- 01. ROL
CREATE TABLE rol (
    idRol INT NOT NULL AUTO_INCREMENT,
    nombre VARCHAR(60) NOT NULL,
    descripcion VARCHAR(255) NULL,
    CONSTRAINT pk_rol PRIMARY KEY (idRol),
    CONSTRAINT uq_rol_nombre UNIQUE (nombre),
    CONSTRAINT ck_rol_nombre CHECK (TRIM(nombre) <> '')
) ENGINE=InnoDB;

-- 02. PERMISO
CREATE TABLE permiso (
    idPermiso INT NOT NULL AUTO_INCREMENT,
    nombre VARCHAR(80) NOT NULL,
    modulo VARCHAR(80) NOT NULL,
    descripcion VARCHAR(255) NULL,
    CONSTRAINT pk_permiso PRIMARY KEY (idPermiso),
    CONSTRAINT uq_permiso_modulo_nombre UNIQUE (modulo, nombre),
    CONSTRAINT ck_permiso_nombre CHECK (TRIM(nombre) <> ''),
    CONSTRAINT ck_permiso_modulo CHECK (TRIM(modulo) <> '')
) ENGINE=InnoDB;

-- 03. ROL_PERMISO: tabla intermedia N:M
CREATE TABLE rol_permiso (
    idRol INT NOT NULL,
    idPermiso INT NOT NULL,
    CONSTRAINT pk_rol_permiso PRIMARY KEY (idRol, idPermiso),
    CONSTRAINT fk_rolpermiso_rol FOREIGN KEY (idRol)
        REFERENCES rol (idRol)
        ON UPDATE CASCADE ON DELETE CASCADE,
    CONSTRAINT fk_rolpermiso_permiso FOREIGN KEY (idPermiso)
        REFERENCES permiso (idPermiso)
        ON UPDATE CASCADE ON DELETE CASCADE
) ENGINE=InnoDB;

-- 04. TRABAJADOR: personal del area de Operaciones
CREATE TABLE trabajador (
    idTrabajador INT NOT NULL AUTO_INCREMENT,
    dni CHAR(8) NOT NULL,
    nombres VARCHAR(100) NOT NULL,
    apellidos VARCHAR(100) NOT NULL,
    cargo VARCHAR(100) NOT NULL,
    estado VARCHAR(20) NOT NULL DEFAULT 'ACTIVO',
    CONSTRAINT pk_trabajador PRIMARY KEY (idTrabajador),
    CONSTRAINT uq_trabajador_dni UNIQUE (dni),
    CONSTRAINT ck_trabajador_dni CHECK (dni REGEXP '^[0-9]{8}$'),
    CONSTRAINT ck_trabajador_nombres CHECK (TRIM(nombres) <> ''),
    CONSTRAINT ck_trabajador_apellidos CHECK (TRIM(apellidos) <> ''),
    CONSTRAINT ck_trabajador_cargo CHECK (TRIM(cargo) <> ''),
    CONSTRAINT ck_trabajador_estado CHECK (estado IN ('ACTIVO', 'INACTIVO'))
) ENGINE=InnoDB;

-- 05. USUARIO: N:1 con ROL y 0..1:1 con TRABAJADOR
-- Una cuenta administrativa puede no corresponder a un trabajador.
-- UNIQUE(idTrabajador) impide asignar dos cuentas al mismo trabajador;
-- multiples usuarios administrativos con idTrabajador NULL son validos.
CREATE TABLE usuario (
    idUsuario INT NOT NULL AUTO_INCREMENT,
    idRol INT NOT NULL,
    idTrabajador INT NULL,
    nombreCompleto VARCHAR(160) NOT NULL,
    nombreUsuario VARCHAR(60) NOT NULL,
    correo VARCHAR(160) NOT NULL,
    contrasenaHash VARCHAR(255) NOT NULL,
    estado VARCHAR(20) NOT NULL DEFAULT 'ACTIVO',
    CONSTRAINT pk_usuario PRIMARY KEY (idUsuario),
    CONSTRAINT uq_usuario_nombre UNIQUE (nombreUsuario),
    CONSTRAINT uq_usuario_correo UNIQUE (correo),
    CONSTRAINT uq_usuario_trabajador UNIQUE (idTrabajador),
    CONSTRAINT ck_usuario_nombre_completo CHECK (TRIM(nombreCompleto) <> ''),
    CONSTRAINT ck_usuario_nombre_usuario CHECK (TRIM(nombreUsuario) <> ''),
    CONSTRAINT ck_usuario_correo CHECK (TRIM(correo) <> ''),
    CONSTRAINT ck_usuario_hash CHECK (TRIM(contrasenaHash) <> ''),
    CONSTRAINT ck_usuario_estado CHECK (estado IN ('ACTIVO', 'INACTIVO')),
    CONSTRAINT fk_usuario_rol FOREIGN KEY (idRol)
        REFERENCES rol (idRol)
        ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT fk_usuario_trabajador FOREIGN KEY (idTrabajador)
        REFERENCES trabajador (idTrabajador)
        ON UPDATE CASCADE ON DELETE RESTRICT
) ENGINE=InnoDB;

-- 06. CURSO: catalogo (el tipo admite SUCAMEC, APN u otros valores pertinentes)
-- vigenciaMeses = NULL significa que no se ha definido periodo de vigencia.
CREATE TABLE curso (
    idCurso INT NOT NULL AUTO_INCREMENT,
    codigo VARCHAR(40) NOT NULL,
    nombre VARCHAR(160) NOT NULL,
    tipo VARCHAR(80) NOT NULL,
    vigenciaMeses INT NULL,
    estado VARCHAR(20) NOT NULL DEFAULT 'ACTIVO',
    CONSTRAINT pk_curso PRIMARY KEY (idCurso),
    CONSTRAINT uq_curso_codigo UNIQUE (codigo),
    CONSTRAINT ck_curso_codigo CHECK (TRIM(codigo) <> ''),
    CONSTRAINT ck_curso_nombre CHECK (TRIM(nombre) <> ''),
    CONSTRAINT ck_curso_tipo CHECK (TRIM(tipo) <> ''),
    CONSTRAINT ck_curso_vigencia CHECK (vigenciaMeses IS NULL OR vigenciaMeses > 0),
    CONSTRAINT ck_curso_estado CHECK (estado IN ('ACTIVO', 'INACTIVO'))
) ENGINE=InnoDB;

-- 07. CAPACITACION: cada programacion corresponde a un curso
CREATE TABLE capacitacion (
    idCapacitacion INT NOT NULL AUTO_INCREMENT,
    idCurso INT NOT NULL,
    fecha DATE NOT NULL,
    horaInicio TIME NOT NULL,
    horaFin TIME NOT NULL,
    modalidad VARCHAR(60) NOT NULL,
    estado VARCHAR(25) NOT NULL DEFAULT 'PROGRAMADA',
    CONSTRAINT pk_capacitacion PRIMARY KEY (idCapacitacion),
    CONSTRAINT fk_capacitacion_curso FOREIGN KEY (idCurso)
        REFERENCES curso (idCurso)
        ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT ck_capacitacion_horario CHECK (horaFin > horaInicio),
    CONSTRAINT ck_capacitacion_modalidad CHECK (TRIM(modalidad) <> ''),
    CONSTRAINT ck_capacitacion_estado CHECK
        (estado IN ('PROGRAMADA', 'CERRADA', 'CANCELADA')),
    INDEX ix_capacitacion_fecha (fecha)
) ENGINE=InnoDB;

-- 08. PARTICIPACION: asignacion de un trabajador a una capacitacion
-- UNIQUE(idTrabajador,idCapacitacion) evita asignaciones duplicadas.
-- La clave compuesta adicional permite verificar certificado .idTrabajador contra el trabajador REAL de la participacion.
CREATE TABLE participacion (
    idParticipacion INT NOT NULL AUTO_INCREMENT,
    idTrabajador INT NOT NULL,
    idCapacitacion INT NOT NULL,
    fechaAsignacion DATE NOT NULL,
    estado VARCHAR(30) NOT NULL DEFAULT 'PENDIENTE',
    CONSTRAINT pk_participacion PRIMARY KEY (idParticipacion),
    CONSTRAINT uq_participacion_trabajador_capacitacion
        UNIQUE (idTrabajador, idCapacitacion),
    CONSTRAINT uq_participacion_id_trabajador
        UNIQUE (idParticipacion, idTrabajador),
    CONSTRAINT ck_participacion_estado CHECK
        (estado IN ('PENDIENTE', 'COMPLETADA', 'CANCELADA')),
    CONSTRAINT fk_participacion_trabajador FOREIGN KEY (idTrabajador)
        REFERENCES trabajador (idTrabajador)
        ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT fk_participacion_capacitacion FOREIGN KEY (idCapacitacion)
        REFERENCES capacitacion (idCapacitacion)
        ON UPDATE CASCADE ON DELETE RESTRICT
) ENGINE=InnoDB;

-- 09. ASISTENCIA: 0..1 registro por participacion
CREATE TABLE asistencia (
    idAsistencia INT NOT NULL AUTO_INCREMENT,
    idParticipacion INT NOT NULL,
    condicion VARCHAR(40) NOT NULL,
    fechaRegistro DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_asistencia PRIMARY KEY (idAsistencia),
    CONSTRAINT uq_asistencia_participacion UNIQUE (idParticipacion),
    CONSTRAINT ck_asistencia_condicion CHECK (TRIM(condicion) <> ''),
    CONSTRAINT fk_asistencia_participacion FOREIGN KEY (idParticipacion)
        REFERENCES participacion (idParticipacion)
        ON UPDATE CASCADE ON DELETE RESTRICT
) ENGINE=InnoDB;

-- 10. RESULTADO: 0..1 registro por participacion
-- El rango MAXIMO de calificacion depende del curso y se validara en Java.
CREATE TABLE resultado (
    idResultado INT NOT NULL AUTO_INCREMENT,
    idParticipacion INT NOT NULL,
    condicion VARCHAR(40) NOT NULL,
    calificacion DECIMAL(5,2) NULL,
    fechaRegistro DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_resultado PRIMARY KEY (idResultado),
    CONSTRAINT uq_resultado_participacion UNIQUE (idParticipacion),
    CONSTRAINT ck_resultado_condicion CHECK (TRIM(condicion) <> ''),
    CONSTRAINT ck_resultado_calificacion CHECK
        (calificacion IS NULL OR calificacion >= 0),
    CONSTRAINT fk_resultado_participacion FOREIGN KEY (idParticipacion)
        REFERENCES participacion (idParticipacion)
        ON UPDATE CASCADE ON DELETE RESTRICT
) ENGINE=InnoDB;

-- 11. CERTIFICADO: 0..1 certificado por participacion
-- idTrabajador es redundancia FISICA CONTROLADA:
-- la FK compuesta garantiza que corresponda a la participacion registrada.
-- UNIQUE(idTrabajador,numero) impide numeros repetidos del mismo trabajador.
CREATE TABLE certificado (
    idCertificado INT NOT NULL AUTO_INCREMENT,
    idParticipacion INT NOT NULL,
    idTrabajador INT NOT NULL,
    numero VARCHAR(80) NOT NULL,
    fechaEmision DATE NOT NULL,
    fechaVencimiento DATE NULL,
    rutaEvidencia VARCHAR(500) NULL,
    estadoVigencia VARCHAR(30) NULL,
    CONSTRAINT pk_certificado PRIMARY KEY (idCertificado),
    CONSTRAINT uq_certificado_participacion UNIQUE (idParticipacion),
    CONSTRAINT uq_certificado_trabajador_numero UNIQUE (idTrabajador, numero),
    CONSTRAINT ck_certificado_numero CHECK (TRIM(numero) <> ''),
    CONSTRAINT ck_certificado_fechas CHECK
        (fechaVencimiento IS NULL OR fechaVencimiento >= fechaEmision),
    CONSTRAINT ck_certificado_estado CHECK
        (estadoVigencia IS NULL OR estadoVigencia IN
            ('VIGENTE', 'PROXIMO_A_VENCER', 'VENCIDO', 'NO_APLICA')),
    CONSTRAINT fk_certificado_participacion_trabajador
        FOREIGN KEY (idParticipacion, idTrabajador)
        REFERENCES participacion (idParticipacion, idTrabajador)
        ON UPDATE CASCADE ON DELETE RESTRICT,
    INDEX ix_certificado_vencimiento (fechaVencimiento)
) ENGINE=InnoDB;

-- 12. NOTIFICACION: N:1 con TRABAJADOR
-- claveEvento debe provenir de un identificador estable del evento, p. ej.
-- ASIGNACION:123 o VENCIMIENTO:123:2026-10-15.
-- La combinacion trabajador + evento evita repetir esa alerta.
CREATE TABLE notificacion (
    idNotificacion INT NOT NULL AUTO_INCREMENT,
    idTrabajador INT NOT NULL,
    tipo VARCHAR(60) NOT NULL,
    mensaje VARCHAR(600) NOT NULL,
    fechaGeneracion DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    estado VARCHAR(30) NOT NULL DEFAULT 'GENERADA',
    claveEvento VARCHAR(180) NOT NULL,
    CONSTRAINT pk_notificacion PRIMARY KEY (idNotificacion),
    CONSTRAINT uq_notificacion_trabajador_evento
        UNIQUE (idTrabajador, claveEvento),
    CONSTRAINT ck_notificacion_tipo CHECK (TRIM(tipo) <> ''),
    CONSTRAINT ck_notificacion_mensaje CHECK (TRIM(mensaje) <> ''),
    CONSTRAINT ck_notificacion_estado CHECK (TRIM(estado) <> ''),
    CONSTRAINT ck_notificacion_evento CHECK (TRIM(claveEvento) <> ''),
    CONSTRAINT fk_notificacion_trabajador FOREIGN KEY (idTrabajador)
        REFERENCES trabajador (idTrabajador)
        ON UPDATE CASCADE ON DELETE RESTRICT,
    INDEX ix_notificacion_fecha (fechaGeneracion)
) ENGINE=InnoDB;

-- VISTA DE CONSULTA (NO ES UNA TABLA): CONTROL ACTUAL DE VIGENCIAS
-- El estado se calcula en tiempo de consulta; no depende de un job diario.
-- El periodo para identificar 'proximos a vencer' debe configurarse en la
-- consulta o en la logica Java mediante diasRestantes.
-- Se conserva certificado.estadoVigencia por correspondencia con el modelo
-- aprobado, pero para el estado ACTUAL debe utilizarse esta vista.
CREATE VIEW vw_control_vigencias AS
SELECT
    ce.idCertificado,
    ce.idParticipacion,
    p.idTrabajador,
    t.dni,
    t.nombres,
    t.apellidos,
    ca.idCapacitacion,
    cu.idCurso,
    cu.codigo AS codigoCurso,
    cu.nombre AS nombreCurso,
    cu.tipo AS tipoCurso,
    ce.numero AS numeroCertificado,
    ce.fechaEmision,
    ce.fechaVencimiento,
    DATEDIFF(ce.fechaVencimiento, CURDATE()) AS diasRestantes,
    CASE
        WHEN ce.fechaVencimiento IS NULL THEN 'NO_APLICA'
        WHEN ce.fechaVencimiento < CURDATE() THEN 'VENCIDO'
        ELSE 'VIGENTE'
    END AS estadoActual
FROM certificado AS ce
INNER JOIN participacion AS p ON p.idParticipacion = ce.idParticipacion
INNER JOIN trabajador AS t ON t.idTrabajador = p.idTrabajador
INNER JOIN capacitacion AS ca ON ca.idCapacitacion = p.idCapacitacion
INNER JOIN curso AS cu ON cu.idCurso = ca.idCurso;

-- ============================================================================
-- VERIFICACIONES (solo lectura)
-- Deben aparecer 12 tablas y 1 vista.
-- ============================================================================
SELECT COUNT(*) AS totalTablas
FROM information_schema.TABLES
WHERE TABLE_SCHEMA = 'sigecap_jv'
  AND TABLE_TYPE = 'BASE TABLE';

SHOW FULL TABLES FROM sigecap_jv;

SELECT TABLE_NAME, CONSTRAINT_NAME, CONSTRAINT_TYPE
FROM information_schema.TABLE_CONSTRAINTS
WHERE TABLE_SCHEMA = 'sigecap_jv'
ORDER BY TABLE_NAME, CONSTRAINT_TYPE, CONSTRAINT_NAME;
