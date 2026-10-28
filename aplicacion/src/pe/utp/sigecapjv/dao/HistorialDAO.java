package pe.utp.sigecapjv.dao;

import pe.utp.sigecapjv.modelo.HistorialCapacitacion;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class HistorialDAO {
    public List<HistorialCapacitacion> consultar(Connection c, int trabajador) throws SQLException {
        String sql = """
                SELECT p.idParticipacion, cu.nombre, ca.fecha,
                       CASE WHEN ca.estado='CANCELADA' THEN 'CANCELADA' ELSE p.estado END AS estado,
                       a.condicion AS asistencia, r.condicion AS resultado, r.calificacion,
                       ce.numero, ce.fechaVencimiento,
                       CASE WHEN ce.idCertificado IS NULL THEN 'SIN_CERTIFICADO'
                            WHEN ce.fechaVencimiento IS NULL THEN 'NO_APLICA'
                            WHEN ce.fechaVencimiento < CURDATE() THEN 'VENCIDO'
                            WHEN DATEDIFF(ce.fechaVencimiento,CURDATE()) <= 30 THEN 'PROXIMO_A_VENCER'
                            ELSE 'VIGENTE' END AS vigencia
                FROM participacion p
                JOIN capacitacion ca ON ca.idCapacitacion=p.idCapacitacion
                JOIN curso cu ON cu.idCurso=ca.idCurso
                LEFT JOIN asistencia a ON a.idParticipacion=p.idParticipacion
                LEFT JOIN resultado r ON r.idParticipacion=p.idParticipacion
                LEFT JOIN certificado ce ON ce.idParticipacion=p.idParticipacion
                WHERE p.idTrabajador=?
                ORDER BY ca.fecha DESC, p.idParticipacion DESC
                """;
        List<HistorialCapacitacion> filas = new ArrayList<>();
        try (var consulta = c.prepareStatement(sql)) {
            consulta.setInt(1, trabajador);
            try (var r = consulta.executeQuery()) {
                while (r.next()) filas.add(new HistorialCapacitacion(
                        r.getInt("idParticipacion"), r.getString("nombre"), r.getObject("fecha", LocalDate.class),
                        r.getString("estado"), r.getString("asistencia"), r.getString("resultado"),
                        r.getBigDecimal("calificacion"), r.getString("numero"),
                        r.getObject("fechaVencimiento", LocalDate.class), r.getString("vigencia")));
            }
        }
        return List.copyOf(filas);
    }
}
