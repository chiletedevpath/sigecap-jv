package pe.utp.sigecapjv.dao;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

public class InicioDAO {

    public record Resumen(int programadas, int pendientes, int proximasVencer, int vencidas) {
    }

    public record CapacitacionProgramada(String curso, LocalDate fecha, LocalTime horaInicio, LocalTime horaFin,
                                         int participantes) {
    }

    public record ProximoVencimiento(String trabajador, String curso, LocalDate vencimiento, String estado) {
    }

    public Resumen obtenerResumen(Connection conexion) throws SQLException { return obtenerResumen(conexion, null); }

    public Resumen obtenerResumen(Connection conexion, Integer trabajador) throws SQLException {

        String sql = """
                SELECT
                    (SELECT COUNT(*)
                     FROM capacitacion
                     WHERE estado = 'PROGRAMADA'
                       AND fecha >= CURDATE() AND (? IS NULL OR EXISTS (SELECT 1 FROM participacion px WHERE px.idCapacitacion=capacitacion.idCapacitacion AND px.idTrabajador=? AND px.estado<>'CANCELADA'))) AS programadas,
                    (SELECT COUNT(*)
                     FROM participacion p
                     INNER JOIN capacitacion c
                         ON c.idCapacitacion = p.idCapacitacion
                     WHERE p.estado = 'PENDIENTE'
                       AND c.estado <> 'CANCELADA' AND (? IS NULL OR p.idTrabajador=?)) AS pendientes,
                    (SELECT COUNT(*)
                     FROM vw_control_vigencias
                     WHERE diasRestantes BETWEEN 0 AND 30 AND (? IS NULL OR idTrabajador=?)) AS proximas,
                    (SELECT COUNT(*)
                     FROM vw_control_vigencias
                     WHERE estadoActual = 'VENCIDO' AND (? IS NULL OR idTrabajador=?)) AS vencidas
                """;

        try (var sentencia = preparar(conexion, sql, trabajador, 8); var resultado = sentencia.executeQuery()) {

            if (resultado.next()) {
                return new Resumen(
                        resultado.getInt("programadas"), resultado.getInt("pendientes"), resultado.getInt("proximas"),
                        resultado.getInt("vencidas"));
            }

            throw new SQLException("No se pudo obtener el resumen de capacitaciones.");
        }
    }

    public List<CapacitacionProgramada> listarProgramadas(Connection conexion) throws SQLException { return listarProgramadas(conexion, null); }

    public List<CapacitacionProgramada> listarProgramadas(Connection conexion, Integer trabajador) throws SQLException {

        String sql = """
                SELECT
                    cu.nombre AS curso,
                    c.fecha,
                    c.horaInicio,
                    c.horaFin,
                    COUNT(p.idParticipacion) AS participantes
                FROM capacitacion c
                INNER JOIN curso cu
                    ON cu.idCurso = c.idCurso
                LEFT JOIN participacion p
                    ON p.idCapacitacion = c.idCapacitacion
                    AND p.estado <> 'CANCELADA' AND (? IS NULL OR p.idTrabajador=?)
                WHERE c.estado = 'PROGRAMADA'
                    AND c.fecha >= CURDATE() AND (? IS NULL OR EXISTS (SELECT 1 FROM participacion px WHERE px.idCapacitacion=c.idCapacitacion AND px.idTrabajador=? AND px.estado<>'CANCELADA'))
                GROUP BY
                    c.idCapacitacion,
                    cu.nombre,
                    c.fecha,
                    c.horaInicio,
                    c.horaFin
                ORDER BY c.fecha, c.horaInicio, c.idCapacitacion
                LIMIT 20
                """;

        List<CapacitacionProgramada> datos = new ArrayList<>();

        try (var sentencia = preparar(conexion, sql, trabajador, 4); var resultado = sentencia.executeQuery()) {

            while (resultado.next()) {
                datos.add(new CapacitacionProgramada(
                        resultado.getString("curso"), resultado.getDate("fecha").toLocalDate(),
                        resultado.getTime("horaInicio").toLocalTime(), resultado.getTime("horaFin").toLocalTime(),
                        resultado.getInt("participantes")));
            }
        }

        return datos;
    }

    public List<ProximoVencimiento> listarProximosVencimientos(Connection conexion) throws SQLException { return listarProximosVencimientos(conexion, null); }

    public List<ProximoVencimiento> listarProximosVencimientos(Connection conexion, Integer trabajador) throws SQLException {

        String sql = """
                SELECT
                    CONCAT_WS(' ', nombres, apellidos) AS trabajador,
                    nombreCurso AS curso,
                    fechaVencimiento
                FROM vw_control_vigencias
                WHERE diasRestantes BETWEEN 0 AND 30 AND (? IS NULL OR idTrabajador=?)
                ORDER BY fechaVencimiento, idCertificado
                LIMIT 20
                """;

        List<ProximoVencimiento> datos = new ArrayList<>();

        try (var sentencia = preparar(conexion, sql, trabajador, 2); var resultado = sentencia.executeQuery()) {

            while (resultado.next()) {
                datos.add(new ProximoVencimiento(
                        resultado.getString("trabajador"), resultado.getString("curso"),
                        resultado.getDate("fechaVencimiento").toLocalDate(), "Próximo"));
            }
        }

        return datos;
    }
    private static java.sql.PreparedStatement preparar(Connection c, String sql, Integer trabajador, int cantidad) throws SQLException {
        var p=c.prepareStatement(sql);
        try { for(int i=1;i<=cantidad;i++) p.setObject(i,trabajador); return p; }
        catch(SQLException ex) { p.close(); throw ex; }
    }
}