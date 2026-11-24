# MySQL y persistencia de SIGECAP J&V

Esta guía describe el esquema, las migraciones, los permisos de conexión y los respaldos. La aplicación requiere Java 17 o superior y se valida con MySQL 8.4.

## Esquema

`001-esquema.sql` contiene el esquema actual: 12 tablas del diseño original, una tabla de configuración y la vista `vw_control_vigencias`. Incluye la institución emisora del certificado. Ejecútalo una sola vez en una base nueva desde MySQL Workbench. Para instalaciones existentes, utiliza las migraciones siguientes.

### Actualizar los plazos en una instalación existente

Desde Workbench, con una cuenta administrativa y conectado a la misma instancia que usa Java, ejecuta `003-plazos-alertas.sql`. Puedes repetirlo: conserva los plazos que haya guardado el administrador. Después otorga estos permisos a la cuenta de conexión de la aplicación. Ajusta el usuario y el host si tu instalación usa otra cuenta.

```sql
GRANT SELECT, UPDATE ON sigecap_jv.configuracion_alertas TO 'sigecap_app'@'%';
```

En un volumen Docker existente aplica los mismos pasos al MySQL del puerto publicado. En volúmenes nuevos Compose ya crea la tabla y concede los permisos.

### Actualizar la institución emisora

Antes de usar esta versión con una base existente, ejecuta `004-entidad-certificado.sql` en Workbench, con una cuenta administrativa y conectado a la instancia que utiliza Java. Añade `certificado.entidadEmisora` como texto opcional de hasta 160 caracteres. Conserva los certificados y deja el dato vacío en los anteriores. Se puede repetir sin duplicar la columna. Las bases nuevas ya la incluyen en `001-esquema.sql`.

No requiere permisos adicionales para la cuenta de aplicación si ya tiene SELECT, INSERT y UPDATE sobre `certificado`. Guarda un respaldo antes de actualizar una base con información que quieras conservar.

El flujo implementado persiste `trabajador`, `curso`, `capacitacion`, `participacion`, `asistencia`, `resultado` y `certificado`. Usuarios, roles, permisos y notificaciones también están integrados. La vista obtiene el estado actual por fecha; las alertas preventivas usan el plazo guardado en configuracion_alertas (inicialmente 30 días). El estado de vigencia se calcula al consultar las fechas; `estadoVigencia` se guarda como NULL para evitar estados desactualizados.

## Conexión local

Para habilitar la eliminación controlada de cursos en una instalación existente, ejecuta `005-permiso-eliminar-curso.sql` con una cuenta administrativa. El ejemplo usa `sigecap_app`@`%`; ajusta ambos valores si tu cuenta es distinta. Solo añade DELETE sobre `curso`, sin modificar tablas ni claves foráneas. Docker concede este permiso al crear un volumen nuevo.

