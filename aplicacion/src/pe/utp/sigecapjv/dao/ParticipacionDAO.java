package pe.utp.sigecapjv.dao;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import pe.utp.sigecapjv.modelo.*;

public class ParticipacionDAO {
    public int insertar(Connection c, Participacion dato) throws SQLException {
        return Jdbc.insertar(c, "INSERT INTO participacion (idTrabajador,idCapacitacion,fechaAsignacion,estado) VALUES (?,?,?,?)",
                dato.getTrabajador().getIdTrabajador(), dato.getCapacitacion().getIdCapacitacion(),
                dato.getFechaAsignacion(), dato.getEstado());
    }

    public List<Participacion> listar(Connection c) throws SQLException {
        List<Participacion> datos = new ArrayList<>();
        try (PreparedStatement p = c.prepareStatement("SELECT * FROM participacion ORDER BY idParticipacion");
             ResultSet r = p.executeQuery()) {
            while (r.next()) datos.add(new Participacion(r.getInt("idParticipacion"),
                    r.getObject("fechaAsignacion", LocalDate.class), r.getString("estado"),
                    new TrabajadorDAO().buscar(c, r.getInt("idTrabajador"), false),
                    new CapacitacionDAO().buscar(c, r.getInt("idCapacitacion"), false)));
        }
        return datos;
    }
}
