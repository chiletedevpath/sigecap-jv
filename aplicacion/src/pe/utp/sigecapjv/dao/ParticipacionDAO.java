package pe.utp.sigecapjv.dao;

import pe.utp.sigecapjv.modelo.Participacion;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class ParticipacionDAO {
    public int insertar(Connection c, Participacion dato) throws SQLException {
        return Jdbc.insertar(
                c, "INSERT INTO participacion (idTrabajador,idCapacitacion,fechaAsignacion,estado) VALUES (?,?,?,?)",
                dato.getTrabajador().getIdTrabajador(), dato.getCapacitacion().getIdCapacitacion(),
                dato.getFechaAsignacion(), dato.getEstado());
    }

    public List<Participacion> listar(Connection c) throws SQLException {
        List<Participacion> datos = new ArrayList<>();
        try (PreparedStatement p = c.prepareStatement(
                "SELECT * FROM participacion ORDER BY idParticipacion"); ResultSet r = p.executeQuery()) {
            while (r.next()) {
                datos.add(mapear(c, r));
            }
        }
        return datos;
    }

    public Participacion buscar(Connection c, int idParticipacion, boolean bloquear) throws SQLException {

        // Al registrar seguimiento también se protege la fecha y el estado de la capacitación.
        String sql = "SELECT p.* FROM participacion p "
                + "JOIN capacitacion c ON c.idCapacitacion=p.idCapacitacion "
                + "WHERE p.idParticipacion=?" + (bloquear ? " FOR UPDATE" : "");

        try (PreparedStatement p = c.prepareStatement(sql)) {
            p.setInt(1, idParticipacion);

            try (ResultSet r = p.executeQuery()) {

                if (!r.next()) {
                    throw new IllegalArgumentException("La participación no existe.");
                }

                return mapear(c, r);
            }
        }
    }

    public void actualizarEstado(Connection c, int idParticipacion, String estado) throws SQLException {

        Jdbc.actualizar(
                c, "UPDATE participacion " + "SET estado=? " + "WHERE idParticipacion=?", estado,
                idParticipacion);
    }

    private Participacion mapear(Connection c, ResultSet r) throws SQLException {

        return new Participacion(
                r.getInt("idParticipacion"), r.getObject("fechaAsignacion", LocalDate.class), r.getString("estado"),
                new TrabajadorDAO().buscar(c, r.getInt("idTrabajador"), false),
                new CapacitacionDAO().buscar(c, r.getInt("idCapacitacion"), false));
    }
}
