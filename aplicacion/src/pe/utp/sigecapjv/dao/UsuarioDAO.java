package pe.utp.sigecapjv.dao;

import java.sql.*;
import java.util.*;
import pe.utp.sigecapjv.modelo.*;

public class UsuarioDAO {
    public List<Usuario> listar(Connection c) throws SQLException {
        List<Usuario> lista = new ArrayList<>();
        try (var p = c.prepareStatement("SELECT idUsuario,idRol,idTrabajador,nombreCompleto,nombreUsuario,correo,estado FROM usuario ORDER BY nombreUsuario"); var r = p.executeQuery()) {
            while (r.next()) lista.add(new Usuario(r.getInt(1),r.getInt(2),(Integer)r.getObject(3),r.getString(4),r.getString(5),r.getString(6),r.getString(7)));
        }
        return lista;
    }
    public int guardar(Connection c, Usuario u, String hash) throws SQLException {
        try (var p = c.prepareStatement("SELECT nombre FROM rol WHERE idRol=?")) {
            p.setInt(1,u.idRol());
            try (var r = p.executeQuery()) {
                if (!r.next()) throw new IllegalArgumentException("Rol inexistente.");
                if ("Trabajador".equals(r.getString(1)) && u.idTrabajador() == null)
                    throw new IllegalArgumentException("El rol Trabajador requiere vincular un trabajador.");
            }
        }
        if (u.idUsuario() == 0) return Jdbc.insertar(c,
                "INSERT INTO usuario(idRol,idTrabajador,nombreCompleto,nombreUsuario,correo,estado,contrasenaHash) VALUES (?,?,?,?,?,?,?)",
                u.idRol(),u.idTrabajador(),u.nombreCompleto(),u.nombreUsuario(),u.correo(),u.estado(),hash);
        Jdbc.actualizar(c,"UPDATE usuario SET idRol=?,idTrabajador=?,nombreCompleto=?,nombreUsuario=?,correo=?,estado=? WHERE idUsuario=?",
                u.idRol(),u.idTrabajador(),u.nombreCompleto(),u.nombreUsuario(),u.correo(),u.estado(),u.idUsuario());
        if (hash != null) Jdbc.actualizar(c,"UPDATE usuario SET contrasenaHash=? WHERE idUsuario=?",hash,u.idUsuario());
        return u.idUsuario();
    }

    /** Serializa cambios administrativos, incluida la creación inicial, en la misma conexión. */
    public void bloquear(Connection c) throws SQLException {
        try (var p = c.prepareStatement("SELECT GET_LOCK('sigecap_administracion_usuarios',10)"); var r = p.executeQuery()) {
            if (!r.next() || r.getInt(1) != 1) throw new IllegalStateException("Otra operación de usuarios está en curso. Reintente.");
        }
        // MySQL libera el bloqueo al cerrar la conexión, después del commit de Jdbc.
    }
    public boolean hayUsuarios(Connection c) throws SQLException {
        try (var p = c.prepareStatement("SELECT 1 FROM usuario LIMIT 1"); var r = p.executeQuery()) { return r.next(); }
    }
}