Configura las [variables de conexión descritas en el README](../README.md#desde-intellij-o-powershell) en el proceso que inicia Java. La URL predeterminada es `jdbc:mysql://localhost:3306/sigecap_jv`.

La cuenta de aplicación necesita estos permisos, iguales a los que configura `docker/002-permisos-app.sh` en una instalación nueva:

| Objetos | Permisos |
| --- | --- |
| `trabajador`, `curso`, `capacitacion`, `participacion`, `asistencia`, `resultado`, `certificado`, `notificacion`, `usuario`, `rol`, `permiso`, `rol_permiso` | SELECT, INSERT, UPDATE |
| `rol_permiso` | DELETE, para revocar permisos |
| `curso` | DELETE, solo para cursos sin capacitaciones asociadas |
| `configuracion_alertas` | SELECT, UPDATE |
| `vw_control_vigencias` | SELECT |

Usa una cuenta administrativa separada para crear el esquema y aplicar migraciones. Docker es opcional si ya tienes MySQL instalado.

La aplicación exige `capacitaciones.gestionar` y comprueba la sesión dentro de la transacción de eliminación. Bloquea el curso antes de comprobar su uso. La clave foránea `ON DELETE RESTRICT` sigue impidiendo eliminarlo si existe cualquier capacitación asociada. La cuenta MySQL tiene permiso de borrado sobre esa tabla; la restricción de relaciones sigue siendo responsabilidad del servidor.

En IntelliJ selecciona **SIGECAP MySQL** y configura las variables en la configuración local de ejecución, sin incorporarlas al XML compartido. También puedes iniciar IntelliJ desde una consola donde ya estén definidas. Si falta el SDK, selecciona tu JDK instalado. La biblioteca del proyecto usa `com.mysql:mysql-connector-j:26.7.0`; IntelliJ puede resolverla desde Maven.

Para ejecutar desde PowerShell, situado en la raíz del repositorio y con las variables de conexión definidas:

```powershell
.\aplicacion\ejecutar.ps1 -JavaHome 'C:\ruta\al\jdk' -ConnectorJar 'C:\ruta\mysql-connector-j-26.7.0.jar'
```

Sustituye ambas rutas por las instaladas en tu equipo. `-JavaHome` es opcional si JAVA_HOME o PATH ya resuelven el JDK. `-ConnectorJar` es opcional si SIGECAP_JDBC_JAR está definida o si el JAR existe en `%USERPROFILE%\.m2\repository\com\mysql\mysql-connector-j\26.7.0\`.

Los errores distinguen entre servicio inaccesible, credenciales o permisos de MySQL y estructura pendiente de actualización. Los mensajes no incluyen credenciales ni consultas SQL. Las operaciones tienen tiempos de espera de conexión y lectura. Inicio, Historial, Mis capacitaciones y Seguimiento cargan sus consultas en segundo plano. Algunas pantallas todavía consultan de forma síncrona; una caída de la base puede bloquearlas brevemente hasta el tiempo de espera.

## Pruebas reproducibles

Desde la raíz del repositorio:

```powershell
# Validaciones del modelo, controladores en memoria y eventos Swing.
.\aplicacion\ejecutar.ps1 -Probar

# Requiere Docker activo y Connector/J disponible; no usa tu base real.
.\aplicacion\probar-mysql.ps1
```

El segundo comando crea un MySQL 8.4 aislado con puerto aleatorio enlazado solo a 127.0.0.1, credenciales aleatorias y el esquema adjunto. Comprueba CRUD, restricciones, estados, rollback, asignaciones concurrentes, edición de permisos, invalidación de sesiones y persistencia en una segunda JVM. Simula la estructura anterior del certificado y aplica dos veces la migración de institución emisora. Finalmente restaura un respaldo en otra instancia, compara estructura y datos por SHA-256 y consulta la vista de vigencias. Elimina exclusivamente los contenedores, volúmenes y archivos temporales de esa prueba, y restaura las variables del proceso. La imagen Docker puede permanecer como caché reutilizable.

### Respaldo de una instalación local

Desde PowerShell, con las herramientas cliente de MySQL instaladas, crea una carpeta privada y exporta la base. Ajusta host, puerto y cuenta a la instancia que utiliza Java. `--password` solicita la clave por teclado; no la escribas en el comando.

```powershell
New-Item -ItemType Directory -Force .\privado\respaldos | Out-Null
$respaldo = Join-Path (Resolve-Path .\privado\respaldos) ('sigecap-' + (Get-Date -Format yyyyMMdd-HHmmss) + '.sql')
mysqldump --host=127.0.0.1 --port=3306 --user=root --password --single-transaction --no-tablespaces --set-gtid-purged=OFF --result-file="$respaldo" sigecap_jv
if ($LASTEXITCODE -ne 0) { throw 'El respaldo no terminó correctamente.' }
```

Conserva también los archivos de evidencia: la base guarda sus rutas, no el contenido. Comprueba la restauración en otra instancia MySQL con una base vacía llamada `sigecap_jv`; no importes un respaldo de prueba sobre la instalación de trabajo. La comprobación automatizada de `verificar-respaldo.ps1` está reservada al contenedor desechable creado por `probar-mysql.ps1` y no sustituye el respaldo de los datos reales.

`SIGECAP_DB_TEST=true` es una protección adicional del ejecutable de pruebas, no una autorización para probar sobre datos reales. Utiliza siempre el script aislado.

## Correspondencia con Java

- Los DAO usan consultas parametrizadas y cierran conexiones, sentencias y resultados.
- `PersonalJdbcControlador` y `CapacitacionJdbcControlador` conservan la validación del modelo y coordinan transacciones. Los controladores originales en memoria se mantienen para las pruebas.
- MySQL genera los identificadores. Los objetos reciben el identificador después de confirmar la transacción.
- Programar bloquea el curso; asignar bloquea capacitación y trabajador mientras se validan sus estados. La restricción UNIQUE impide asignaciones duplicadas incluso con dos sesiones.
- Las participaciones se reconstruyen al leer las capacitaciones, sin ejecutar otra vez la asignación del modelo.
- `DATE` se mapea a `LocalDate`, `TIME` a `LocalTime` y `vigenciaMeses` permite NULL. `DATETIME` se mapea a `LocalDateTime` y las notas `DECIMAL` a `BigDecimal` (0 a 20, hasta dos decimales).

La autenticación usa las cuentas de `usuario`, sus roles y permisos. El primer administrador se crea desde el login solo cuando no existe ninguna cuenta; no hay claves predeterminadas. Si ya tienes usuarios, utiliza una cuenta con un hash PBKDF2 compatible; no se aceptan contraseñas en texto plano. Las notificaciones se consultan desde Alertas con autorización de Capacitaciones; su generación requiere gestionar ese módulo.

Registrar un resultado marca la participación como COMPLETADA en la misma transacción, incluso si fue desaprobado. Resultados y certificados pueden registrarse o corregirse tras cerrar la capacitación. Las capacitaciones con seguimiento no se pueden reprogramar ni cancelar. Un resultado existente bloquea el cambio a inasistencia; un certificado existente bloquea el cambio a resultado desfavorable. El vencimiento se toma del documento recibido: vacío significa que no aplica. El día del vencimiento todavía es vigente.

El acceso por rol se aplica en los controladores de la aplicación. Como es un cliente de escritorio con conexión JDBC directa, las credenciales de MySQL son de confianza y deben permanecer en el equipo autorizado; estos controles no sustituyen los permisos del servidor MySQL. La opción JVM `sigecap.pruebas` se activa únicamente en los scripts de pruebas para los controladores sin sesión; no se usa al iniciar normalmente.
