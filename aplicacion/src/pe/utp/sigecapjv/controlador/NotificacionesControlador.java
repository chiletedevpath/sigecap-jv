package pe.utp.sigecapjv.controlador;

import pe.utp.sigecapjv.dao.Jdbc;
import pe.utp.sigecapjv.dao.NotificacionDAO;
import pe.utp.sigecapjv.modelo.Notificacion;
import java.util.List;

public class NotificacionesControlador {
    private final SesionUsuario sesion;
    private final NotificacionDAO dao = new NotificacionDAO();
    public NotificacionesControlador() { this(null); }
    public NotificacionesControlador(SesionUsuario sesion) { this.sesion = sesion; }
    public boolean puedeGestionar() { return sesion != null && sesion.puede("capacitaciones", "gestionar"); }

    public record Plazos(int vencimiento, int recordatorio) {
        public Plazos {
            if (vencimiento < 0 || vencimiento > 365 || recordatorio < 0 || recordatorio > 365)
                throw new IllegalArgumentException("Ingrese un plazo entre 0 y 365 días.");
        }
    }

    public boolean puedeConfigurar() {
        return sesion != null && "Administrador".equals(sesion.getRol()) && sesion.puede("usuarios", "gestionar");
    }

    public Plazos consultarPlazos() {
        return Jdbc.transaccion(c -> {
            AccesoControlador.exigir(c, sesion, "usuarios", "gestionar");
            try (var consulta = c.prepareStatement("SELECT diasVencimiento,diasRecordatorio FROM configuracion_alertas WHERE id=1");
                 var fila = consulta.executeQuery()) {
                if (!fila.next()) throw new IllegalStateException("Falta la configuración de alertas en MySQL.");
                return new Plazos(fila.getInt(1), fila.getInt(2));
            }
        });
    }

    public void guardarPlazos(Plazos anterior, Plazos nuevo) {
        Jdbc.transaccion(c -> {
            AccesoControlador.exigir(c, sesion, "usuarios", "gestionar");
            try (var cambio = c.prepareStatement("""
                    UPDATE configuracion_alertas SET diasVencimiento=?, diasRecordatorio=?
                    WHERE id=1 AND diasVencimiento=? AND diasRecordatorio=?
                    """)) {
                cambio.setInt(1, nuevo.vencimiento());
                cambio.setInt(2, nuevo.recordatorio());
                cambio.setInt(3, anterior.vencimiento());
                cambio.setInt(4, anterior.recordatorio());
                if (cambio.executeUpdate()!=1) throw new IllegalStateException("Los plazos cambiaron. Vuelva a abrir la configuración.");
            }
            return null;
        });
    }

    public boolean esTrabajador() { return sesion != null && "Trabajador".equals(sesion.getRol()); }
    public boolean puedeMarcarLeida() { return esTrabajador() || puedeGestionar(); }
    private int trabajadorVigente(java.sql.Connection c) throws java.sql.SQLException {
        if (!SesionControlador.vigente(c, sesion) || sesion.getIdTrabajador() == null)
            throw new SecurityException("Debe iniciar sesión con una cuenta de trabajador vinculada.");
        return sesion.getIdTrabajador();
    }

    public List<Notificacion> obtenerAlertasActualizadas() {
        if (sesion == null) throw new SecurityException("Debe iniciar sesión.");
        return Jdbc.transaccion(c -> {
            c.setTransactionIsolation(java.sql.Connection.TRANSACTION_READ_COMMITTED);
            if (esTrabajador()) {
                int trabajador = trabajadorVigente(c);
                dao.generarRecordatorios(c, trabajador);
                return dao.listarPorTrabajador(c, trabajador);
            }
            AccesoControlador.exigir(c, sesion, "capacitaciones", "consultar");
            if (puedeGestionar()) {
                dao.generarRecordatorios(c, null);
                dao.generarAlertasVencimiento(c);
            }
            return dao.listarNotificaciones(c);
        });
    }

    public void marcarComoLeida(int id) {
        if (sesion == null) throw new SecurityException("Debe iniciar sesión.");
        if (id <= 0) throw new IllegalArgumentException("Seleccione una notificación.");
        Jdbc.transaccion(c -> {
            if (esTrabajador()) {
                dao.marcarPropiaLeida(c, id, trabajadorVigente(c));
                return null;
            }
            AccesoControlador.exigir(c, sesion, "capacitaciones", "gestionar");
            dao.marcarLeida(c, id);
            return null;
        });
    }
}
