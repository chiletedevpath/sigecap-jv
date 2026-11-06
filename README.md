# SIGECAP J&V

SIGECAP es un sistema de gestión de cursos de capacitación para el área de Operaciones de J&V Resguardo. Lo desarrollamos como proyecto académico con Java 17, Swing y MySQL. Permite registrar trabajadores, organizar capacitaciones y consultar sus resultados, certificados y vencimientos.

## Qué hemos avanzado

La aplicación ya trabaja con datos de MySQL. El flujo disponible es registrar un trabajador y un curso, programar la capacitación, asignar participantes y registrar asistencia, resultado y certificado.

| Módulo | Qué permite hacer |
| --- | --- |
| Acceso y usuarios | Iniciar sesión, crear usuarios, asignar roles, editar permisos y activar o desactivar cuentas. |
| Personal | Registrar y actualizar trabajadores; consultar su historial de capacitación. |
| Capacitaciones | Administrar cursos, programar sesiones, asignar participantes y registrar asistencia, resultados y certificados. |
| Inicio | Consultar tarjetas y tablas con datos reales según el perfil. |
| Mis capacitaciones | Consultar el historial del trabajador que inició sesión, con filtros por estado. |
| Seguimiento | Consultar certificados, vencimientos y días restantes; filtrar por curso y vigencia. |
| Alertas | Consultar asignaciones, recordatorios y avisos de vencimiento y marcarlos como leídos según el acceso del usuario. |
| Reportes | Filtrar el consolidado de capacitaciones y exportarlo a PDF. |

Se ingresa con **nombre de usuario y contraseña**. El correo es un dato de contacto. La opción **Crear primer administrador** aparece únicamente cuando la base no tiene usuarios. Si ya existe una cuenta, no hay que crearla nuevamente.

El administrador gestiona usuarios y permisos. Por defecto, el responsable de capacitaciones puede gestionar Personal y Capacitaciones; el supervisor consulta la información; el trabajador ve sus propios datos. Seguimiento requiere consultar Capacitaciones y no está disponible para el trabajador. El historial de Personal requiere consultar Personal y Capacitaciones. Los cambios de rol, permisos, contraseña o estado invalidan las sesiones anteriores.

Los resultados admiten una nota opcional de 0 a 20 y requieren asistencia registrada. El certificado requiere un resultado favorable. Su evidencia se guarda como una ruta a un archivo local: el sistema no copia ni publica ese archivo.

Las vigencias de Inicio, Historial, Seguimiento y Reportes usan la fecha de MySQL. Un certificado es próximo a vencer desde hoy hasta el plazo configurado inclusive (30 días inicialmente); está vencido después de su fecha de vencimiento. Sin fecha de vencimiento figura NO_APLICA; una participación sin certificado figura SIN_CERTIFICADO en Historial y Reportes. Seguimiento muestra únicamente certificados existentes.

Administrador y Responsable generan avisos al consultar Alertas con permiso de gestión. Supervisor solo consulta. El trabajador puede leer y marcar únicamente sus propios avisos. Volver a consultar no duplica el mismo evento. La pestaña Alertas de Seguimiento utiliza este mismo módulo.

Desde **Alertas → Configurar plazos**, el administrador puede definir entre 0 y 365 días para vencimientos y recordatorios por separado. Cero incluye solo hoy para el aviso anticipado. Los cambios se aplican en la siguiente consulta y los avisos ya emitidos conservan su estado de lectura. Para actualizar una instalación existente, aplica primero la migración de plazos indicada en la guía de base de datos.

## Recorrido para la presentación

1. Ingresa como Administrador. En Personal registra al trabajador y en Usuarios crea su cuenta con el rol Trabajador. El selector muestra los trabajadores disponibles para vincular.
2. Ingresa como Responsable de capacitaciones. Programa una sesión para mañana y asigna al trabajador.
3. Ingresa con la cuenta del trabajador. Abre Mis capacitaciones y Alertas, comprueba el curso y marca el aviso como leído. Cierra sesión y vuelve a ingresar para verificar que la lectura quedó guardada.
4. Ingresa como Supervisor y consulta la programación. Con los permisos iniciales de este perfil puedes revisar los datos y reportes.

