# SIGECAP J&V

SIGECAP es el sistema de gestión de cursos de capacitación que desarrollamos para el área de Operaciones de J&V Resguardo como proyecto del Curso Integrador I. Está hecho con Java 17, Swing y MySQL.

El sistema permite registrar trabajadores, programar cursos, asignar participantes y llevar el control de asistencia, resultados, certificados y vencimientos. Cada usuario ingresa con su cuenta y accede a los módulos que corresponden a su perfil.

## Qué hemos avanzado

Los módulos trabajan con datos guardados en MySQL.

| Módulo | Funciones disponibles |
| --- | --- |
| Acceso y usuarios | Inicio de sesión, creación y edición de cuentas, asignación de roles, permisos y activación o desactivación de usuarios. |
| Personal | Registro y actualización de trabajadores, con consulta de su historial y los estados de sesión y participación por separado. |
| Capacitaciones | Registro de cursos, programación y modificación de sesiones, asignación individual o grupal, asistencia, resultados y certificados. |
| Inicio | Resumen de capacitaciones y vencimientos según el perfil. |
| Mis capacitaciones | Historial propio del trabajador, con fecha, horario, modalidad, resultado, certificado y vigencia. |
| Seguimiento | Consulta de certificados y días restantes, con filtros por trabajador o DNI, curso, institución emisora, estado y rango de vencimiento. |
| Alertas | Avisos de asignación, reprogramación, cancelación, recordatorios y vencimientos. |
| Reportes | Consulta del consolidado con filtros y exportación a PDF. |

### Usuarios y acceso

Se ingresa con el **nombre de usuario y la contraseña**. El correo se registra como dato de contacto. La opción **Crear primer administrador** aparece cuando la base todavía carece de cuentas.

El administrador gestiona usuarios, roles y permisos. Con los permisos iniciales, el responsable gestiona Personal y Capacitaciones, el supervisor consulta la información y el trabajador accede a sus propios datos.

Todos los perfiles pueden cambiar su contraseña desde el menú. Deben ingresar la actual y confirmar la nueva, de entre 12 y 128 caracteres. Después vuelven al login. Los formularios incluyen un botón de ojo para mostrar u ocultar cada contraseña.

Los cambios de rol, permisos, contraseña o estado invalidan las sesiones anteriores cuando se comprueba su vigencia o se intenta realizar una operación protegida. El sistema exige conservar al menos un administrador activo.

### Capacitaciones y seguimiento

En **Capacitaciones → Participantes** se puede buscar por DNI o nombre y seleccionar varios trabajadores activos. La selección se conserva al cambiar la búsqueda. Las inscripciones y sus avisos se guardan juntos. Si una inscripción falla, se revierte el grupo completo.

Cuando cambia el curso, la fecha, el horario o la modalidad de una sesión, sus participantes pendientes reciben un aviso con la programación actualizada. La cancelación también genera un aviso.

El registro de resultados requiere asistencia. La nota es opcional y admite valores de 0 a 20. Para registrar un certificado debe existir un resultado favorable. La evidencia del certificado se conserva como una ruta a un archivo local.

En Certificados se registra también la institución emisora, por ejemplo SUCAMEC o APN. Es un dato opcional: los registros antiguos muestran «Sin registrar» hasta que se complete. Seguimiento permite buscar por una parte del nombre de la institución o del trabajador, sin distinguir mayúsculas ni tildes. Los filtros se combinan y las fechas incluyen ambos extremos. Los certificados sin vencimiento aparecen cuando no se limita el rango de fechas.

### Vigencias y alertas

Inicio, Historial, Seguimiento y Reportes calculan la vigencia con la fecha de MySQL y el plazo configurado. Un certificado pasa a vencido después de su fecha de vencimiento. Los certificados sin fecha de vencimiento figuran como NO_APLICA.

Desde **Alertas → Configurar plazos**, el administrador puede definir por separado la anticipación de vencimientos y recordatorios, entre 0 y 365 días. Los valores iniciales son 30 días para certificados y 3 para capacitaciones. Cero considera el día actual para la anticipación. Los cambios se aplican en la siguiente consulta.

Los avisos de asignación y cambios de programación se guardan al realizar la operación. Los recordatorios y avisos de vencimiento se generan al consultar Alertas. El trabajador consulta y marca sus propios avisos como leídos. Consultar nuevamente conserva la lectura y evita repetir el mismo evento.

Los avisos se muestran dentro de SIGECAP. Las evidencias y los PDF exportados se gestionan como archivos locales.

## Qué falta para la entrega

Los módulos del alcance académico están implementados. La regresión comprueba el flujo de capacitación, los cuatro perfiles, los filtros de seguimiento, la actualización del esquema y la restauración de un respaldo en otra instancia de MySQL.

Antes de presentar, debemos aplicar las migraciones en el equipo que se usará, preparar sus cuentas y recorrer con el grupo un caso completo, desde la asignación hasta el certificado y el reporte. Esa revisión confirma la instalación y la aceptación del grupo; las pruebas automáticas se ejecutan en una base separada.

