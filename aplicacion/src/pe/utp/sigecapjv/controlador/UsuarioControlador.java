package pe.utp.sigecapjv.controlador;

import java.util.List;
import pe.utp.sigecapjv.dao.*;
import pe.utp.sigecapjv.modelo.*;
import pe.utp.sigecapjv.util.Contrasenas;

public class UsuarioControlador {
    private final SesionUsuario sesion;
    private final UsuarioDAO usuarios = new UsuarioDAO();
    private final RolDAO roles = new RolDAO();
    public UsuarioControlador(SesionUsuario sesion) { this.sesion = java.util.Objects.requireNonNull(sesion); }
    public RolControlador controladorRoles() { return new RolControlador(sesion); }
    public boolean puedeGestionar() { return sesion.puede("usuarios", "gestionar"); }
    public String rolActual() { return sesion.getRol(); }
    public List<Usuario> listar() {
        AccesoControlador.exigir(sesion,"usuarios","consultar");
        return Jdbc.transaccion(usuarios::listar);
    }
    public List<Rol> roles() {
        AccesoControlador.exigir(sesion,"usuarios","consultar");
        return Jdbc.transaccion(roles::listar);
    }
    public List<String[]> permisos() {
        AccesoControlador.exigir(sesion,"usuarios","consultar");
        return Jdbc.transaccion(roles::permisos);
    }
    public int guardar(Usuario usuario, char[] clave) {
        AccesoControlador.exigir(sesion,"usuarios","gestionar");
        String hash = clave == null || clave.length == 0 ? null : Contrasenas.crearHash(clave);
        if (usuario.idUsuario() == 0 && hash == null) throw new IllegalArgumentException("Ingrese una contraseña para el nuevo usuario.");
        return Jdbc.transaccion(c -> {
            usuarios.bloquear(c);
            AccesoControlador.exigir(c, sesion, "usuarios", "gestionar");
            List<Usuario> actuales = usuarios.listar(c);
            Usuario anterior = actuales.stream().filter(u -> u.idUsuario()==usuario.idUsuario()).findFirst().orElse(null);
            if (usuario.idUsuario() > 0 && anterior == null) throw new IllegalArgumentException("Usuario inexistente.");
            int administrador = roles.listar(c).stream().filter(r -> r.nombre().equals("Administrador")).mapToInt(Rol::idRol).findFirst().orElse(-1);
            if (anterior != null && anterior.idRol()==administrador && anterior.estado().equals("ACTIVO")
                    && (usuario.idRol()!=administrador || !usuario.estado().equals("ACTIVO"))
                    && actuales.stream().filter(u -> u.idRol()==administrador && u.estado().equals("ACTIVO")).count() <= 1)
                throw new IllegalArgumentException("Debe conservar al menos un administrador activo.");
            return usuarios.guardar(c,usuario,hash);
        });
    }
    public static boolean requiereAdministradorInicial() { return Jdbc.transaccion(c -> !new UsuarioDAO().hayUsuarios(c)); }
    public static void crearAdministradorInicial(String nombre, String usuario, String correo, char[] clave) {
        String hash = Contrasenas.crearHash(clave);
        Jdbc.transaccion(c -> {
            UsuarioDAO dao = new UsuarioDAO(); dao.bloquear(c);
            if (dao.hayUsuarios(c)) throw new IllegalStateException("Ya existen usuarios. Solicite acceso al administrador.");
            int rol = new RolDAO().prepararIniciales(c);
            dao.guardar(c,new Usuario(0,rol,null,nombre,usuario,correo,"ACTIVO"),hash);
            return null;
        });
    }
}
