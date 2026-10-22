package pe.utp.sigecapjv.modelo;

import java.util.Objects;

/** La pareja módulo/nombre identifica el permiso, como en la base de datos. */
public record PermisoAcceso(String modulo, String nombre) {
    public PermisoAcceso {
        Objects.requireNonNull(modulo);
        Objects.requireNonNull(nombre);
        if (modulo.isBlank() || nombre.isBlank()) throw new IllegalArgumentException("Permiso incompleto.");
    }
}
