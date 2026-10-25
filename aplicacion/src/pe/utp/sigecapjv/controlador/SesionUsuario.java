
package pe.utp.sigecapjv.controlador;

import pe.utp.sigecapjv.modelo.PermisoAcceso;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.Objects;
import java.util.Set;

public final class SesionUsuario implements AutoCloseable {
    private final int idUsuario;
    private final Integer idTrabajador;
    private final String nombreCompleto;
    private final String rol;
    private final Set<PermisoAcceso> permisos;
    private final String huellaClave;
    private volatile boolean activa = true;

    SesionUsuario(int idUsuario, Integer idTrabajador,
                  String nombreCompleto, String rol,
                  Set<PermisoAcceso> permisos, String hash) {
        this.idUsuario = idUsuario;
        this.idTrabajador = idTrabajador;
        this.nombreCompleto = nombreCompleto;
        this.rol = rol;
        this.permisos = Set.copyOf(permisos);
        this.huellaClave = huella(hash);
    }

    public int getIdUsuario() {
        return idUsuario;
    }

    public Integer getIdTrabajador() {
        return idTrabajador;
    }

    public String getNombreCompleto() {
        return nombreCompleto;
    }

    public String getRol() {
        return rol;
    }

    public boolean estaActiva() {
        return activa;
    }

    boolean conservaAcceso(Integer trabajador, String nombreRol,
                           Set<PermisoAcceso> actuales, String hashActual) {
        return activa
                && Objects.equals(idTrabajador, trabajador)
                && rol.equals(nombreRol)
                && permisos.equals(actuales)
                && huellaClave.equals(huella(hashActual));
    }

    private static String huella(String hash) {
        try {
            byte[] resumen = MessageDigest.getInstance("SHA-256")
                    .digest(Objects.requireNonNull(hash, "Hash requerido")
                                    .getBytes(StandardCharsets.UTF_8));

            return Base64.getEncoder().encodeToString(resumen);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 no disponible.", ex);
        }
    }

    public boolean puede(String modulo, String accion) {
        return activa && permisos.contains(new PermisoAcceso(modulo, accion));
    }

    public void exigirPermiso(String modulo, String accion) {
        if (!puede(modulo, accion)) {
            throw new SecurityException(
                    "No tiene permiso para realizar esta operación.");
        }
    }

    @Override
    public void close() {
        activa = false;
    }
}