La entrega es una aplicación de escritorio para Operaciones. Las alertas son internas, las evidencias son archivos locales y no se integran servicios de SUCAMEC, APN ni mensajería externa. El empaquetado con Maven quedó aplazado; se puede ejecutar con IntelliJ, PowerShell o Docker.

## Cómo ejecutarlo

Ejecuta los comandos desde la raíz del repositorio.

### Preparar MySQL

Necesitas JDK 17 o superior, MySQL y MySQL Connector/J. Las pruebas de integración usan MySQL 8.4.

Para una base nueva, ejecuta el esquema y prepara la cuenta de conexión siguiendo la guía. Para una instalación existente, aplica `003-plazos-alertas.sql` si todavía falta la configuración de plazos y `004-entidad-certificado.sql` para la institución emisora. Ambos scripts se pueden repetir y conservan los registros. No vuelvas a ejecutar el esquema inicial sobre tu base existente. [Guía de base de datos](database/README.md)

### Desde IntelliJ o PowerShell

Configura estas variables en el proceso que inicia Java.

| Variable | Contenido |
| --- | --- |
| SIGECAP_DB_URL | URL JDBC de tu instalación, por ejemplo jdbc:mysql://localhost:3306/sigecap_jv. |
| SIGECAP_DB_USER | Cuenta de MySQL con permisos para la aplicación. |
| SIGECAP_DB_PASSWORD | Contraseña de esa cuenta MySQL. |
| SIGECAP_JDBC_JAR | Ruta a Connector/J, si el lanzador necesita una ubicación distinta de la caché Maven. |

Las credenciales de conexión se guardan en la configuración local del equipo.

En IntelliJ abre la raíz del repositorio, selecciona el JDK y ejecuta **SIGECAP MySQL**. La clase de entrada es `pe.utp.sigecapjv.vista.FrmPrincipal`, que abre el login.

Desde PowerShell, con las variables configuradas, ejecuta lo siguiente.

```powershell
.\aplicacion\ejecutar.ps1
```

Si necesitas indicar la ubicación del JDK o del conector, sustituye las rutas de ejemplo por las de tu equipo.

```powershell
.\aplicacion\ejecutar.ps1 -JavaHome 'C:\ruta\al\jdk' -ConnectorJar 'C:\ruta\mysql-connector-j-26.7.0.jar'
```

### Con Docker

En Windows 11, la ventana Swing utiliza Docker Desktop con WSL 2 y WSLg. Copia `.env.example` como `.env` y define `SIGECAP_MYSQL_ROOT_PASSWORD` y `SIGECAP_DB_PASSWORD`.

```powershell
docker compose up -d --build
```

Para volver a abrir la aplicación después de cerrarla, ejecuta `docker compose up -d app`. Para detener los servicios conservando el volumen de datos, usa `docker compose down`.

También puedes levantar la base con `docker compose up -d db` y ejecutar Java en IntelliJ o PowerShell. Con la configuración predeterminada, usa `jdbc:mysql://localhost:3307/sigecap_jv`, la cuenta `sigecap_app` y la contraseña definida en `.env`. Configura esas variables también en el proceso de Java.

La instalación Docker usa su propio volumen y sus propias cuentas. Los scripts de inicialización se ejecutan al crear ese volumen. En una base existente, las migraciones y los cambios de contraseña se aplican sobre esa instalación. Los PDF exportados desde Docker quedan en `reportes/`.

## Pruebas

Para comprobar modelos, controladores y eventos Swing, ejecuta lo siguiente.

```powershell
.\aplicacion\ejecutar.ps1 -Probar
```

La regresión con MySQL requiere Docker y Connector/J. Crea una base temporal con cuentas de prueba y elimina su contenedor al finalizar.

```powershell
.\aplicacion\probar-mysql.ps1
```

Para comprobar también el ingreso y la navegación de los cuatro perfiles, utiliza un escritorio activo.

```powershell
.\aplicacion\probar-mysql.ps1 -Ventanas
```

Las pruebas cubren persistencia, asignación grupal, asistencia, resultados, certificados e institución emisora, usuarios, permisos, cambio de contraseña, sesiones, historial, alertas, vigencias y reportes. Comprueban la migración desde la estructura anterior y su repetición. Al finalizar, restauran un respaldo en otro contenedor y comparan la estructura y los datos por SHA-256. Las capturas de revisión quedan en `aplicacion/out/revision-perfiles/`, una carpeta de salida excluida de Git.

## Trabajo en equipo

| Integrante | Responsabilidad | Rama |
| --- | --- | --- |
| Adrian Pisco | Seguridad y control de acceso | feature/seguridad-acceso |
| Anthony Sacha | Historial y consulta del trabajador | feature/historial-trabajador |
| Cristofher Chirihuana | Vigencias y notificaciones | feature/vigencias-notificaciones |
| Lijhoan Machaca | Reportes e integración | feature/reportes-integracion |

Cada integrante trabaja en su rama y abre un pull request hacia `main`. Adrian revisa los cambios antes de integrarlos. Las modificaciones de clases compartidas y base de datos se coordinan con el equipo.

El código está en `aplicacion/src/`, las pruebas en `aplicacion/test/` y el esquema y las migraciones en `database/`. Las credenciales, la configuración personal, los archivos generados y el contenido de `privado/` se mantienen fuera del repositorio.
