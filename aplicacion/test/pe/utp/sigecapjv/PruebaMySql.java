package pe.utp.sigecapjv;

import java.sql.*;
import java.time.*;
import java.util.concurrent.*;
import javax.swing.*;
import java.awt.*;
import pe.utp.sigecapjv.conexion.ConexionBD;
import pe.utp.sigecapjv.controlador.*;
import pe.utp.sigecapjv.dao.*;
import pe.utp.sigecapjv.modelo.*;

public class PruebaMySql {
    private static int comprobaciones;
    private static void comprobar(boolean valor, String mensaje) {
        if (!valor) throw new AssertionError(mensaje);
        comprobaciones++;
    }
    private static void rechazar(Runnable accion) {
        try { accion.run(); }
        catch (IllegalArgumentException | IllegalStateException | SecurityException ex) { comprobaciones++; return; }
        throw new AssertionError("La operación inválida fue aceptada.");
    }
    private static Trabajador trabajador(String dni) {
        return new Trabajador(0, dni, "Prueba", "Persistencia", "Operaciones", "ACTIVO");
    }
    private static Curso curso(String codigo) {
        return new Curso(0, codigo, "Curso de prueba", "Interno", null, "ACTIVO");
    }
    private static Capacitacion capacitacion(Curso curso) {
        return new Capacitacion(0, LocalDate.now().plusDays(5), LocalTime.of(9, 0),
                LocalTime.of(12, 0), "Presencial", null, curso);
    }

    private static boolean contieneFilas(Container contenedor, int filas) {
        for (Component componente : contenedor.getComponents()) {
            if (componente instanceof JTable tabla && tabla.getRowCount() == filas) return true;
            if (componente instanceof Container hijo && contieneFilas(hijo, filas)) return true;
        }
        return false;
    }