## Qué falta

El cierre del proyecto también debe cubrir pruebas, despliegue y operación. Tomamos el sílabo como guía de trabajo; los criterios de evaluación específicos deben confirmarse con el docente.

1. **Empaquetado con Maven.** Configurar compilación Java 17, dependencias, ejecución de pruebas y distribución de la aplicación. Actualmente compilamos con PowerShell y `javac`; Docker tampoco usa Maven. Mantendremos la ejecución local mientras validamos el nuevo empaquetado.
2. **Evidencia de pruebas y seguridad.** Relacionar las historias de usuario con los casos y resultados de prueba. Ya comprobamos permisos, sesiones, aislamiento y validaciones; falta consolidar la revisión de seguridad y sus observaciones. Tener pruebas no demuestra por sí solo que se haya aplicado TDD.
3. **Monitoreo.** Registrar errores de forma consistente sin exponer contraseñas ni datos personales, y definir qué revisar cuando falle la aplicación o MySQL. Evaluaremos una biblioteca de logs para una necesidad concreta.
4. **Mantenimiento.** Preparar y probar respaldo y restauración en una base aislada, y definir el procedimiento de actualización. No basta con generar un respaldo: debemos comprobar que se puede recuperar.
5. **Aceptación y entrega.** Revisar el flujo completo con los cuatro perfiles en el equipo de presentación, contrastar las historias con el informe y completar las instrucciones de usuario. La revisión del grupo sigue pendiente aunque las pruebas automatizadas pasen.

También debemos contrastar el informe con los entregables de planificación y diseño del curso: Lean Canvas, Project Charter, Gantt, WBS, requerimientos, BPM y diseños de base de datos. Su revisión documental no está cerrada. La base definitiva de presentación se preparará con respaldo previo; todavía no se han eliminado los datos actuales.

Desde Capacitaciones → Participantes puedes buscar por DNI o nombre y marcar varios trabajadores activos para una sesión. La selección se conserva al cambiar la búsqueda. Al confirmar, MySQL guarda el grupo y un aviso individual por trabajador en una sola transacción. Si alguien ya fue asignado o dejó de estar activo, se revierte el grupo. Si el responsable cambia el curso, fecha, horario o modalidad, cada participante pendiente recibe la nueva programación. Cancelar la sesión también genera un aviso. El trabajador consulta el horario y la modalidad en Mis capacitaciones. Al abrir Alertas se generan recordatorios de las sesiones pendientes que empiezan dentro del plazo configurado (tres días inicialmente). El trabajador recibe únicamente los suyos. Los avisos se consultan dentro de SIGECAP: no existe un proceso automático en segundo plano ni envío por correo o WhatsApp. Las evidencias de certificados son archivos locales y los PDF se guardan en el equipo desde el que se ejecuta la aplicación.

## Cómo ejecutarlo

### Desde IntelliJ o PowerShell

Necesitas JDK 17 o superior, MySQL 8.4 y MySQL Connector/J. La [guía de base de datos](database/README.md) explica cómo crear el esquema y configurar la cuenta de conexión. El esquema se ejecuta una sola vez en una base nueva; no lo vuelvas a ejecutar sobre una instalación existente.

Configura estas variables en el proceso que inicia la aplicación:

| Variable | Valor |
| --- | --- |
| `SIGECAP_DB_URL` | URL de tu base, por ejemplo `jdbc:mysql://localhost:3306/sigecap_jv`. |
| `SIGECAP_DB_USER` | Cuenta de MySQL para la aplicación. |
| `SIGECAP_DB_PASSWORD` | Contraseña de esa cuenta. |
| `SIGECAP_JDBC_JAR` | Ruta al JAR de Connector/J, si el lanzador no lo encuentra en la caché Maven. |

En IntelliJ abre la raíz del repositorio, selecciona el JDK y ejecuta **SIGECAP MySQL**. La clase principal es `pe.utp.sigecapjv.vista.FrmPrincipal`. Guarda las credenciales en la configuración local, no en los archivos compartidos.

