package pe.utp.sigecapjv.dao;

import java.sql.*;
import java.time.LocalDateTime;
import pe.utp.sigecapjv.modelo.Resultado;

public class ResultadoDAO {
    public Resultado buscarPorParticipacion(Connection c, int id) throws SQLException {
        try (PreparedStatement s = c.prepareStatement("SELECT * FROM resultado WHERE idParticipacion=?")) {
            s.setInt(1, id);
            try (ResultSet r = s.executeQuery()) {
                if (!r.next()) return null;
                return new Resultado(r.getInt("idResultado"), id, r.getString("condicion"),
                        r.getBigDecimal("calificacion"), r.getObject("fechaRegistro", LocalDateTime.class));
            }
        }
    }

    public int insertar(Connection c, Resultado r) throws SQLException {
        return Jdbc.insertar(c, "INSERT INTO resultado(idParticipacion,condicion,calificacion,fechaRegistro) VALUES (?,?,?,?)",
                r.getIdParticipacion(), r.getCondicion(), r.getCalificacion(), r.getFechaRegistro());
    }

    public void actualizar(Connection c, Resultado r) throws SQLException {
        Jdbc.actualizar(c, "UPDATE resultado SET condicion=?,calificacion=? WHERE idResultado=? AND idParticipacion=?",
                r.getCondicion(), r.getCalificacion(), r.getIdResultado(), r.getIdParticipacion());
    }
}
