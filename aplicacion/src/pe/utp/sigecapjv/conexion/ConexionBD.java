package pe.utp.sigecapjv.conexion;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public final class ConexionBD {
    private ConexionBD() {
    }

    public static Connection obtenerConexion() throws SQLException {
        String usuario = System.getenv("SIGECAP_DB_USER");
        String clave = System.getenv("SIGECAP_DB_PASSWORD");
        if (usuario == null || usuario.isBlank() || clave == null || clave.isBlank()) {
            throw new SQLException("Configure SIGECAP_DB_USER y SIGECAP_DB_PASSWORD.", "08001");
        }
        String url = System.getenv("SIGECAP_DB_URL");
        if (url == null || url.isBlank()) url = "jdbc:mysql://localhost:3306/sigecap_jv";
        java.util.Properties opciones = new java.util.Properties();
        opciones.setProperty("user", usuario);
        opciones.setProperty("password", clave);
        opciones.setProperty("connectTimeout", "5000");
        opciones.setProperty("socketTimeout", "10000");
        return DriverManager.getConnection(url, opciones);
    }
}
