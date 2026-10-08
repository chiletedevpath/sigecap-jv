# SIGECAP J&V

Sistema académico de gestión de cursos de capacitación para el personal del área de Operaciones de J&V Resguardo. Aplicación de escritorio en Java 17 y Swing, sin dependencias externas.

## Organización

Todo el proyecto está en una sola carpeta, `aplicacion`, con el paquete base `pe.utp.sigecapjv`:

```text
aplicacion/
├── src/pe/utp/sigecapjv/
│   ├── modelo/
│   ├── vista/
│   ├── controlador/
│   ├── dao/
│   ├── conexion/
│   └── util/
├── test/pe/utp/sigecapjv/PruebaFlujo.java
└── ejecutar.ps1
```

Los mockups de referencia están fuera de este proyecto, en la carpeta hermana `sigecap-jv-mockups`. Las vistas adaptadas al paquete `vista` se conservan junto al código de la aplicación. `.idea` contiene la configuración de IntelliJ para el único módulo `aplicacion`.

## Ejecutar

En IntelliJ, seleccionar **SIGECAP J&V**. La clase principal es `pe.utp.sigecapjv.vista.FrmPrincipal`; el módulo `aplicacion` usa lenguaje y bytecode Java 17 y hereda el SDK del proyecto. Recargar el proyecto si todavía aparecen las antiguas ejecuciones.

Desde PowerShell, con un JDK 17 o superior:

```powershell
.\aplicacion\ejecutar.ps1 -JavaHome 'C:\Users\Adria\.jdks\ms-17.0.18'
```

Puede omitirse `-JavaHome` si `JAVA_HOME` o los comandos `java`/`javac` ya apuntan a un JDK compatible. El script compila con `--release 17` y genera `aplicacion/out`, una salida regenerable excluida de Git.

## Primera etapa funcional

| Clase o vista | Funcionalidad | Historia |
| --- | --- | --- |
| `Trabajador` y `PersonalControlador` | Registrar, actualizar y consultar personal en memoria; validar datos y evitar DNI duplicados. | HU-JVR-001 |
| `Curso` | Validar código, nombre, tipo y vigencia; activar/desactivar. El controlador evita códigos duplicados. | HU-JVR-002 |
| `Capacitacion` | Validar curso activo, fecha futura, horario y modalidad; programar, actualizar, cancelar y cerrar. Contiene sus participaciones. | HU-JVR-003 |
| `Participacion` | Relacionar trabajador activo y capacitación programada; asignar fecha y estado pendiente; evitar asignaciones duplicadas. | HU-JVR-004 |
| `CapacitacionControlador` | Coordinar cursos, programaciones y asignaciones, con registros existentes e IDs temporales. | HU-JVR-002 a 004 |
| `FrmPrincipal`, `PnlPersonal` y `PnlCapacitaciones` | Navegación, captura de datos y tablas conectadas a controladores compartidos. | Flujo funcional |
| `Validador` | Validaciones reutilizables invocadas por el modelo. | Apoyo al modelo |

Para probar el flujo desde la interfaz:

1. Personal → Registrar trabajador: completar DNI de ocho dígitos, nombres, apellidos, cargo y estado.
2. Capacitaciones → Cursos → Nuevo curso: registrar un curso activo. Vigencia vacía significa «no aplica»; cualquier periodo informado debe ser positivo, en meses.
3. Programación: seleccionar curso, fecha `AAAA-MM-DD`, horario `HH:mm - HH:mm` y modalidad. Guardar una sesión futura.
4. Participantes: seleccionar la capacitación y asignar el trabajador registrado.

Los formularios llaman a los controladores; las reglas de negocio pertenecen al modelo. Los controladores validan copias antes de modificar datos almacenados y sus listados devuelven copias protegidas.

## Diseños y funcionalidades pendientes

La carpeta externa `../sigecap-jv-mockups` permite ejecutar los diseños de referencia de manera independiente. Las vistas `FrmLogin`, `PnlInicio` (cuatro perfiles), `PnlSeguimiento`, `PnlReportes`, `PnlUsuarios`, `PnlMisCapacitaciones` y `PnlAlertas` ya adaptadas se conservan en la aplicación para las siguientes etapas. Todavía usan datos referenciales y no están habilitadas en el flujo funcional.

DAO, conexión y las otras ocho clases del UML mantienen sus esqueletos. No se implementan SQL, MySQL, autenticación, asistencia, resultados, certificados, notificaciones ni reportes. Las pestañas futuras están deshabilitadas.

Los atributos, tipos y firmas principales siguen el UML. Las referencias de objetos y la colección de participaciones representan sus relaciones. `copia()` y `toString()` son auxiliares técnicos.

`Trabajador.consultarHistorial()` y `consultarCapacitaciones()` conservan retorno `void` y comunican que están pendientes con `UnsupportedOperationException`. HU-JVR-008 y HU-JVR-014 todavía no están implementadas. `Participacion.actualizarEstado()` valida `PENDIENTE` o `CANCELADA`; el cumplimiento se incorporará con asistencia y resultados. Cerrar una capacitación actualmente bloquea nuevas asignaciones y edición, sin consolidar resultados.

La aplicación inicia sin datos ni autenticación. Los estados usan `String` y los IDs son secuenciales en memoria. Los datos se pierden al cerrar. La persistencia de HU-JVR-001 a 004 queda pendiente del diseño 3.2. No se ha modificado el informe ni redactado 3.5.

## Verificar

El mismo script permite compilar y ejecutar la única clase de pruebas, sin generar capturas ni informes adicionales:

```powershell
.\aplicacion\ejecutar.ps1 -JavaHome 'C:\Users\Adria\.jdks\ms-17.0.18' -Probar
```

Comprueba el flujo, relaciones UML, encapsulamiento, estados, datos inválidos, duplicados, protección de registros y eventos Swing de registro, edición, búsqueda y programación. Los eventos se prueban sin pantalla; los diálogos nativos requieren revisión manual.
