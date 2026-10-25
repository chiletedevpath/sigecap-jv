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

    public Resumen obtenerResumen(Connection conexion) throws SQLException {

        String sql = """
                SELECT
                    (SELECT COUNT(*)
                     FROM capacitacion
                     WHERE estado = 'PROGRAMADA'
                       AND fecha >= CURDATE()) AS programadas,
                    (SELECT COUNT(*)
                     FROM participacion p
                     INNER JOIN capacitacion c
                         ON c.idCapacitacion = p.idCapacitacion
                     WHERE p.estado = 'PENDIENTE'
                       AND c.estado <> 'CANCELADA') AS pendientes,
                    (SELECT COUNT(*)
                     FROM vw_control_vigencias
                     WHERE diasRestantes BETWEEN 0 AND 30) AS proximas,
                    (SELECT COUNT(*)
                     FROM vw_control_vigencias
                     WHERE estadoActual = 'VENCIDO') AS vencidas
                """;

        try (var sentencia = conexion.prepareStatement(sql); var resultado = sentencia.executeQuery()) {

            if (resultado.next()) {
                return new Resumen(
                        resultado.getInt("programadas"), resultado.getInt("pendientes"), resultado.getInt("proximas"),
                        resultado.getInt("vencidas"));
            }

            throw new SQLException("No se pudo obtener el resumen de capacitaciones.");
        }
    }

    public List<CapacitacionProgramada> listarProgramadas(Connection conexion) throws SQLException {

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
                    AND p.estado <> 'CANCELADA'
                WHERE c.estado = 'PROGRAMADA'
                    AND c.fecha >= CURDATE()
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

        try (var sentencia = conexion.prepareStatement(sql); var resultado = sentencia.executeQuery()) {

            while (resultado.next()) {
                datos.add(new CapacitacionProgramada(
                        resultado.getString("curso"), resultado.getDate("fecha").toLocalDate(),
                        resultado.getTime("horaInicio").toLocalTime(), resultado.getTime("horaFin").toLocalTime(),
                        resultado.getInt("participantes")));
            }
        }

        return datos;
    }

    public List<ProximoVencimiento> listarProximosVencimientos(Connection conexion) throws SQLException {

        String sql = """
                SELECT
                    CONCAT_WS(' ', nombres, apellidos) AS trabajador,
                    nombreCurso AS curso,
                    fechaVencimiento
                FROM vw_control_vigencias
                WHERE diasRestantes BETWEEN 0 AND 30
                ORDER BY fechaVencimiento, idCertificado
                LIMIT 20
                """;

        List<ProximoVencimiento> datos = new ArrayList<>();

        try (var sentencia = conexion.prepareStatement(sql); var resultado = sentencia.executeQuery()) {

            while (resultado.next()) {
                datos.add(new ProximoVencimiento(
                        resultado.getString("trabajador"), resultado.getString("curso"),
                        resultado.getDate("fechaVencimiento").toLocalDate(), "Próximo"));
            }
        }

        return datos;
    }
}