Desde PowerShell, situado en la raíz del repositorio y con las variables configuradas:

```powershell
.\aplicacion\ejecutar.ps1
```

Si el JDK o Connector/J no se detectan, indica sus rutas reales:

```powershell
.\aplicacion\ejecutar.ps1 -JavaHome 'C:\ruta\al\jdk' -ConnectorJar 'C:\ruta\mysql-connector-j-26.7.0.jar'
```

### Con Docker

En Windows 11, Docker Desktop con WSL 2 y WSLg permite ejecutar MySQL y la ventana Swing. Desde la raíz del repositorio, copia `.env.example` como `.env` y define `SIGECAP_MYSQL_ROOT_PASSWORD` y `SIGECAP_DB_PASSWORD`. Después ejecuta:

```powershell
docker compose up -d --build
```

Para volver a abrir la aplicación después de cerrarla, usa `docker compose up -d app`. Para detener los servicios, usa `docker compose down`; los datos permanecen en el volumen. **No añadas `-v` si quieres conservarlos.**

Si Docker no dispone del socket WSLg, levanta solo la base con `docker compose up -d db` y ejecuta Java desde IntelliJ o PowerShell. En ese caso la URL es `jdbc:mysql://localhost:3307/sigecap_jv` y el usuario es `sigecap_app`, con la contraseña definida en `.env`. El lanzador PowerShell no carga `.env` automáticamente.

La base de Docker en el puerto 3307 es independiente de un MySQL local en el 3306. Sus cuentas y datos no se copian entre ambas. Los scripts de inicialización solo se ejecutan al crear el volumen: cambiar `.env` no cambia las contraseñas de una base existente. Consulta la guía de base de datos para actualizar permisos sin borrar registros.

Los PDF exportados desde Docker quedan en `reportes/`.

## Pruebas

Desde la raíz del repositorio:

```powershell
# Modelo, controladores y eventos Swing, sin base de datos.
.\aplicacion\ejecutar.ps1 -Probar

# Regresión con MySQL temporal. Requiere Docker y Connector/J.
.\aplicacion\probar-mysql.ps1

# Inicio por perfil con MySQL temporal.
.\aplicacion\probar-mysql.ps1 -Inicio

# Suite completa y navegación de ventanas por perfil; requiere un escritorio activo.
.\aplicacion\probar-mysql.ps1 -Ventanas
```

La suite MySQL verifica el flujo de capacitación, persistencia entre procesos, usuarios, permisos, sesiones, reportes, historial, alertas y seguimiento. Crea su propio contenedor y lo elimina al terminar; no usa la base real. La opción `-Ventanas` abre los módulos con las cuatro cuentas de prueba, verifica el menú y el cierre de sesión, y guarda renders en `aplicacion/out/revision-perfiles/` para revisar el diseño. Esos archivos no se publican en Git.

Las pruebas de Seguimiento comprueban la consulta de los tres perfiles autorizados y el rechazo del trabajador, además de filtros y limpieza de datos al cerrar sesión.

## Trabajo en equipo

| Integrante | Responsabilidad | Rama |
| --- | --- | --- |
| Adrian Pisco | Seguridad y control de acceso | `feature/seguridad-acceso` |
| Anthony Sacha | Historial y consulta del trabajador | `feature/historial-trabajador` |
| Cristofher Chirihuana | Vigencias y notificaciones | `feature/vigencias-notificaciones` |
| Lijhoan Machaca | Reportes e integración | `feature/reportes-integracion` |

Cada integrante trabaja en su rama y abre un pull request hacia `main`. Adrian revisa los cambios antes de integrarlos. Las modificaciones de clases compartidas y base de datos se coordinan con el equipo.

El código está en `aplicacion/src/`, las pruebas en `aplicacion/test/` y el esquema en `database/`. Las credenciales, `.env`, preferencias personales, PDF generados y `privado/` quedan fuera de Git. El contenido de `privado/` no tiene respaldo en este repositorio.
