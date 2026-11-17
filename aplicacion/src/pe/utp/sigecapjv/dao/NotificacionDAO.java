package pe.utp.sigecapjv.dao;

import pe.utp.sigecapjv.modelo.Notificacion;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class NotificacionDAO {
    public void avisarAsignacion(Connection c, int participacion) throws SQLException {
        String sql = """
                INSERT INTO notificacion (idTrabajador,tipo,mensaje,estado,claveEvento)
                SELECT p.idTrabajador,'ASIGNACION',CONCAT('Capacitación asignada: ',cu.nombre,' - ',ca.fecha,' ',ca.horaInicio),
                       'PENDIENTE',CONCAT('ASIGNACION:',p.idParticipacion)
                FROM participacion p JOIN capacitacion ca USING (idCapacitacion) JOIN curso cu USING (idCurso)
                WHERE p.idParticipacion=?
                """;
        try (PreparedStatement sentencia = c.prepareStatement(sql)) {
            sentencia.setInt(1, participacion);
            sentencia.executeUpdate();
        }
    }

    public void avisarCambioSesion(Connection c, int capacitacion, boolean cancelada) throws SQLException {
        String tipo = cancelada ? "CANCELACION" : "REPROGRAMACION";
        String detalle = cancelada ? "Sesión cancelada: " : "Programación actualizada: ";
        String sql = """
                INSERT INTO notificacion (idTrabajador,tipo,mensaje,estado,claveEvento)
                SELECT p.idTrabajador,?,CONCAT(?,cu.nombre,' - ',ca.fecha,' de ',ca.horaInicio,
                       ' a ',ca.horaFin,' (',ca.modalidad,')'),'PENDIENTE',CONCAT(?,':',p.idParticipacion)
                FROM participacion p JOIN capacitacion ca USING (idCapacitacion) JOIN curso cu USING (idCurso)
                WHERE p.idCapacitacion=? AND p.estado='PENDIENTE'
                ORDER BY p.idParticipacion
                """;
        try (PreparedStatement aviso = c.prepareStatement(sql)) {
            aviso.setString(1, tipo);
            aviso.setString(2, detalle);
            aviso.setString(3, tipo + ":" + java.util.UUID.randomUUID());
            aviso.setInt(4, capacitacion);
            aviso.executeUpdate();
        }
    }

    public void generarRecordatorios(Connection c, Integer trabajador) throws SQLException {
        String sql = """
                INSERT INTO notificacion (idTrabajador,tipo,mensaje,estado,claveEvento)
                SELECT p.idTrabajador,'RECORDATORIO',CONCAT(cu.nombre,' empieza el ',ca.fecha,' a las ',ca.horaInicio),
                       'PENDIENTE',CONCAT('RECORDATORIO:',p.idParticipacion,':',ca.fecha,':',ca.horaInicio)
                FROM participacion p JOIN capacitacion ca USING (idCapacitacion) JOIN curso cu USING (idCurso)
                WHERE p.estado='PENDIENTE' AND ca.estado='PROGRAMADA'
                  AND TIMESTAMP(ca.fecha,ca.horaInicio)>=CURRENT_TIMESTAMP
                  AND ca.fecha<=ADDDATE(CURRENT_DATE,(SELECT diasRecordatorio FROM configuracion_alertas WHERE id=1))
                """ + (trabajador == null ? "" : " AND p.idTrabajador=?")
                + " ORDER BY p.idParticipacion ON DUPLICATE KEY UPDATE idNotificacion=notificacion.idNotificacion";
        try (PreparedStatement sentencia = c.prepareStatement(sql)) {
            if (trabajador != null) sentencia.setInt(1, trabajador);
            sentencia.executeUpdate();
        }
    }

    public void generarAlertasVencimiento(Connection c) throws SQLException {
        // Un evento por certificado, fecha y etapa. La clave única también protege consultas concurrentes.
        String sql = "INSERT INTO notificacion (idTrabajador,tipo,mensaje,estado,claveEvento) "
                + "SELECT idTrabajador, CASE WHEN diasRestantes<0 THEN 'VENCIDO' ELSE 'POR_VENCER' END, "
                + "CONCAT(nombreCurso, ' - certificado ', numeroCertificado, ' - vence el ', fechaVencimiento), "
                + "'PENDIENTE', CONCAT('VENCIMIENTO:',idCertificado,':',fechaVencimiento,':',"
                + "CASE WHEN diasRestantes<0 THEN 'VENCIDO' ELSE 'POR_VENCER' END) "
                + "FROM vw_control_vigencias WHERE fechaVencimiento IS NOT NULL AND diasRestantes<=(SELECT diasVencimiento FROM configuracion_alertas WHERE id=1) "
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
