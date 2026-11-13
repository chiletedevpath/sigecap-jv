package pe.utp.sigecapjv.controlador;

import pe.utp.sigecapjv.dao.*;
import pe.utp.sigecapjv.util.Contrasenas;
import java.util.Arrays;

public final class CuentaControlador {
    private final SesionUsuario sesion;

    public CuentaControlador(SesionUsuario sesion) {
        this.sesion = sesion;
    }

    public void cambiarClave(char[] actual, char[] nueva, char[] confirmacion) {
        if (sesion == null) throw new SecurityException("Debe iniciar sesión.");
        if (actual == null || actual.length == 0) throw new IllegalArgumentException("Ingrese su contraseña actual.");
        if (nueva == null || !Arrays.equals(nueva, confirmacion))
            throw new IllegalArgumentException("La confirmación no coincide.");
        if (Arrays.equals(actual, nueva)) throw new IllegalArgumentException("Ingrese una contraseña diferente.");
        String hash = Contrasenas.crearHash(nueva);
        Jdbc.transaccion(c -> {
            new UsuarioDAO().bloquear(c);
            if (!SesionControlador.vigente(c, sesion)) throw new SecurityException("Inicie sesión nuevamente.");
            var cuenta = AccesoDAO.estado(c, sesion.getIdUsuario());
            if (!Contrasenas.verificar(actual, cuenta.hash()))
                throw new IllegalArgumentException("La contraseña actual es incorrecta.");
            new UsuarioDAO().cambiarClave(c, sesion.getIdUsuario(), cuenta.hash(), hash);
            return null;
        });
        sesion.close();
    }
}
