# SIGECAP J&V

Sistema de gestión de cursos de capacitación desarrollado como proyecto académico con Java 17, Swing y MySQL. El código está organizado en modelos, vistas, controladores y DAO.

## Avance

Ya se puede registrar personal y cursos, programar capacitaciones, asignar trabajadores y registrar asistencia, resultados y certificados. Estos datos se guardan en MySQL.

| Hito | Estado |
| --- | --- |
| Personal, cursos, programación y participantes | Implementado |
| Asistencia: registro, consulta y actualización | Implementado |
| Resultados y certificados | Implementado |
| Historial y alertas del trabajador | Pendiente |
| Usuarios, roles y acceso | Login, usuarios, permisos editables y control de sesiones implementados |
| Reportes | Pendiente |

Las clases y vistas de los módulos pendientes se conservan para continuar el desarrollo. Las pruebas de Swing verifican los eventos; la revisión visual de las ventanas se hace en IntelliJ.

Al ejecutar se abre el login. Ingresa con tu **nombre de usuario y contraseña**; el correo se registra como dato de contacto y no se utiliza para autenticar. Si ya creaste tu cuenta, no tienes que registrarla otra vez. La opción **Crear primer administrador** solo aparece cuando se confirma que la base no tiene usuarios y se oculta después del registro. En una instalación nueva, define una contraseña de 12 a 128 caracteres. Después puedes registrar cuentas, asignar roles, cambiar su estado y renovar sus contraseñas desde Usuarios. No se permite desactivar ni quitar el rol al último administrador activo. Al editar, dejar la contraseña vacía conserva la actual.

Inicialmente, el administrador gestiona Personal, Capacitaciones y Usuarios; el responsable gestiona Personal y Capacitaciones; el supervisor tiene acceso de consulta. En **Usuarios → Roles y permisos** puedes ajustar estos permisos y guardarlos para cada rol. Gestionar requiere consultar; el acceso administrativo no se puede quitar. Los módulos propios del trabajador, reportes y alertas siguen pendientes de integración. El rol Trabajador requiere asociar el ID de un trabajador existente. Cambiar el rol, sus permisos o el vínculo del trabajador invalida la sesión en la siguiente operación. La ventana también lo revisa cada 15 segundos y al recuperar el foco; si el acceso cambió, la cuenta fue desactivada o no se puede verificar la conexión, vuelve al login. Dos administradores no pueden sobrescribir permisos cargados antes del cambio del otro: hay que recargar primero.

Los resultados admiten notas opcionales de 0 a 20 y requieren asistencia. La condición (Aprobado, Desaprobado, Apto o No apto) se registra según la evaluación recibida. El certificado requiere un resultado favorable; se guardan su número, fechas y ruta de evidencia. La ruta referencia el archivo local, no lo copia ni lo publica. La vigencia se calcula al consultar; las alertas de próximo vencimiento corresponden al siguiente hito.

## Trabajo en equipo

| Integrante | Trabajo | Rama |
| --- | --- | --- |
| Adrian Pisco | Usuarios, roles, permisos y login (HU-012, HU-013) | `feature/seguridad-acceso` |
| Anthony Sacha | Historial y Mis capacitaciones (HU-008, HU-014) | `feature/historial-trabajador` |
| Cristofher Chirihuana | Vigencias y notificaciones (HU-009, HU-010) | `feature/vigencias-notificaciones` |
| Lijhoan Machaca | Reportes, inicio e integración (HU-011) | `feature/reportes-integracion` |

Cada integrante publica sus avances en su rama y abre un pull request hacia `main`. Un PR puede incluir varios commits y se actualiza con los siguientes push de esa misma rama. Los cambios de los compañeros pasan por la revisión de Adrian antes de integrarse. Hacer push no significa que el trabajo ya esté en `main`.

