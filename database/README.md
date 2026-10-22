# MySQL y persistencia de SIGECAP J&V

La aplicación funcional requiere Java 17 o superior y MySQL 8.4. Los mockups de la carpeta hermana no se conectan a la base.

## Esquema

`001-esquema.sql` conserva el diseño físico entregado: 12 tablas InnoDB y la vista `vw_control_vigencias`. Ejecútalo una sola vez en una base nueva desde MySQL Workbench. Si ya creaste las tablas, no vuelvas a ejecutarlo: no es una migración incremental ni borra registros existentes.

El flujo implementado persiste `trabajador`, `curso`, `capacitacion`, `participacion`, `asistencia`, `resultado` y `certificado`. Las demás tablas están preparadas para etapas posteriores. La vista obtiene el estado actual por fecha; el umbral de próximo vencimiento todavía debe definirse. El estado de vigencia se calcula al consultar las fechas; `estadoVigencia` se guarda como NULL para evitar estados desactualizados.

## Conexión local

La aplicación lee estas variables del proceso que la inicia:

| Variable | Uso |
| --- | --- |
| `SIGECAP_DB_URL` | URL JDBC; por defecto `jdbc:mysql://localhost:3306/sigecap_jv`. |
| `SIGECAP_DB_USER` | Usuario MySQL de la aplicación. |
| `SIGECAP_DB_PASSWORD` | Contraseña de ese usuario. No guardarla en Git. |
| `SIGECAP_JDBC_JAR` | Ruta al JAR de Connector/J para el lanzador PowerShell, si no está en la caché Maven indicada abajo. |

Usa una cuenta de aplicación con permisos SELECT, INSERT y UPDATE sobre las siete tablas del flujo y sobre `usuario`, `rol`, `permiso` y `rol_permiso`. Estas cuatro tablas ya forman parte del esquema; no hace falta recrearlo. La creación del esquema se hace con una cuenta administrativa separada. No se requiere acceso a Docker para trabajar con el MySQL que ya tienes en Workbench.

En IntelliJ selecciona **SIGECAP MySQL** y configura las variables en la configuración local de ejecución, sin incorporarlas al XML compartido. También puedes iniciar IntelliJ desde una consola donde ya estén definidas. Si falta el SDK, selecciona tu JDK instalado. La biblioteca del proyecto usa `com.mysql:mysql-connector-j:26.7.0`; IntelliJ puede resolverla desde Maven.

Para ejecutar desde PowerShell, situado en la raíz del repositorio y con las variables de conexión definidas:

```powershell
.\aplicacion\ejecutar.ps1 -JavaHome 'C:\ruta\al\jdk' -ConnectorJar 'C:\ruta\mysql-connector-j-26.7.0.jar'
```

Sustituye ambas rutas por las instaladas en tu equipo. `-JavaHome` es opcional si JAVA_HOME o PATH ya resuelven el JDK. `-ConnectorJar` es opcional si SIGECAP_JDBC_JAR está definida o si el JAR existe en `%USERPROFILE%\.m2\repository\com\mysql\mysql-connector-j\26.7.0\`.

No se cambia silenciosamente a memoria cuando falla MySQL. El error se muestra al usuario; no se muestran credenciales ni consultas SQL. Las operaciones tienen tiempos de espera de conexión y lectura. En esta etapa las consultas Swing son síncronas; una caída de la base puede bloquear brevemente la interfaz hasta el tiempo de espera.

## Pruebas reproducibles

Desde la raíz del repositorio:

```powershell
# Validaciones del modelo, controladores en memoria y eventos Swing.
.\aplicacion\ejecutar.ps1 -Probar

# Requiere Docker activo y Connector/J disponible; no usa tu base real.
.\aplicacion\probar-mysql.ps1
```

El segundo comando crea un MySQL 8.4 aislado con puerto aleatorio enlazado solo a 127.0.0.1, credenciales aleatorias y el esquema adjunto. Comprueba CRUD, restricciones, estados, rollback, asignaciones concurrentes y persistencia en una segunda JVM. Finalmente elimina exclusivamente su contenedor y volumen y restaura las variables del proceso. La imagen Docker puede permanecer como caché reutilizable.

`SIGECAP_DB_TEST=true` es una protección adicional del ejecutable de pruebas, no una autorización para probar sobre datos reales. Utiliza siempre el script aislado.

## Correspondencia con Java

- Los DAO usan consultas parametrizadas y cierran conexiones, sentencias y resultados.
- `PersonalJdbcControlador` y `CapacitacionJdbcControlador` conservan la validación del modelo y coordinan transacciones. Los controladores originales en memoria se mantienen para las pruebas.
- MySQL genera los identificadores. Los objetos reciben el identificador después de confirmar la transacción.
- Programar bloquea el curso; asignar bloquea capacitación y trabajador mientras se validan sus estados. La restricción UNIQUE impide asignaciones duplicadas incluso con dos sesiones.
- Las participaciones se reconstruyen al leer las capacitaciones, sin ejecutar otra vez la asignación del modelo.
- `DATE` se mapea a `LocalDate`, `TIME` a `LocalTime` y `vigenciaMeses` permite NULL. `DATETIME` se mapea a `LocalDateTime` y las notas `DECIMAL` a `BigDecimal` (0 a 20, hasta dos decimales).

La autenticación usa las cuentas de `usuario`, sus roles y permisos. El primer administrador se crea desde el login solo cuando no existe ninguna cuenta; no hay claves predeterminadas. Si ya tienes usuarios, utiliza una cuenta con un hash PBKDF2 compatible; no se aceptan contraseñas en texto plano. Las notificaciones siguen pendientes.

Registrar un resultado marca la participación como COMPLETADA en la misma transacción, incluso si fue desaprobado. Resultados y certificados pueden registrarse o corregirse tras cerrar la capacitación. Las capacitaciones con seguimiento no se pueden reprogramar ni cancelar. Un resultado existente bloquea el cambio a inasistencia; un certificado existente bloquea el cambio a resultado desfavorable. El vencimiento se toma del documento recibido: vacío significa que no aplica. El día del vencimiento todavía es vigente.

El acceso por rol se aplica en los controladores de la aplicación. Como es un cliente de escritorio con conexión JDBC directa, las credenciales de MySQL son de confianza y deben permanecer en el equipo autorizado; estos controles no sustituyen los permisos del servidor MySQL. La opción JVM `sigecap.pruebas` se activa únicamente en los scripts de pruebas para los controladores sin sesión; no se usa al iniciar normalmente.
