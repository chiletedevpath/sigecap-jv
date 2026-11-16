package pe.utp.sigecapjv;

import java.sql.*;
import java.time.*;
import java.util.concurrent.*;
import java.math.BigDecimal;
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
        catch (IllegalArgumentException | IllegalStateException ex) { comprobaciones++; return; }
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

    private static <T extends Component> T encontrar(Component componente, Class<T> tipo) {
        if (tipo.isInstance(componente)) return tipo.cast(componente);
        if (!(componente instanceof Container contenedor)) return null;
        for (Component hijo : contenedor.getComponents()) {
            if (tipo.isInstance(hijo)) return tipo.cast(hijo);
            if (hijo instanceof Container) {
                T encontrado = encontrar(hijo, tipo);
                if (encontrado != null) return encontrado;
            }
        }
        return null;
    }

    private static JButton encontrarBoton(Container contenedor, String texto) {
        for (Component componente : contenedor.getComponents()) {
            if (componente instanceof JButton boton && texto.equals(boton.getText())) return boton;
            if (componente instanceof Container hijo && !(componente instanceof JComboBox<?>)) {
                JButton encontrado = encontrarBoton(hijo, texto);
                if (encontrado != null) return encontrado;
            }
        }
        return null;
    }

    public static void main(String[] args) throws Exception {
        if (!"true".equals(System.getenv("SIGECAP_DB_TEST"))) {
            throw new IllegalStateException("Solo ejecutar en una base aislada con SIGECAP_DB_TEST=true.");
        }
        PersonalJdbcControlador personal = new PersonalJdbcControlador();
        CapacitacionJdbcControlador gestion = new CapacitacionJdbcControlador(personal);
        if (args.length > 0 && "sin-conexion".equals(args[0])) {
            rechazar(personal::listarTrabajadores);
            System.out.println("OK: fallo de conexión propagado, sin retorno silencioso a memoria.");
            return;
        }
        // Una segunda JVM debe recuperar lo registrado por la primera.
        if (!personal.listarTrabajadores().isEmpty()) {
            Trabajador persistido = personal.listarTrabajadores().stream()
                    .filter(t -> "87654321".equals(t.getDni())).findFirst().orElseThrow();
            comprobar(persistido.getIdTrabajador() > 0, "Persistencia del trabajador entre procesos");
            comprobar(gestion.listarParticipaciones().size() == 2, "Persistencia de asignaciones entre procesos");
            comprobar(gestion.listarCapacitaciones().stream().anyMatch(c -> "CERRADA".equals(c.getEstado())), "Persistencia del cierre");
            comprobar(personal.consultarHistorial(persistido.getIdTrabajador()).stream()
                    .anyMatch(h -> "CERT-PRUEBA".equals(h.certificado())),
                    "Persistencia del historial y certificado entre procesos");
            System.out.println("OK: " + comprobaciones + " comprobaciones entre procesos.");
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
        comprobar(new PersonalJdbcControlador().buscarTrabajador(t.getIdTrabajador()).getNombres().equals("Nombre editado"), "Actualización persistida");

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
        var historialInicial = personal.consultarHistorial(t.getIdTrabajador());
        comprobar(historialInicial.size() == 1, "Historial incluye capacitaciones sin asistencia registrada");
        comprobar(historialInicial.get(0).asistencia() == null && historialInicial.get(0).resultado() == null
                        && historialInicial.get(0).certificado() == null,
                "Historial conserva datos opcionales ausentes");
        Jdbc.transaccion(c -> {
            Jdbc.insertar(c, "INSERT INTO asistencia (idParticipacion,condicion) VALUES (?,?)",
                    p.getIdParticipacion(), "ASISTIÓ");
            Jdbc.insertar(c, "INSERT INTO resultado (idParticipacion,condicion,calificacion) VALUES (?,?,?)",
                    p.getIdParticipacion(), "APROBADO", new BigDecimal("18.50"));
            Jdbc.insertar(c, "INSERT INTO certificado "
                            + "(idParticipacion,idTrabajador,numero,fechaEmision,fechaVencimiento) VALUES (?,?,?,?,?)",
                    p.getIdParticipacion(), t.getIdTrabajador(), "CERT-PRUEBA", LocalDate.now(),
                    LocalDate.now().plusDays(365));
            return null;
        });
        var historial = personal.consultarHistorial(t.getIdTrabajador());
        comprobar(historial.size() == 1, "Historial filtrado por trabajador");
        HistorialCapacitacion registro = historial.get(0);
        comprobar("Curso de prueba".equals(registro.curso()) && cap.getFecha().equals(registro.fecha()),
                "Curso y fecha del historial");
        comprobar("PROGRAMADA".equals(registro.estadoCapacitacion())
                        && "PENDIENTE".equals(registro.estadoParticipacion()),
                "Estados del historial");
        comprobar("ASISTIÓ".equals(registro.asistencia()) && "APROBADO".equals(registro.resultado())
                        && new BigDecimal("18.50").compareTo(registro.calificacion()) == 0,
                "Asistencia y resultado del historial");
        comprobar("CERT-PRUEBA".equals(registro.certificado()) && "VIGENTE".equals(registro.vigencia()),
                "Certificado y vigencia del historial");
        rechazar(() -> personal.consultarHistorial(999999));
        rechazar(() -> gestion.asignarTrabajador(t, cap));
        comprobar(gestion.listarCapacitaciones().get(0).getParticipaciones().size() == 1, "Composición reconstruida");
        gestion.cerrarCapacitacion(cap.getIdCapacitacion());
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
                new CapacitacionJdbcControlador(new PersonalJdbcControlador()).asignarTrabajador(t, concurrente);
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
        comprobar(personal.consultarHistorial(t.getIdTrabajador()).size() == 2,
                "Consulta JDBC recupera el historial completo del trabajador");
        comprobar(personal.listarTrabajadores().size() == 1, "Programar y asignar no vuelven a registrar personal");
        comprobar(gestion.listarCursos().size() == 2, "Programar no vuelve a registrar curso");
        SwingUtilities.invokeAndWait(() -> {
            comprobar(contieneFilas(new pe.utp.sigecapjv.vista.PnlPersonal(personal), 1), "Personal muestra datos JDBC");
            comprobar(contieneFilas(new pe.utp.sigecapjv.vista.PnlCapacitaciones(gestion, personal), 3), "Programaciones muestran datos JDBC");
            pe.utp.sigecapjv.vista.PnlPersonal panel = new pe.utp.sigecapjv.vista.PnlPersonal(personal);
            JTabbedPane pestañas = encontrar(panel, JTabbedPane.class);
            pestañas.setSelectedIndex(2);
            JButton consultar = encontrarBoton((Container) pestañas.getComponentAt(2), "Consultar");
            comprobar(consultar != null, "Botón de consulta del historial disponible");
            consultar.doClick();
            int filasHistorial = encontrar(pestañas.getComponentAt(2), JTable.class).getRowCount();
            comprobar(filasHistorial == 2,
                    "Consulta visual del historial desde la pestaña; filas encontradas: " + filasHistorial);
        });
        System.out.println("OK: " + comprobaciones + " comprobaciones MySQL.");
    }
}
