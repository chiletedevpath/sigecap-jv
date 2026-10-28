package pe.utp.sigecapjv.dao;

import pe.utp.sigecapjv.modelo.Notificacion;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class NotificacionDAO {
    public void generarAlertasVencimiento(Connection c) throws SQLException {
        // Un evento por certificado, fecha y etapa. La clave única también protege consultas concurrentes.
        String sql = "INSERT INTO notificacion (idTrabajador,tipo,mensaje,estado,claveEvento) "
                + "SELECT idTrabajador, CASE WHEN diasRestantes<0 THEN 'VENCIDO' ELSE 'POR_VENCER' END, "
                + "CONCAT(nombreCurso, ' - certificado ', numeroCertificado, ' - vence el ', fechaVencimiento), "
                + "'PENDIENTE', CONCAT('VENCIMIENTO:',idCertificado,':',fechaVencimiento,':',"
                + "CASE WHEN diasRestantes<0 THEN 'VENCIDO' ELSE 'POR_VENCER' END) "
                + "FROM vw_control_vigencias WHERE fechaVencimiento IS NOT NULL AND diasRestantes<=30 "
                + "ORDER BY idCertificado ON DUPLICATE KEY UPDATE idNotificacion=notificacion.idNotificacion";
        try (PreparedStatement p = c.prepareStatement(sql)) { p.executeUpdate(); }
    }

    public List<Notificacion> listarNotificaciones(Connection c) throws SQLException {
        return listar(c, null);
    }

    public List<Notificacion> listarPorTrabajador(Connection c, int trabajador) throws SQLException {
        return listar(c, trabajador);
    }

    private List<Notificacion> listar(Connection c, Integer trabajador) throws SQLException {
        List<Notificacion> lista = new ArrayList<>();
        try (PreparedStatement p = c.prepareStatement("SELECT idNotificacion,idTrabajador,tipo,mensaje,fechaGeneracion,estado,claveEvento FROM notificacion"
                + (trabajador == null ? "" : " WHERE idTrabajador=?")
                + " ORDER BY fechaGeneracion DESC,idNotificacion DESC")) {
            if (trabajador != null) p.setInt(1, trabajador);
            try (ResultSet r = p.executeQuery()) {
                while (r.next()) {
                    Notificacion n = new Notificacion();
                    n.setIdNotificacion(r.getInt("idNotificacion"));
                    n.setIdTrabajador(r.getInt("idTrabajador"));
                    n.setTipo(r.getString("tipo"));
                    n.setMensaje(r.getString("mensaje"));
                    n.setFechaGeneracion(r.getString("fechaGeneracion"));
                    n.setEstado(r.getString("estado"));
                    n.setClaveEvento(r.getString("claveEvento"));
                    lista.add(n);
                }
            }
        }
        return lista;
    }

    public void marcarPropiaLeida(Connection c, int id, int trabajador) throws SQLException {
        Jdbc.actualizar(c, "UPDATE notificacion SET estado='LEIDA' WHERE idNotificacion=? AND idTrabajador=?", id, trabajador);
    }

    public void marcarLeida(Connection c, int id) throws SQLException {
        Jdbc.actualizar(c, "UPDATE notificacion SET estado='LEIDA' WHERE idNotificacion=?", id);
    }
}
