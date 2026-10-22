package pe.utp.sigecapjv.dao;

import java.sql.*;

public final class AccesoDAO {
    private AccesoDAO() { }
    public static boolean permite(Connection c, int usuario, String modulo, String accion) throws SQLException {
        try (PreparedStatement p = c.prepareStatement("SELECT 1 FROM usuario u JOIN rol_permiso rp ON rp.idRol=u.idRol "
                + "JOIN permiso p ON p.idPermiso=rp.idPermiso WHERE u.idUsuario=? AND u.estado='ACTIVO' AND p.modulo=? AND p.nombre=?")) {
            Jdbc.parametros(p, usuario, modulo, accion);
            try (ResultSet r = p.executeQuery()) { return r.next(); }
        }
    }
}
