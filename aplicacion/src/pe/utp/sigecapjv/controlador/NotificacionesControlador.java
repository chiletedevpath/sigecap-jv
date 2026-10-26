package pe.utp.sigecapjv.controlador;

import pe.utp.sigecapjv.dao.NotificacionDAO;
import pe.utp.sigecapjv.modelo.Notificacion;
import java.util.List;

public class NotificacionesControlador {
    private NotificacionDAO dao;

    public NotificacionesControlador(NotificacionDAO dao) {
        this.dao = dao;
    }

    public NotificacionesControlador() {
        this.dao = new NotificacionDAO();
    }

    public List<Notificacion> obtenerAlertasActualizadas() {
        dao.generarAlertasVencimiento();
        return dao.listarNotificaciones();
    }

    public void marcarComoLeida(int idNotificacion) {
        dao.marcarLeida(idNotificacion);
    }
}
