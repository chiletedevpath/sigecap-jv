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
| Usuarios, roles y acceso | Pendiente |
| Reportes | Pendiente |

Las clases y vistas de los módulos pendientes se conservan para continuar el desarrollo. Las pruebas de Swing verifican los eventos; la revisión visual de las ventanas se hace en IntelliJ.

Los resultados admiten notas opcionales de 0 a 20 y requieren asistencia. La condición (Aprobado, Desaprobado, Apto o No apto) se registra según la evaluación recibida. El certificado requiere un resultado favorable; se guardan su número, fechas y ruta de evidencia. La ruta referencia el archivo local, no lo copia ni lo publica. La vigencia se calcula al consultar; las alertas de próximo vencimiento corresponden al siguiente hito.

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

El esquema y la cuenta `sigecap_app` (SELECT, INSERT y UPDATE sobre las siete tablas del flujo) se crean solo la primera vez que se inicializa el volumen. La base queda publicada en `127.0.0.1:3307` para Workbench, separada del MySQL local. `docker compose down -v` borra el volumen y todos sus datos.

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
