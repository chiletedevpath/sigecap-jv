package pe.utp.sigecapjv.modelo;

public record Usuario(int idUsuario, int idRol, Integer idTrabajador, String nombreCompleto, String nombreUsuario,
                      String correo, String estado) {
    public Usuario {
        nombreCompleto = texto(nombreCompleto, 160, "Nombre");
        nombreUsuario = texto(nombreUsuario, 60, "Usuario");
        correo = texto(correo, 160, "Correo");
        if (!correo.matches("[^\\s@]+@[^\\s@]+\\.[^\\s@]+")) throw new IllegalArgumentException("Correo inválido.");
        if (idUsuario < 0 || idRol <= 0 || (idTrabajador != null && idTrabajador <= 0))
            throw new IllegalArgumentException("Identificador inválido.");
        if (!"ACTIVO".equals(estado) && !"INACTIVO".equals(estado))
            throw new IllegalArgumentException("Estado inválido.");
    }

    private static String texto(String valor, int max, String campo) {
        if (valor == null || valor.isBlank() || valor.strip().length() > max)
            throw new IllegalArgumentException(campo + " es obligatorio (máximo " + max + " caracteres).");
        return valor.strip();
    }
}
