package pe.utp.sigecapjv.dao;

import java.sql.*;
import java.util.*;
import pe.utp.sigecapjv.modelo.Rol;

public class RolDAO {
    public List<Rol> listar(Connection c) throws SQLException {
        List<Rol> roles = new ArrayList<>();
        try (var p = c.prepareStatement("SELECT idRol,nombre FROM rol ORDER BY idRol"); var r = p.executeQuery()) {
            while(r.next()) roles.add(new Rol(r.getInt(1),r.getString(2)));
        }
        return roles;
    }
    public List<String[]> permisos(Connection c) throws SQLException {
        List<String[]> filas = new ArrayList<>();
        try (var p = c.prepareStatement("SELECT r.nombre,p.modulo,p.nombre FROM rol r JOIN rol_permiso rp ON rp.idRol=r.idRol JOIN permiso p ON p.idPermiso=rp.idPermiso ORDER BY r.idRol,p.modulo,p.nombre"); var r = p.executeQuery()) {
            while (r.next()) filas.add(new String[]{r.getString(1),r.getString(2),r.getString(3)});
        }
        return filas;
    }
    public int prepararIniciales(Connection c) throws SQLException {
        String[] nombres = {"Administrador", "Responsable de capacitaciones", "Supervisor de Operaciones", "Trabajador"};
        for (String nombre : nombres) {
            try (var p = c.prepareStatement("INSERT INTO rol(nombre) VALUES (?) ON DUPLICATE KEY UPDATE nombre=nombre")) {
                p.setString(1,nombre); p.executeUpdate();
            }
        }
        for (String modulo : List.of("personal","capacitaciones","usuarios")) {
            for (String accion : List.of("consultar","gestionar")) {
                try (var p = c.prepareStatement("INSERT INTO permiso(modulo,nombre) VALUES (?,?) ON DUPLICATE KEY UPDATE nombre=nombre")) {
                    Jdbc.parametros(p,modulo,accion); p.executeUpdate();
                }
                for (String rol : nombres) {
                    boolean asignar = rol.equals(nombres[0]) || !modulo.equals("usuarios") &&
                            (rol.equals(nombres[1]) || rol.equals(nombres[2]) && accion.equals("consultar"));
                    if (asignar) try (var p = c.prepareStatement("INSERT IGNORE INTO rol_permiso(idRol,idPermiso) SELECT r.idRol,p.idPermiso FROM rol r CROSS JOIN permiso p WHERE r.nombre=? AND p.modulo=? AND p.nombre=?")) {
                        Jdbc.parametros(p,rol,modulo,accion); p.executeUpdate();
                    }
                }
            }
        }
        return listar(c).stream().filter(r -> r.nombre().equals("Administrador")).findFirst().orElseThrow().idRol();
    }
}
