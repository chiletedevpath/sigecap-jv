package pe.utp.sigecapjv.controlador;

import pe.utp.sigecapjv.dao.Jdbc;
import pe.utp.sigecapjv.dao.RolDAO;
import pe.utp.sigecapjv.dao.UsuarioDAO;
import pe.utp.sigecapjv.modelo.Permiso;
import pe.utp.sigecapjv.modelo.PermisoAcceso;
import pe.utp.sigecapjv.modelo.Rol;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Administra las asignaciones del catálogo, sin crear módulos todavía no implementados.
 */
public class RolControlador {
    public record Configuracion(Rol rol, List<Permiso> catalogo, Set<Integer> asignados) {
        public Configuracion {
            catalogo = List.copyOf(catalogo);
            asignados = Set.copyOf(asignados);
        }
    }

    private final SesionUsuario sesion;
    private final RolDAO dao = new RolDAO();

    public RolControlador(SesionUsuario sesion) {
        this.sesion = Objects.requireNonNull(sesion);
    }

    public List<Rol> listar() {
        AccesoControlador.exigir(sesion, "usuarios", "consultar");
        return Jdbc.transaccion(dao::listar);
    }

    public boolean puedeEditar() {
        return sesion.puede("usuarios", "gestionar");
    }

    public Configuracion consultar(int idRol) {
        AccesoControlador.exigir(sesion, "usuarios", "consultar");
        return Jdbc.transaccion(c -> new Configuracion(dao.buscar(c, idRol), dao.catalogo(c), dao.asignados(c, idRol)));
    }

    public void guardar(int idRol, Set<Integer> esperados, Set<Integer> seleccionados) {
        Objects.requireNonNull(esperados, "Falta la configuración original.");
        Objects.requireNonNull(seleccionados, "Falta la selección de permisos.");
        Set<Integer> original = Set.copyOf(esperados), elegidos = Set.copyOf(seleccionados);
        AccesoControlador.exigir(sesion, "usuarios", "gestionar");
        Jdbc.transaccion(c -> {
            // Comparte el bloqueo con cambios de usuarios para validar la autorización después de esperar.
            new UsuarioDAO().bloquear(c);
            AccesoControlador.exigir(c, sesion, "usuarios", "gestionar");
            Rol rol = dao.buscar(c, idRol);
            Set<Integer> actuales = dao.asignados(c, idRol);
            if (!actuales.equals(original)) throw new IllegalStateException(
                    "Otro administrador cambió estos permisos. Recargue el rol antes de guardar.");
            validar(rol, dao.catalogo(c), elegidos);
            dao.actualizarPermisos(c, idRol, actuales, elegidos);
            return null;
        });
    }

    private static void validar(Rol rol, List<Permiso> catalogo, Set<Integer> elegidos) {
        Set<Integer> conocidos = catalogo.stream().map(Permiso::idPermiso).collect(Collectors.toSet());
        if (!conocidos.containsAll(elegidos))
            throw new IllegalArgumentException("La selección contiene un permiso inexistente.");
        Set<PermisoAcceso> permisos = catalogo.stream().filter(p -> elegidos.contains(p.idPermiso())).map(
                p -> new PermisoAcceso(p.modulo(), p.nombre())).collect(Collectors.toSet());
        for (PermisoAcceso permiso : permisos) {
            if (permiso.nombre().equals("gestionar") && !permisos.contains(
                    new PermisoAcceso(permiso.modulo(), "consultar"))) throw new IllegalArgumentException(
                    "Para gestionar " + permiso.modulo() + " también debe permitir su consulta.");
        }
        if (permisos.contains(new PermisoAcceso("capacitaciones", "consultar")) && !permisos.contains(
                new PermisoAcceso("personal", "consultar"))) throw new IllegalArgumentException(
                "Capacitaciones necesita consultar Personal para mostrar los participantes.");
        if (rol.nombre().equals("Administrador") && (!permisos.contains(
                new PermisoAcceso("usuarios", "consultar")) || !permisos.contains(
                new PermisoAcceso("usuarios", "gestionar")))) throw new IllegalArgumentException(
                "El rol Administrador debe conservar la consulta y gestión de Usuarios.");
        if (!"Administrador".equals(rol.nombre())
                && permisos.stream().anyMatch(p -> "usuarios".equals(p.modulo()))) {
            throw new IllegalArgumentException(
                    "Solo el rol Administrador puede tener permisos de Usuarios.");
        }
        if (rol.nombre().equals("Trabajador") && permisos.stream().anyMatch(
                p -> Set.of("personal", "capacitaciones", "usuarios").contains(p.modulo())))
            throw new IllegalArgumentException(
                    "Trabajador utiliza consultas propias; no puede recibir acceso global a estos módulos.");
    }
}
