package pe.utp.sigecapjv.dao;

import pe.utp.sigecapjv.modelo.Curso;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class CursoDAO {
    public int insertar(Connection c, Curso dato) throws SQLException {
        return Jdbc.insertar(
                c, "INSERT INTO curso (codigo,nombre,tipo,vigenciaMeses,estado) VALUES (?,?,?,?,?)",
                dato.getCodigo(), dato.getNombre(), dato.getTipo(), dato.getVigenciaMeses(), dato.getEstado());
    }

    public void actualizar(Connection c, Curso dato) throws SQLException {
        Jdbc.actualizar(
                c, "UPDATE curso SET codigo=?,nombre=?,tipo=?,vigenciaMeses=?,estado=? WHERE idCurso=?",
                dato.getCodigo(), dato.getNombre(), dato.getTipo(), dato.getVigenciaMeses(), dato.getEstado(),
                dato.getIdCurso());
    }

    public Curso buscar(Connection c, int id, boolean bloquear) throws SQLException {
        try (PreparedStatement p = c.prepareStatement(
                "SELECT * FROM curso WHERE idCurso=?" + (bloquear ? " FOR UPDATE" : ""))) {
            p.setInt(1, id);
            try (ResultSet r = p.executeQuery()) {
                if (!r.next()) throw new IllegalArgumentException("El registro de curso no existe.");
                return mapear(r);
            }
        }
    }

    public List<Curso> listar(Connection c) throws SQLException {
        List<Curso> datos = new ArrayList<>();
        try (PreparedStatement p = c.prepareStatement("SELECT * FROM curso ORDER BY idCurso");
             ResultSet r = p.executeQuery()) {
            while (r.next()) datos.add(mapear(r));
        }
        return datos;
    }

    private Curso mapear(ResultSet r) throws SQLException {
        return new Curso(
                r.getInt("idCurso"), r.getString("codigo"), r.getString("nombre"), r.getString("tipo"),
                (Integer) r.getObject("vigenciaMeses"), r.getString("estado"));
    }
}
