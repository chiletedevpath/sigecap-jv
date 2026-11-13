package pe.utp.sigecapjv;

import pe.utp.sigecapjv.controlador.*;

public final class PruebaCuentaMySql {
    private static void rechazar(Runnable accion) {
        try { accion.run(); }
        catch (SecurityException | IllegalArgumentException ex) { return; }
        throw new AssertionError("Se aceptó una operación inválida de contraseña");
    }

    public static void verificar() {
        for (String usuario : new String[]{"admin_prueba","responsable_prueba","supervisor_prueba","trabajador_prueba"}) {
            char[] anterior = (usuario.equals("supervisor_prueba") ? "Clave-renovada-prueba-2026" : "Clave-usuarios-prueba-2026").toCharArray();
            char[] nueva = "Clave-personal-prueba-2026".toCharArray();
            LoginControlador login = new LoginControlador();
            try (var sesion = login.autenticar(usuario, anterior);
                 var paralela = login.autenticar(usuario, anterior)) {
                var control = new CuentaControlador(sesion);
                rechazar(() -> control.cambiarClave("incorrecta".toCharArray(), nueva, nueva));
                rechazar(() -> control.cambiarClave(anterior, nueva, anterior));
                rechazar(() -> control.cambiarClave(anterior, anterior, anterior));
                if (!new SesionControlador().vigente(sesion)) throw new AssertionError("Intento fallido cerró la sesión");
                control.cambiarClave(anterior, nueva, nueva);
                if (sesion.estaActiva() || new SesionControlador().vigente(paralela))
                    throw new AssertionError("La clave anterior conserva una sesión");
                rechazar(() -> login.autenticar(usuario, anterior));
                rechazar(() -> new CuentaControlador(paralela).cambiarClave(nueva, anterior, anterior));
                try (var renovada = login.autenticar(usuario, nueva)) {
                    new CuentaControlador(renovada).cambiarClave(nueva, anterior, anterior);
                }
            } finally {
                java.util.Arrays.fill(anterior,'\0');
                java.util.Arrays.fill(nueva,'\0');
            }
        }
        System.out.println("OK: cambio de contraseña propia y revocación de sesiones en los cuatro perfiles.");
    }
}
