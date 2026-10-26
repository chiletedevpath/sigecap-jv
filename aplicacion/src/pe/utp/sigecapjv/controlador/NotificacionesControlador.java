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

    public List<Notificacion> obtenerAlertasActualizadas() {
        if (sesion == null) throw new SecurityException("Debe iniciar sesión.");
        return Jdbc.transaccion(c -> {
            c.setTransactionIsolation(java.sql.Connection.TRANSACTION_READ_COMMITTED);
            AccesoControlador.exigir(c, sesion, "capacitaciones", "consultar");
            if (puedeGestionar()) dao.generarAlertasVencimiento(c);
            return dao.listarNotificaciones(c);
        });
    }

    public void marcarComoLeida(int id) {
        if (sesion == null) throw new SecurityException("Debe iniciar sesión.");
        if (id <= 0) throw new IllegalArgumentException("Seleccione una notificación.");
        Jdbc.transaccion(c -> {
            AccesoControlador.exigir(c, sesion, "capacitaciones", "gestionar");
            dao.marcarLeida(c, id);
            return null;
        });
    }
}
