
package pe.utp.sigecapjv.dao;

import pe.utp.sigecapjv.modelo.Trabajador;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class TrabajadorDAO {

    public int insertar(Connection c, Trabajador dato) throws SQLException {
        return Jdbc.insertar(
                c,
                "INSERT INTO trabajador (dni,nombres,apellidos,cargo,estado) VALUES (?,?,?,?,?)",
                dato.getDni(),
                dato.getNombres(),
                dato.getApellidos(),
                dato.getCargo(),
                dato.getEstado()
        );
    }

    public void actualizar(Connection c, Trabajador dato) throws SQLException {
        Jdbc.actualizar(
                c,
                "UPDATE trabajador SET dni=?,nombres=?,apellidos=?,cargo=?,estado=? WHERE idTrabajador=?",
                dato.getDni(),
                dato.getNombres(),
                dato.getApellidos(),
                dato.getCargo(),
                dato.getEstado(),
                dato.getIdTrabajador()
        );
    }

    public Trabajador buscar(Connection c, int id, boolean bloquear)
            throws SQLException {

        String sql = "SELECT * FROM trabajador WHERE idTrabajador=?"
                + (bloquear ? " FOR UPDATE" : "");

        try (PreparedStatement p = c.prepareStatement(sql)) {
            p.setInt(1, id);

            try (ResultSet r = p.executeQuery()) {
                if (!r.next()) {
                    throw new IllegalArgumentException(
                            "El registro de trabajador no existe.");
                }

                return mapear(r);
            }
        }
    }

    public List<Trabajador> listar(Connection c) throws SQLException {
        List<Trabajador> datos = new ArrayList<>();

        String sql = "SELECT * FROM trabajador ORDER BY idTrabajador";

        try (PreparedStatement p = c.prepareStatement(sql);
             ResultSet r = p.executeQuery()) {

            while (r.next()) {
                datos.add(mapear(r));
            }
        }

        return datos;
    }

    /**
     * Obtiene trabajadores disponibles para vincular a una cuenta.
     *
     * Al crear un usuario, idUsuario = 0:
     * - Solo muestra trabajadores activos sin cuenta.
     *
     * Al editar un usuario:
     * - Muestra trabajadores activos sin otra cuenta.
     * - Incluye al trabajador ya vinculado al usuario actual,
     *   incluso si se encuentra inactivo.
     * - Excluye trabajadores vinculados a otras cuentas.
     */
    public List<Trabajador> disponiblesParaUsuario(
            Connection c, int idUsuario) throws SQLException {

        if (idUsuario < 0) {
            throw new IllegalArgumentException(
                    "El identificador del usuario no es válido.");
        }

        List<Trabajador> datos = new ArrayList<>();

        String sql = """
                SELECT t.*
                FROM trabajador t
                WHERE (
                    t.estado = 'ACTIVO'
                    OR EXISTS (
                        SELECT 1
                        FROM usuario propia
                        WHERE propia.idUsuario = ?
                          AND propia.idTrabajador = t.idTrabajador
                    )
                )
                AND NOT EXISTS (
                    SELECT 1
                    FROM usuario otra
                    WHERE otra.idTrabajador = t.idTrabajador
                      AND otra.idUsuario <> ?
                )
                ORDER BY t.apellidos, t.nombres, t.dni
                """;

        try (PreparedStatement p = c.prepareStatement(sql)) {
            p.setInt(1, idUsuario);
            p.setInt(2, idUsuario);

            try (ResultSet r = p.executeQuery()) {
                while (r.next()) {
                    datos.add(mapear(r));
                }
            }
        }

        return datos;
    }

    private Trabajador mapear(ResultSet r) throws SQLException {
        return new Trabajador(
                r.getInt("idTrabajador"),
                r.getString("dni"),
                r.getString("nombres"),
                r.getString("apellidos"),
                r.getString("cargo"),
                r.getString("estado")
        );
    }
}
