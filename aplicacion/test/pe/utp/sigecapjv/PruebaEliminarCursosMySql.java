package pe.utp.sigecapjv;

import pe.utp.sigecapjv.conexion.ConexionBD;
import pe.utp.sigecapjv.controlador.*;
import pe.utp.sigecapjv.dao.*;
import pe.utp.sigecapjv.modelo.*;
import java.sql.*;
import java.util.*;
import java.util.concurrent.*;
import static pe.utp.sigecapjv.PruebaEliminarCursos.*;

public final class PruebaEliminarCursosMySql {
    public static void verificar() throws Exception {
        try (var admin = new LoginControlador().autenticar("admin_prueba", "Clave-usuarios-prueba-2026".toCharArray())) {
            var personal = new PersonalJdbcControlador(admin);
            var gestion = new CapacitacionJdbcControlador(personal, admin);
            Curso libre = curso("DELETE-LIBRE"); gestion.registrarCurso(libre);
            gestion.eliminarCurso(libre.getIdCurso());
            comprobar(gestion.listarCursos().stream().noneMatch(c->c.getIdCurso()==libre.getIdCurso()), "Elimina curso en MySQL");
            rechazar(() -> gestion.eliminarCurso(libre.getIdCurso()), IllegalArgumentException.class);
            rechazar(() -> gestion.eliminarCurso(-1), IllegalArgumentException.class);
            rechazar(() -> new CapacitacionJdbcControlador(personal).eliminarCurso(1), SecurityException.class);
            for (String estado : List.of("PROGRAMADA", "CERRADA", "CANCELADA")) {
                Curso usado = curso("DELETE-" + estado); gestion.registrarCurso(usado);
                Capacitacion cap = capacitacion(usado); gestion.programarCapacitacion(cap);
                Jdbc.transaccion(c -> { Jdbc.actualizar(c,"UPDATE capacitacion SET estado=? WHERE idCapacitacion=?",estado,cap.getIdCapacitacion()); return null; });
                var antes = historial();
                rechazar(() -> gestion.eliminarCurso(usado.getIdCurso()), IllegalStateException.class);
                comprobar(antes.equals(historial()), "El rechazo conserva todo el historial: " + estado);
                usado.desactivar(); gestion.actualizarCurso(usado);
            }
            int conHistorial = Jdbc.transaccion(c -> {
                try (var p=c.prepareStatement("SELECT ca.idCurso FROM capacitacion ca JOIN participacion p ON p.idCapacitacion=ca.idCapacitacion JOIN certificado ce ON ce.idParticipacion=p.idParticipacion LIMIT 1"); var r=p.executeQuery()) {
                    if (!r.next()) throw new AssertionError("Falta certificado para probar la conservación del historial");
                    return r.getInt(1);
                }
            });
            var antes = historial();
            rechazar(() -> gestion.eliminarCurso(conHistorial), IllegalStateException.class);
            comprobar(antes.equals(historial()), "Conserva participaciones, asistencia, resultados, certificados y avisos");
            Curso protegido = curso("DELETE-PERMISOS"); gestion.registrarCurso(protegido);
            for (String usuario : List.of("supervisor_prueba", "trabajador_prueba")) {
                String clave = usuario.startsWith("supervisor") ? "Clave-renovada-prueba-2026" : "Clave-usuarios-prueba-2026";
                try (var sesion = new LoginControlador().autenticar(usuario,clave.toCharArray())) {
                    var restringido = new CapacitacionJdbcControlador(new PersonalJdbcControlador(sesion),sesion);
                    rechazar(() -> restringido.eliminarCurso(protegido.getIdCurso()), SecurityException.class);
                }
            }
            try (var responsable = new LoginControlador().autenticar("responsable_prueba","Clave-usuarios-prueba-2026".toCharArray())) {
                var autorizado = new CapacitacionJdbcControlador(new PersonalJdbcControlador(responsable),responsable);
                autorizado.eliminarCurso(protegido.getIdCurso());
                responsable.close();
                rechazar(() -> autorizado.eliminarCurso(conHistorial), SecurityException.class);
            }
            verificarVista(gestion, personal);
            concurrencia(gestion, false);
            concurrencia(gestion, true);
            permisosMySql(gestion);
            System.out.println("OK: eliminación MySQL, permisos, historial, cancelación Swing y ambas carreras con programación.");
        }
    }

