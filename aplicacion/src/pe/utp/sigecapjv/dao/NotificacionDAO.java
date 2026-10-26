package pe.utp.sigecapjv.dao;

import pe.utp.sigecapjv.conexion.ConexionBD;
import pe.utp.sigecapjv.modelo.Notificacion;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
/**
 * Clase base creada como parte de la estructura inicial del proyecto SIGECAP J&V.
 * Su implementación se completará progresivamente según los requerimientos del sistema.
 */
public class NotificacionDAO {
        public void generarAlertasVencimiento() {
            String sql = "INSERT INTO notificacion (idTrabajador, tipo, mensaje, fechaGeneracion, estado, claveEvento) " +
                    "SELECT idTrabajador, " +
                    "       CASE WHEN diasRestantes <= 0 THEN 'VENCIDO' ELSE 'POR_VENCER' END, " +
                    "       CONCAT('La capacitacion del curso vence el ', fechaVencimiento), " +
                    "       NOW(), 'PENDIENTE', CONCAT(idTrabajador, '_', idCapacitacion) " +
                    "FROM vw_control_vigencias " +
                    "WHERE fechaVencimiento IS NOT NULL " +
                    "  AND diasRestantes <= 30 " +
                    "  AND NOT EXISTS ( " +
                    "      SELECT 1 FROM notificacion n " +
                    "      WHERE n.claveEvento = CONCAT(vw_control_vigencias.idTrabajador, '_', vw_control_vigencias.idCapacitacion) " +
                    "  );";

            try (Connection cn = ConexionBD.obtenerConexion();
                 PreparedStatement ps = cn.prepareStatement(sql)) {
                ps.executeUpdate();
            } catch (SQLException e) {
                System.err.println("--- ERROR SQL EN NOTIFICACION DAO ---");
                System.err.println("Mensaje: " + e.getMessage());
                System.err.println("Código SQL State: " + e.getSQLState());
                e.printStackTrace();
            }
        }
        // HU-010: Lista todas las notificaciones para la consulta en la interfaz
        public List<Notificacion> listarNotificaciones() {
            List<Notificacion> lista = new ArrayList<>();
            String sql = "SELECT idNotificacion, idTrabajador, tipo, mensaje, fechaGeneracion, estado FROM notificacion ORDER BY fechaGeneracion DESC";

            try (Connection cn = ConexionBD.obtenerConexion();
                 PreparedStatement ps = cn.prepareStatement(sql);
                 ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Notificacion n = new Notificacion();
                    n.setIdNotificacion(rs.getInt("idNotificacion"));
                    n.setIdTrabajador(rs.getInt("idTrabajador"));
                    n.setTipo(rs.getString("tipo"));
                    n.setMensaje(rs.getString("mensaje"));
                    n.setFechaGeneracion(rs.getString("fechaGeneracion"));
                    n.setEstado(rs.getString("estado"));
                    lista.add(n);
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
            return lista;
        }

        // Marcar notificación como leída
        public void marcarLeida(int idNotificacion) {
            String sql = "UPDATE notificacion SET estado = 'LEIDA' WHERE idNotificacion = ?";
            try (Connection cn = ConexionBD.obtenerConexion();
                 PreparedStatement ps = cn.prepareStatement(sql)) {
                ps.setInt(1, idNotificacion);
                ps.executeUpdate();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
}
