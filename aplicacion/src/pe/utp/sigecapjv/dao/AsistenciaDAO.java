package pe.utp.sigecapjv.dao;

import pe.utp.sigecapjv.modelo.Asistencia;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;

public class AsistenciaDAO {
    public Asistencia buscarPorParticipacion(Connection conexion, int idParticipacion) throws SQLException {
        try (PreparedStatement sentencia = conexion.prepareStatement(
                "SELECT idAsistencia,idParticipacion,condicion,fechaRegistro FROM asistencia WHERE idParticipacion=?")) {
            sentencia.setInt(1, idParticipacion);
            try (ResultSet fila = sentencia.executeQuery()) {
                if (!fila.next()) return null;
                return new Asistencia(
                        fila.getInt("idAsistencia"), fila.getInt("idParticipacion"),
                        fila.getString("condicion"), fila.getObject("fechaRegistro", LocalDateTime.class));
            }
        }
    }

    public int insertar(Connection conexion, Asistencia asistencia) throws SQLException {
        return Jdbc.insertar(
                conexion,
                "INSERT INTO asistencia (idParticipacion,condicion,fechaRegistro) VALUES (?,?,?)",
                asistencia.getIdParticipacion(), asistencia.getCondicion(), asistencia.getFechaRegistro());
    }

    public void actualizar(Connection conexion, Asistencia asistencia) throws SQLException {
        Jdbc.actualizar(
                conexion, "UPDATE asistencia SET condicion=? WHERE idAsistencia=? AND idParticipacion=?",
                asistencia.getCondicion(), asistencia.getIdAsistencia(), asistencia.getIdParticipacion());
    }
}
