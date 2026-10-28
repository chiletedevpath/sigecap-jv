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
            if (esTrabajador()) return dao.listarPorTrabajador(c, trabajadorVigente(c));
            AccesoControlador.exigir(c, sesion, "capacitaciones", "consultar");
            if (puedeGestionar()) dao.generarAlertasVencimiento(c);
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