    public static void main(String[] args) throws Exception {
        if (!"true".equals(System.getenv("SIGECAP_DB_TEST"))) {
            throw new IllegalStateException("Solo ejecutar en una base aislada con SIGECAP_DB_TEST=true.");
        }
        if (args.length > 0 && "sin-conexion".equals(args[0])) {
            rechazar(() -> Jdbc.transaccion(c -> null));
            System.out.println("OK: fallo de conexión propagado.");
            return;
        }
        if (UsuarioControlador.requiereAdministradorInicial()) PruebaUsuariosMySql.inicializar();
        try (SesionUsuario admin = new LoginControlador().autenticar(
                "admin_prueba", "Clave-usuarios-prueba-2026".toCharArray())) {
            PersonalJdbcControlador personal = new PersonalJdbcControlador(admin);
            CapacitacionJdbcControlador gestion = new CapacitacionJdbcControlador(personal, admin);
            // Una segunda JVM debe recuperar lo registrado por la primera.
            if (!personal.listarTrabajadores().isEmpty()) {
                comprobar(personal.listarTrabajadores().stream().anyMatch(t -> "87654321".equals(t.getDni())), "Persistencia del trabajador entre procesos");
                comprobar(gestion.listarParticipaciones().size() == 3, "Persistencia de asignaciones entre procesos");
                int evaluada = gestion.listarParticipaciones().stream().filter(p -> "EVALUACION".equals(p.getCapacitacion().getCurso().getCodigo())).findFirst().orElseThrow().getIdParticipacion();
                comprobar("APTO".equals(new SeguimientoControlador(admin).consultarResultado(evaluada).getCondicion()), "Resultado entre procesos");
                comprobar("CERT-EVAL-EDITADO".equals(new SeguimientoControlador(admin).consultarCertificado(evaluada).getNumero()), "Certificado entre procesos");
                comprobar(gestion.listarCapacitaciones().stream().anyMatch(c -> "CERRADA".equals(c.getEstado())), "Persistencia del cierre");
                int participacion = gestion.listarParticipaciones().get(0).getIdParticipacion();
                comprobar("JUSTIFICADA".equals(new SeguimientoControlador(admin).consultarAsistencia(participacion).getCondicion()),
                        "Asistencia persistida entre procesos");
                System.out.println("OK: " + comprobaciones + " comprobaciones entre procesos.");
                PruebaAccesoMySql.verificarPersistencia();
                // Al final: agrega datos propios que alterarían los conteos anteriores.
                PruebaReportesMySql.verificar();
                PruebaNotificacionesMySql.verificar();
                PruebaHistorialMySql.verificar();
                return;
            }

            try (Connection c = ConexionBD.obtenerConexion();
                 Statement s = c.createStatement()) {
                try (ResultSet r = s.executeQuery("SELECT COUNT(*) FROM information_schema.tables WHERE table_schema=DATABASE() AND table_type='BASE TABLE'")) {
                    r.next(); comprobar(r.getInt(1) == 12, "Doce tablas");
                }
                try (ResultSet r = s.executeQuery("SELECT COUNT(*) FROM information_schema.views WHERE table_schema=DATABASE()")) {
                    r.next(); comprobar(r.getInt(1) == 1, "Una vista");
                }
            }
            Trabajador t = trabajador("12345678");
            personal.registrarTrabajador(t);
            comprobar(t.getIdTrabajador() > 0, "ID generado");
            rechazar(() -> personal.registrarTrabajador(trabajador("12345678")));
            rechazar(() -> personal.registrarTrabajador(trabajador("123")));
            t.setDni("87654321"); t.setNombres("Nombre editado");
            personal.actualizarTrabajador(t);
            comprobar(new PersonalJdbcControlador(admin).buscarTrabajador(t.getIdTrabajador()).getNombres().equals("Nombre editado"), "Actualización persistida");

            Curso curso = curso("PRUEBA");
            gestion.registrarCurso(curso);
            rechazar(() -> gestion.registrarCurso(curso("prueba")));
            curso.setVigenciaMeses(24);
            gestion.actualizarCurso(curso);
            comprobar(gestion.listarCursos().get(0).getVigenciaMeses() == 24, "Vigencia editada");
            curso.setEstado("INACTIVO"); gestion.actualizarCurso(curso);
            rechazar(() -> gestion.programarCapacitacion(capacitacion(curso)));
            curso.setEstado("ACTIVO"); gestion.actualizarCurso(curso);
            Capacitacion cap = capacitacion(curso);
            gestion.programarCapacitacion(cap);
            comprobar(cap.getIdCapacitacion() > 0 && "PROGRAMADA".equals(cap.getEstado()), "Programación persistida");
            cap.setModalidad("Virtual"); gestion.actualizarCapacitacion(cap);
            comprobar("Virtual".equals(gestion.listarCapacitaciones().get(0).getModalidad()), "Edición de programación");
            Capacitacion invalida = capacitacion(curso);
            invalida.setHoraFin(LocalTime.of(8, 0));
            rechazar(() -> gestion.programarCapacitacion(invalida));
            comprobar(gestion.listarCapacitaciones().size() == 1, "Sin programación parcial");
            t.setEstado("INACTIVO"); personal.actualizarTrabajador(t);
            rechazar(() -> gestion.asignarTrabajador(t, cap));
            t.setEstado("ACTIVO"); personal.actualizarTrabajador(t);
            Participacion p = gestion.asignarTrabajador(t, cap);
            comprobar(p.getIdParticipacion() > 0 && "PENDIENTE".equals(p.getEstado()), "Asignación persistida");
            // La extracción a DAO debe conservar el bloqueo de ambas entidades.
            try (Connection lectura = ConexionBD.obtenerConexion(); Connection cambio = ConexionBD.obtenerConexion()) {
                lectura.setAutoCommit(false);
                cambio.setAutoCommit(false);
                try {
                    new ParticipacionDAO().buscar(lectura, p.getIdParticipacion(), true);
                    try (Statement configuracion = cambio.createStatement()) {
                        configuracion.execute("SET SESSION innodb_lock_wait_timeout=1");
                    }
                    try (PreparedStatement cancelar = cambio.prepareStatement("UPDATE capacitacion SET estado='CANCELADA' WHERE idCapacitacion=?")) {
                        cancelar.setInt(1, cap.getIdCapacitacion());
                        try {
                            cancelar.executeUpdate();
                            throw new AssertionError("La capacitación no quedó bloqueada durante el seguimiento.");
                        } catch (SQLException ex) {
                            comprobar(ex.getErrorCode() == 1205, "Cambio concurrente espera el bloqueo de capacitación");
                        }
                    }
                } finally {
                    cambio.rollback();
                    lectura.rollback();
                }
            }
            rechazar(() -> gestion.asignarTrabajador(t, cap));
            comprobar(gestion.listarCapacitaciones().get(0).getParticipaciones().size() == 1, "Composición reconstruida");
            SeguimientoControlador seguimiento = new SeguimientoControlador(admin);
            int idParticipacion = p.getIdParticipacion();
            comprobar(seguimiento.consultarAsistencia(idParticipacion) == null, "Sin asistencia ficticia");
            rechazar(() -> seguimiento.registrarAsistencia(idParticipacion, "ASISTIO"));
            rechazar(() -> seguimiento.registrarAsistencia(999999, "ASISTIO"));
            // Simula que llegó el día del curso exclusivamente en la base aislada.
            Jdbc.transaccion(c -> {
                Jdbc.actualizar(c, "UPDATE capacitacion SET fecha=? WHERE idCapacitacion=?", LocalDate.now(), cap.getIdCapacitacion());
                return null;
            });
            rechazar(() -> seguimiento.actualizarAsistencia(idParticipacion, "ASISTIO"));
            rechazar(() -> seguimiento.registrarAsistencia(idParticipacion, "INVALIDA"));
            comprobar(seguimiento.consultarAsistencia(idParticipacion) == null, "Registro inválido no persiste");
            Asistencia asistencia = seguimiento.registrarAsistencia(idParticipacion, " asistio ");
            comprobar(asistencia.getIdAsistencia() > 0 && "ASISTIO".equals(asistencia.getCondicion()), "Asistencia registrada y normalizada");
            rechazar(() -> seguimiento.registrarAsistencia(idParticipacion, "NO_ASISTIO"));
            rechazar(() -> seguimiento.actualizarAsistencia(idParticipacion, ""));
            comprobar("ASISTIO".equals(seguimiento.consultarAsistencia(idParticipacion).getCondicion()), "Actualización inválida conserva asistencia");
            Asistencia editada = seguimiento.actualizarAsistencia(idParticipacion, "JUSTIFICADA");
            comprobar(editada.getIdAsistencia() == asistencia.getIdAsistencia()
                    && editada.getFechaRegistro().equals(asistencia.getFechaRegistro()), "Edición conserva identidad y fecha original");
            comprobar("JUSTIFICADA".equals(new SeguimientoControlador(admin).consultarAsistencia(idParticipacion).getCondicion()), "Edición persistida");
            PruebaPantallaAsistencia.verificarMySql(gestion, seguimiento);
            gestion.cerrarCapacitacion(cap.getIdCapacitacion());
            rechazar(() -> seguimiento.actualizarAsistencia(idParticipacion, "ASISTIO"));
            rechazar(() -> gestion.actualizarCapacitacion(cap));
            rechazar(() -> gestion.asignarTrabajador(t, cap));

            Capacitacion cancelada = capacitacion(curso);
            gestion.programarCapacitacion(cancelada);
            gestion.cancelarCapacitacion(cancelada.getIdCapacitacion());
            rechazar(() -> gestion.asignarTrabajador(t, cancelada));
            rechazar(() -> gestion.cerrarCapacitacion(cancelada.getIdCapacitacion()));

            int anteriores = personal.listarTrabajadores().size();
            rechazar(() -> Jdbc.transaccion(c -> {
                new TrabajadorDAO().insertar(c, trabajador("11112222"));
                throw new SQLException("Fallo controlado después del INSERT");
            }));
            comprobar(personal.listarTrabajadores().size() == anteriores, "Rollback completo");
            // Verifica las restricciones SQL sin depender de las validaciones Java.
            rechazar(() -> Jdbc.transaccion(c -> new TrabajadorDAO().insertar(c, trabajador("abc"))));
            rechazar(() -> Jdbc.transaccion(c -> Jdbc.insertar(c,
                    "INSERT INTO participacion(idTrabajador,idCapacitacion,fechaAsignacion,estado) VALUES (?,?,?,?)",
                    999999, cap.getIdCapacitacion(), LocalDate.now(), "PENDIENTE")));
            Curso sinVigencia = curso("SIN-VIGENCIA"); gestion.registrarCurso(sinVigencia);
            comprobar(gestion.listarCursos().stream().anyMatch(c -> c.getCodigo().equals("SIN-VIGENCIA") && c.getVigenciaMeses() == null), "NULL preservado");

            Capacitacion concurrente = capacitacion(curso);
            gestion.programarCapacitacion(concurrente);
            ExecutorService pool = Executors.newFixedThreadPool(2);
            CountDownLatch inicio = new CountDownLatch(1);
            Callable<Boolean> asignar = () -> {
                inicio.await();
                try {
                    new CapacitacionJdbcControlador(new PersonalJdbcControlador(admin), admin).asignarTrabajador(t, concurrente);
                    return true;
                } catch (IllegalStateException ex) {
                    return false;
                }
            };
            try {
                Future<Boolean> uno = pool.submit(asignar), dos = pool.submit(asignar);
                inicio.countDown();
                int exitos = (uno.get(20, TimeUnit.SECONDS) ? 1 : 0) + (dos.get(20, TimeUnit.SECONDS) ? 1 : 0);
                comprobar(exitos == 1, "Solo una asignación concurrente");
            } finally { pool.shutdownNow(); }
            comprobar(gestion.listarParticipaciones().size() == 2, "Sin duplicados concurrentes");
            comprobar(personal.listarTrabajadores().size() == 1, "Programar y asignar no vuelven a registrar personal");
            comprobar(gestion.listarCursos().size() == 2, "Programar no vuelve a registrar curso");
            SwingUtilities.invokeAndWait(() -> {
                comprobar(contieneFilas(new pe.utp.sigecapjv.vista.PnlPersonal(personal), 1), "Personal muestra datos JDBC");
                comprobar(contieneFilas(new pe.utp.sigecapjv.vista.PnlCapacitaciones(gestion, personal), 3), "Programaciones muestran datos JDBC");
            });
            PruebaResultadosMySql.verificar(personal, gestion, admin);
            PruebaUsuariosMySql.verificar();
            PruebaPermisosMySql.verificar();
            PruebaAccesoMySql.verificar();
            System.out.println("OK: " + comprobaciones + " comprobaciones MySQL.");
        }
    }
}
