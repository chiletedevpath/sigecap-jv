
package pe.utp.sigecapjv.dao;

import pe.utp.sigecapjv.modelo.PermisoAcceso;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Set;

public final class AccesoDAO {
    private AccesoDAO() {
    }

    public record EstadoCuenta(Integer trabajador, String rol, boolean activa,
                               Set<PermisoAcceso> permisos, String hash) {
        public EstadoCuenta {
            permisos = Set.copyOf(permisos);
        }
    }

    public static EstadoCuenta estado(Connection c, int idUsuario) throws SQLException {
        String sql = "SELECT u.idTrabajador, u.estado, u.contrasenaHash, " +
                "r.nombre AS rol, p.modulo, p.nombre AS permiso " +
                "FROM usuario u " +
                "JOIN rol r ON r.idRol=u.idRol " +
                "LEFT JOIN rol_permiso rp ON rp.idRol=r.idRol " +
                "LEFT JOIN permiso p ON p.idPermiso=rp.idPermiso " +
                "WHERE u.idUsuario=?";

        try (var p = c.prepareStatement(sql)) {
            p.setInt(1, idUsuario);

            try (var r = p.executeQuery()) {
                if (!r.next()) return null;

                Integer trabajador = (Integer) r.getObject("idTrabajador");
                String rol = r.getString("rol");
                boolean activa = "ACTIVO".equals(r.getString("estado"));
                String hash = r.getString("contrasenaHash");

                Set<PermisoAcceso> permisos = new HashSet<>();

                do {
                    if (r.getString("permiso") != null) {
                        permisos.add(new PermisoAcceso(
                                r.getString("modulo"),
                                r.getString("permiso")));
                    }
                } while (r.next());

                return new EstadoCuenta(trabajador, rol, activa, permisos, hash);
            }
        }
    }
}
