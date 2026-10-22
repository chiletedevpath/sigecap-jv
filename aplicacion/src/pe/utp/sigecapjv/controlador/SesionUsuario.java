package pe.utp.sigecapjv.controlador;

import java.util.Set;
import pe.utp.sigecapjv.modelo.PermisoAcceso;

/** Sesión local creada únicamente después de autenticar; no contiene contraseñas. */
public final class SesionUsuario implements AutoCloseable {
    private final int idUsuario;
    private final Integer idTrabajador;
    private final String nombreCompleto;
    private final String rol;
    private final Set<PermisoAcceso> permisos;
    private volatile boolean activa = true;

    SesionUsuario(int idUsuario, Integer idTrabajador, String nombreCompleto,
                  String rol, Set<PermisoAcceso> permisos) {
        this.idUsuario = idUsuario;
        this.idTrabajador = idTrabajador;
        this.nombreCompleto = nombreCompleto;
        this.rol = rol;
        this.permisos = Set.copyOf(permisos);
    }
    public int getIdUsuario() { return idUsuario; }
    public Integer getIdTrabajador() { return idTrabajador; }
    public String getNombreCompleto() { return nombreCompleto; }
    public String getRol() { return rol; }
    public boolean estaActiva() { return activa; }
    public boolean puede(String modulo, String accion) {
        return activa && permisos.contains(new PermisoAcceso(modulo, accion));
    }
    public void exigirPermiso(String modulo, String accion) {
        if (!puede(modulo, accion)) throw new SecurityException("No tiene permiso para realizar esta operación.");
    }
    @Override public void close() { activa = false; }
}
