package pe.utp.sigecapjv.dao;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import pe.utp.sigecapjv.modelo.HistorialCapacitacion;
import pe.utp.sigecapjv.modelo.Trabajador;

public class TrabajadorDAO {
    public int insertar(Connection c, Trabajador dato) throws SQLException {
        return Jdbc.insertar(c, "INSERT INTO trabajador (dni,nombres,apellidos,cargo,estado) VALUES (?,?,?,?,?)",
                dato.getDni(), dato.getNombres(), dato.getApellidos(), dato.getCargo(), dato.getEstado());
    }

    public void actualizar(Connection c, Trabajador dato) throws SQLException {
        Jdbc.actualizar(c, "UPDATE trabajador SET dni=?,nombres=?,apellidos=?,cargo=?,estado=? WHERE idTrabajador=?",
                dato.getDni(), dato.getNombres(), dato.getApellidos(), dato.getCargo(), dato.getEstado(), dato.getIdTrabajador());
    }

    public Trabajador buscar(Connection c, int id, boolean bloquear) throws SQLException {
        try (PreparedStatement p = c.prepareStatement("SELECT * FROM trabajador WHERE idTrabajador=?" + (bloquear ? " FOR UPDATE" : ""))) {
            p.setInt(1, id);
            try (ResultSet r = p.executeQuery()) {
                if (!r.next()) throw new IllegalArgumentException("El registro de trabajador no existe.");
                return mapear(r);
            }
        }
    }

    public List<Trabajador> listar(Connection c) throws SQLException {
        List<Trabajador> datos = new ArrayList<>();
        try (PreparedStatement p = c.prepareStatement("SELECT * FROM trabajador ORDER BY idTrabajador");
             ResultSet r = p.executeQuery()) {
            while (r.next()) datos.add(mapear(r));
        }
        return datos;
    }

    public List<HistorialCapacitacion> listarHistorial(Connection c, int idTrabajador) throws SQLException {
        String sql = """
                SELECT cu.nombre AS curso, ca.fecha, ca.estado AS estadoCapacitacion,
                       p.estado AS estadoParticipacion, a.condicion AS asistencia,
                       r.condicion AS resultado, r.calificacion, ce.numero AS certificado,
                       v.estadoActual AS vigencia
                FROM participacion p
                INNER JOIN capacitacion ca ON ca.idCapacitacion = p.idCapacitacion
                INNER JOIN curso cu ON cu.idCurso = ca.idCurso
                LEFT JOIN asistencia a ON a.idParticipacion = p.idParticipacion
                LEFT JOIN resultado r ON r.idParticipacion = p.idParticipacion
                LEFT JOIN certificado ce ON ce.idParticipacion = p.idParticipacion
                LEFT JOIN vw_control_vigencias v ON v.idCertificado = ce.idCertificado
                WHERE p.idTrabajador = ?
                ORDER BY ca.fecha DESC, ca.idCapacitacion DESC
                """;
        List<HistorialCapacitacion> historial = new ArrayList<>();
        try (PreparedStatement p = c.prepareStatement(sql)) {
            p.setInt(1, idTrabajador);
            try (ResultSet r = p.executeQuery()) {
                while (r.next()) {
                    historial.add(new HistorialCapacitacion(
                            r.getString("curso"),
                            r.getObject("fecha", java.time.LocalDate.class),
                            r.getString("estadoCapacitacion"),
                            r.getString("estadoParticipacion"),
                            r.getString("asistencia"),
                            r.getString("resultado"),
                            r.getBigDecimal("calificacion"),
                            r.getString("certificado"),
                            r.getString("vigencia")));
                }
            }
        }
        return historial;
    }

    private Trabajador mapear(ResultSet r) throws SQLException {
        return new Trabajador(r.getInt("idTrabajador"), r.getString("dni"), r.getString("nombres"), r.getString("apellidos"), r.getString("cargo"), r.getString("estado"));
    }
}
