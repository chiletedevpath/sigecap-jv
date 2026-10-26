package pe.utp.sigecapjv.modelo;

/**
 * Permiso del catálogo persistido; no contiene datos de una cuenta.
 */
public record Permiso(int idPermiso, String modulo, String nombre) {
    public Permiso {
        if (idPermiso <= 0 || modulo == null || modulo.isBlank() || nombre == null || nombre.isBlank())
            throw new IllegalArgumentException("Permiso inválido.");
    }
}
