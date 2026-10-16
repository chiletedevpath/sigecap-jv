package pe.utp.sigecapjv.dao;

import java.sql.*;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import pe.utp.sigecapjv.modelo.*;

public class CapacitacionDAO {
    public int insertar(Connection c, Capacitacion dato) throws SQLException {
        return Jdbc.insertar(c, "INSERT INTO capacitacion (idCurso,fecha,horaInicio,horaFin,modalidad,estado) VALUES (?,?,?,?,?,?)",
                dato.getCurso().getIdCurso(), dato.getFecha(), dato.getHoraInicio(), dato.getHoraFin(), dato.getModalidad(), dato.getEstado());
    }

    public void actualizar(Connection c, Capacitacion dato) throws SQLException {
        Jdbc.actualizar(c, "UPDATE capacitacion SET idCurso=?,fecha=?,horaInicio=?,horaFin=?,modalidad=?,estado=? WHERE idCapacitacion=?",
                dato.getCurso().getIdCurso(), dato.getFecha(), dato.getHoraInicio(), dato.getHoraFin(), dato.getModalidad(), dato.getEstado(), dato.getIdCapacitacion());
    }

    public Capacitacion buscar(Connection c, int id, boolean bloquear) throws SQLException {
        try (PreparedStatement p = c.prepareStatement("SELECT * FROM capacitacion WHERE idCapacitacion=?" + (bloquear ? " FOR UPDATE" : ""))) {
            p.setInt(1, id);
            try (ResultSet r = p.executeQuery()) {
                if (!r.next()) throw new IllegalArgumentException("La capacitación no existe.");
                return mapear(c, r);
            }
        }
    }

    public List<Capacitacion> listar(Connection c) throws SQLException {
        List<Capacitacion> datos = new ArrayList<>();
        try (PreparedStatement p = c.prepareStatement("SELECT * FROM capacitacion ORDER BY idCapacitacion");
             ResultSet r = p.executeQuery()) {
            while (r.next()) datos.add(mapear(c, r));
        }
        return datos;
    }

    private Capacitacion mapear(Connection c, ResultSet r) throws SQLException {
        return new Capacitacion(r.getInt("idCapacitacion"), r.getObject("fecha", LocalDate.class),
                r.getObject("horaInicio", LocalTime.class), r.getObject("horaFin", LocalTime.class),
                r.getString("modalidad"), r.getString("estado"),
                new CursoDAO().buscar(c, r.getInt("idCurso"), false));
    }
}
