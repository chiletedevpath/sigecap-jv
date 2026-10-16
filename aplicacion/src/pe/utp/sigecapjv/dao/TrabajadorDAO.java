package pe.utp.sigecapjv.dao;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
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

    private Trabajador mapear(ResultSet r) throws SQLException {
        return new Trabajador(r.getInt("idTrabajador"), r.getString("dni"), r.getString("nombres"), r.getString("apellidos"), r.getString("cargo"), r.getString("estado"));
    }
}
