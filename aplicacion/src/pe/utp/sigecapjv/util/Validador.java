package pe.utp.sigecapjv.util;

public final class Validador {
    private Validador() {}

    public static String texto(String valor, String campo) {
        if (valor == null || valor.isBlank()) throw new IllegalArgumentException(campo + " es obligatorio.");
        return valor.trim();
    }

    public static void requerido(Object valor, String campo) {
        if (valor == null) throw new IllegalArgumentException(campo + " es obligatorio.");
    }

    public static void estado(String valor, String... permitidos) {
        for (String permitido : permitidos) if (permitido.equals(valor)) return;
        throw new IllegalArgumentException("Estado inválido: " + valor);
    }
}
