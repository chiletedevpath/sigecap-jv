package pe.utp.sigecapjv.dao;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.HashSet;
import java.util.Set;
import pe.utp.sigecapjv.modelo.PermisoAcceso;

/** Lee la cuenta y sus permisos en una misma transacción. */
public class AutenticacionDAO {
    public static final class Cuenta {
        public final int id;
        public final Integer trabajador;
        public final String nombre, rol, hash;
        public final boolean activa;
        public final Set<PermisoAcceso> permisos;
        public Cuenta(int id, Integer trabajador, String nombre, String rol, String hash,
                      boolean activa, Set<PermisoAcceso> permisos) {
            this.id = id; this.trabajador = trabajador; this.nombre = nombre;
            this.rol = rol; this.hash = hash; this.activa = activa;
            this.permisos = Set.copyOf(permisos);
        }
    }

    public Cuenta buscar(String usuario) {
        return Jdbc.transaccion(c -> {
            String sql = "SELECT u.idUsuario,u.idTrabajador,u.nombreCompleto,u.contrasenaHash,"
                    + "u.estado,r.nombre AS rol,p.modulo,p.nombre AS permiso FROM usuario u "
                    + "JOIN rol r ON r.idRol=u.idRol "
                    + "LEFT JOIN rol_permiso rp ON rp.idRol=r.idRol "
                    + "LEFT JOIN permiso p ON p.idPermiso=rp.idPermiso WHERE u.nombreUsuario=?";
            try (PreparedStatement s = c.prepareStatement(sql)) {
                s.setString(1, usuario);
                try (ResultSet r = s.executeQuery()) {
                    if (!r.next()) return null;
                    int id = r.getInt("idUsuario");
                    Integer trabajador = (Integer) r.getObject("idTrabajador");
                    String nombre = r.getString("nombreCompleto"), rol = r.getString("rol");
                    String hash = r.getString("contrasenaHash");
                    boolean activa = "ACTIVO".equals(r.getString("estado"));
                    Set<PermisoAcceso> permisos = new HashSet<>();
                    do {
                        if (r.getString("permiso") != null)
                            permisos.add(new PermisoAcceso(r.getString("modulo"), r.getString("permiso")));
                    } while (r.next());
                    return new Cuenta(id, trabajador, nombre, rol, hash, activa, permisos);
                }
            }
        });
    }
}
