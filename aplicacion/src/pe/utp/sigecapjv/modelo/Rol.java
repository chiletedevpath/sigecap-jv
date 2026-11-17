package pe.utp.sigecapjv.modelo;

public record Rol(int idRol, String nombre) {
    @Override public String toString() { return nombre; }
}
