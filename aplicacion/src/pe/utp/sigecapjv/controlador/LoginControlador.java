package pe.utp.sigecapjv.controlador;

import pe.utp.sigecapjv.dao.AutenticacionDAO;
import pe.utp.sigecapjv.util.Contrasenas;

import java.util.Objects;

public class LoginControlador {
    private final AutenticacionDAO cuentas;
    // Obliga a ejecutar el cálculo también cuando el usuario no existe.
    private static final String HASH_AUSENTE = "pbkdf2-sha256$600000$AAAAAAAAAAAAAAAAAAAAAA" +
            "==$AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=";

    public LoginControlador() {
        this(new AutenticacionDAO());
    }

    public LoginControlador(AutenticacionDAO cuentas) {
        this.cuentas = Objects.requireNonNull(cuentas);
    }

    /**
     * El llamador debe borrar su char[] después del uso.
     */
    public SesionUsuario autenticar(String usuario, char[] clave) {
        if (usuario == null || usuario.isBlank() || usuario.strip().length() > 60
                || clave == null || clave.length == 0 || clave.length > 128) throw accesoInvalido();
        AutenticacionDAO.Cuenta cuenta = cuentas.buscar(usuario.strip());
        boolean coincide = Contrasenas.verificar(clave, cuenta == null ? HASH_AUSENTE : cuenta.hash);
        if (cuenta == null || !coincide) throw accesoInvalido();
        if (!cuenta.activa) throw new SecurityException("Su cuenta está inactiva. Solicite su activación al administrador.");
        return new SesionUsuario(cuenta.id, cuenta.trabajador, cuenta.nombre, cuenta.rol, cuenta.permisos);
    }

    private SecurityException accesoInvalido() {
        return new SecurityException("Nombre de usuario o contraseña incorrectos.");
    }
}