Antes de solicitar revisión, ejecutar las pruebas que correspondan y describir qué cambió, cómo se probó y qué queda pendiente. Los cambios de base de datos o clases compartidas se coordinan con el equipo. Esta organización debe acompañarse de reglas de protección en GitHub; el README por sí solo no bloquea las fusiones.

## Ejecutar

Se necesita un JDK 17 o superior, MySQL 8.4 y MySQL Connector/J. La [guía de conexión](database/README.md) explica cómo crear el esquema y configurar las credenciales.

En IntelliJ, abrir esta carpeta, seleccionar el JDK y ejecutar **SIGECAP MySQL**. La clase principal es `pe.utp.sigecapjv.vista.FrmPrincipal`.

Desde PowerShell, en la raíz del proyecto y con la conexión configurada:

```powershell
.\aplicacion\ejecutar.ps1
```

El script toma el JDK de `JAVA_HOME` o `PATH`. Si es necesario, se pueden indicar las rutas con `-JavaHome` y `-ConnectorJar`.

### Con Docker

`docker-compose.yml` levanta MySQL 8.4 y la aplicación, sin JDK ni MySQL instalados. Requiere Windows 11 con Docker Desktop (WSL 2): la ventana Swing se muestra mediante WSLg.

```powershell
# Una vez: copiar .env.example como .env y definir SIGECAP_MYSQL_ROOT_PASSWORD y SIGECAP_DB_PASSWORD.
docker compose up -d --build   # base y aplicación
docker compose up -d app       # volver a abrir la ventana tras cerrarla
docker compose down            # detener; los datos quedan en el volumen sigecap-datos
```

El esquema y la cuenta `sigecap_app` (SELECT, INSERT y UPDATE sobre las siete tablas del flujo y las cuatro tablas de seguridad; DELETE únicamente en rol_permiso) se crean solo la primera vez que se inicializa el volumen. Los permisos de seguridad permiten integrar el login y la administración de usuarios del PR correspondiente; este cambio de Docker no implementa esos módulos. La base queda publicada en `127.0.0.1:3307` para Workbench, separada del MySQL local. `docker compose down -v` borra el volumen y todos sus datos.

Para ejecutar Java en el equipo usando esta base, configure `SIGECAP_DB_URL=jdbc:mysql://localhost:3307/sigecap_jv`, `SIGECAP_DB_USER=sigecap_app` y `SIGECAP_DB_PASSWORD` en IntelliJ o en las variables de entorno de PowerShell. `.env` lo carga Compose, no `ejecutar.ps1`. Esta base es independiente de la que ya tenga instalada en el puerto 3306: sus usuarios y datos no se copian automáticamente.

Si ya existía el volumen, cambiar `.env` o el script de permisos no actualiza las cuentas existentes. Conserve el volumen y ajuste las credenciales/permisos desde MySQL con una cuenta administradora.

La opción gráfica depende de que Docker Desktop tenga disponible el socket WSLg indicado en los montajes. Si no está disponible, use `docker compose up -d db` y ejecute la aplicación desde IntelliJ o PowerShell con la conexión anterior.

## Carpetas

- `aplicacion/src/`: código del sistema.
- `aplicacion/test/`: pruebas del modelo, los controladores, Swing y MySQL.
- `database/`: esquema SQL e instrucciones de conexión.
- `.idea/`: configuración compartida para abrir y ejecutar el proyecto en IntelliJ.

## Pruebas

Desde la raíz del proyecto:

```powershell
# Pruebas sin base de datos.
.\aplicacion\ejecutar.ps1 -Probar

# Pruebas con MySQL temporal; requiere Docker y Connector/J.
.\aplicacion\probar-mysql.ps1
```

Las pruebas de MySQL crean su propia base y eliminan el contenedor al terminar. No deben ejecutarse sobre datos reales.

Las credenciales, los archivos `.env`, las preferencias personales del editor y el contenido de `privado/` quedan fuera de Git. `privado/` es solo almacenamiento local: no tiene respaldo en este repositorio.