    private static List<String> historial() {
        return Jdbc.transaccion(c -> {
            List<String> datos = new ArrayList<>();
            for (String tabla : List.of("capacitacion","participacion","asistencia","resultado","certificado","notificacion")) {
                try (var p=c.prepareStatement("SELECT * FROM " + tabla + " ORDER BY 1"); var r=p.executeQuery()) {
                    while (r.next()) for (int i=1;i<=r.getMetaData().getColumnCount();i++)
                        datos.add(tabla + ":" + i + ":" + Objects.toString(r.getObject(i)));
                }
            }
            return datos;
        });
    }

    private static void concurrencia(CapacitacionJdbcControlador gestion, boolean programarPrimero) throws Exception {
        Curso curso = curso("DELETE-CARRERA-" + programarPrimero); gestion.registrarCurso(curso);
        ExecutorService ejecutor = Executors.newSingleThreadExecutor();
        try (Connection c = ConexionBD.obtenerConexion()) {
            c.setAutoCommit(false);
            try {
                new CursoDAO().buscar(c,curso.getIdCurso(),true);
                if (programarPrimero) {
                    Capacitacion cap = capacitacion(curso); cap.programar();
                    new CapacitacionDAO().insertar(c,cap);
                }
                CountDownLatch iniciada = new CountDownLatch(1);
                Future<Boolean> otra = ejecutor.submit(() -> {
                    iniciada.countDown();
                    try {
                        if (programarPrimero) gestion.eliminarCurso(curso.getIdCurso());
                        else gestion.programarCapacitacion(capacitacion(curso));
                        return true;
                    } catch (IllegalArgumentException | IllegalStateException esperado) {
                        if (programarPrimero && !esperado.getMessage().contains("historial")) throw esperado;
                        if (!programarPrimero && !esperado.getMessage().contains("no existe")) throw esperado;
                        return false;
                    }
                });
                comprobar(iniciada.await(5,TimeUnit.SECONDS), "Comienza la operación concurrente");
                try { otra.get(300,TimeUnit.MILLISECONDS); throw new AssertionError("Se omitió el bloqueo del curso"); }
                catch (TimeoutException esperado) { }
                if (!programarPrimero) new CursoDAO().eliminar(c,curso.getIdCurso());
                c.commit();
                comprobar(!otra.get(20,TimeUnit.SECONDS), "La operación que perdió la carrera se rechaza sin inconsistencias");
            } finally { c.rollback(); }
        } finally { ejecutor.shutdownNow(); }
    }

    private static void permisosMySql(CapacitacionJdbcControlador gestion) throws Exception {
        Curso curso = curso("DELETE-CUENTA-APP"); gestion.registrarCurso(curso);
        try (Connection c=DriverManager.getConnection(System.getenv("SIGECAP_DB_URL"),"sigecap_app",System.getenv("SIGECAP_DB_PASSWORD"))) {
            c.setAutoCommit(false);
            new CursoDAO().eliminar(c,curso.getIdCurso()); c.commit();
            try (var p=c.prepareStatement("DELETE FROM trabajador WHERE idTrabajador=-1")) {
                try { p.executeUpdate(); throw new AssertionError("La cuenta app no debe eliminar trabajadores"); }
                catch (SQLException esperado) { comprobar(esperado.getErrorCode()==1142,"DELETE limitado a curso y rol_permiso"); }
            } finally { c.rollback(); }
        }
    }
}
