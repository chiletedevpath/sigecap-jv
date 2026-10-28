package pe.utp.sigecapjv.dao;

import pe.utp.sigecapjv.modelo.Reporte;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class ReporteDAO {
    // Cancelar una capacitación no modifica sus participaciones; el reporte la muestra cancelada.
    private static final String ESTADO = "CASE WHEN ca.estado='CANCELADA' THEN 'CANCELADA' ELSE p.estado END";

    public List<Reporte.Fila> consultar(Connection c, Reporte filtros) throws SQLException {
        StringBuilder sql = new StringBuilder(
                "SELECT p.idParticipacion,p.idTrabajador,t.nombres,t.apellidos,cu.nombre AS curso,ca.fecha,"
                        + ESTADO + " AS estado,r.condicion,ce.idCertificado,ce.numero,ce.fechaEmision,"
                        + "ce.fechaVencimiento, CASE WHEN ce.idCertificado IS NULL THEN 'SIN_CERTIFICADO' "
                        + "WHEN v.diasRestantes BETWEEN 0 AND 30 THEN 'PROXIMO_A_VENCER' "
                        + "ELSE v.estadoActual END AS vigencia FROM participacion p "
                        + "JOIN trabajador t ON t.idTrabajador=p.idTrabajador "
                        + "JOIN capacitacion ca ON ca.idCapacitacion=p.idCapacitacion "
                        + "JOIN curso cu ON cu.idCurso=ca.idCurso "
                        + "LEFT JOIN resultado r ON r.idParticipacion=p.idParticipacion "
                        + "LEFT JOIN certificado ce ON ce.idParticipacion=p.idParticipacion "
                        + "LEFT JOIN vw_control_vigencias v ON v.idCertificado=ce.idCertificado WHERE 1=1");
        List<Object> valores = new ArrayList<>();
        if (filtros.getDesde() != null) {
            sql.append(" AND ca.fecha>=?");
            valores.add(filtros.getDesde());
        }
        if (filtros.getHasta() != null) {
            sql.append(" AND ca.fecha<=?");
            valores.add(filtros.getHasta());
        }
        if (filtros.getCurso() != null) {
            sql.append(" AND cu.idCurso=?");
            valores.add(filtros.getCurso().getIdCurso());
        }
        if (filtros.getTrabajador() != null) {
            sql.append(" AND t.idTrabajador=?");
            valores.add(filtros.getTrabajador().getIdTrabajador());
        }
        if (filtros.getEstado() != null) {
            sql.append(" AND ").append(ESTADO).append("=?");
            valores.add(filtros.getEstado());
        }
        sql.append(" ORDER BY ca.fecha,t.apellidos,t.nombres,cu.nombre");
        List<Reporte.Fila> filas = new ArrayList<>();
        try (PreparedStatement p = c.prepareStatement(sql.toString())) {
            Jdbc.parametros(p, valores.toArray());
            try (ResultSet r = p.executeQuery()) {
                while (r.next()) filas.add(mapear(r));
            }
        }
        return filas;
    }

    private Reporte.Fila mapear(ResultSet r) throws SQLException {
        String vigencia = r.getString("vigencia");
        String resultado = r.getString("condicion");
        return new Reporte.Fila(
                r.getString("nombres") + " " + r.getString("apellidos"), r.getString("curso"),
                r.getObject("fecha", LocalDate.class), r.getString("estado"), resultado == null ? "-" : resultado,
                vigencia);
    }
}
