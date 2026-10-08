# SIGECAP J&V

Proyecto académico de gestión de capacitaciones para el personal del área de Operaciones de J&V Resguardo, desarrollado en Java 17 y Swing.

## Alcance implementado

El primer avance cubre el flujo **registrar personal → registrar curso → programar capacitación → asignar trabajador**. Los registros se mantienen en memoria durante la sesión.

| Clase del modelo | Funcionalidad implementada | Requerimiento |
| --- | --- | --- |
| `Trabajador` | Registro y actualización de personal; validación de DNI de ocho dígitos, nombres, apellidos, cargo y estado. | HU-JVR-001 |
| `Curso` | Registro y actualización; validación de código, nombre, tipo y vigencia; activación y desactivación. | HU-JVR-002 |
| `Capacitacion` | Programación y actualización con curso activo, fecha futura, horario y modalidad válidos; cancelación y cierre. | HU-JVR-003 |
| `Participacion` | Asignación de un trabajador activo a una capacitación programada; fecha de asignación, estado y control de duplicados. | HU-JVR-004 |

Los atributos, tipos y métodos principales conservan el diseño del diagrama de clases del apartado 3.1. Cada capacitación pertenece a un curso y contiene participaciones; cada participación vincula un trabajador con una capacitación.

## Separación de responsabilidades

- **Modelo:** valida los datos y aplica las reglas de negocio.
- **Controladores:** `PersonalControlador` y `CapacitacionControlador` coordinan los registros en memoria, identificadores y controles de duplicidad.
- **Vistas:** `FrmPrincipal`, `PnlPersonal` y `PnlCapacitaciones` capturan datos y llaman a los controladores. Permiten registrar, editar, buscar, programar y asignar participantes.
- **Utilidades:** `Validador` concentra validaciones reutilizables.

## Revisión para el apartado 3.5

`Capacitacion` valida el curso activo y la programación sin volver a registrar el curso. `Participacion` valida las relaciones, estados y duplicidad sin volver a registrar al trabajador. La existencia de ambos registros se comprueba desde los controladores.

Las secciones Historial, Asistencia, Resultados y Certificados permanecen vacías y deshabilitadas, con indicación de funcionalidad pendiente. No contienen registros simulados. Las clases del modelo y los controladores presentan las instrucciones por separado para facilitar su lectura y sustentación.

Las pruebas incluyen DNI y curso duplicados, curso y trabajador inactivos, programación inválida y asignación duplicada. También comprueban que programar o asignar no invoque nuevamente el registro de sus entidades y que las secciones pendientes no muestren datos ficticios.

El primer avance queda preparado como evidencia para documentar el apartado **3.5. Funcionalidades del código fuente**, conservando el UML y la arquitectura MVC + DAO del apartado 3.4.

## Organización

El código funcional está en `aplicacion/src/pe/utp/sigecapjv`, organizado en `modelo`, `vista`, `controlador`, `dao`, `conexion` y `util`. DAO y conexión conservan sus esqueletos para una etapa posterior.

Los mockups de referencia están en una carpeta hermana independiente. Las vistas adaptadas se conservan en la aplicación; los módulos futuros permanecen deshabilitados en el flujo funcional.

## Validación del avance

Se verificó la compilación con Java 17 y se completaron **120 comprobaciones** del flujo, relaciones UML, encapsulamiento, estados, datos inválidos, duplicados, protección de registros y eventos Swing. Los eventos se verificaron sin pantalla; los diálogos nativos requieren revisión manual.

## Pendientes

- Persistencia en base de datos: los datos actuales se pierden al cerrar la aplicación.
- Desarrollo funcional de `Asistencia`, `Resultado`, `Certificado`, `Usuario`, `Rol`, `Permiso`, `Notificacion` y `Reporte`.
- Autenticación, historial del trabajador, asistencia, resultados, certificados, notificaciones y reportes.
- Consolidación de resultados al cerrar una capacitación. Actualmente el cierre bloquea edición y nuevas asignaciones.

`Trabajador.consultarHistorial()` y `consultarCapacitaciones()` conservan las firmas del UML y señalan que están pendientes mediante `UnsupportedOperationException`. `Participacion.actualizarEstado()` admite `PENDIENTE` y `CANCELADA`; el cumplimiento dependerá de asistencia y resultados.

Este avance no modifica el informe ni incorpora todavía las capturas de código del apartado 3.5.
