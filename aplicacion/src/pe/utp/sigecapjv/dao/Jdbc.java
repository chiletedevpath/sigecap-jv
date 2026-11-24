package pe.utp.sigecapjv.dao;

import pe.utp.sigecapjv.conexion.ConexionBD;

import java.sql.*;

/**
 * Cada operación utiliza su propia conexión y confirma todos sus cambios juntos.
 */
public final class Jdbc {
    private Jdbc() {
    }

    @FunctionalInterface
    public interface Operacion<T> {
        T ejecutar(Connection conexion) throws SQLException;
    }

    public static <T> T transaccion(Operacion<T> operacion) {
        try (Connection conexion = ConexionBD.obtenerConexion()) {
            conexion.setAutoCommit(false);
            try {
                T resultado = operacion.ejecutar(conexion);
                conexion.commit();
                return resultado;
            } catch (SQLException | RuntimeException ex) {
                try {
                    conexion.rollback();
                } catch (SQLException rollback) {
                    ex.addSuppressed(rollback);
                }
                throw ex;
            }
        } catch (SQLException ex) {
            // No mostrar SQL, credenciales ni detalles internos en los diálogos.
            throw new IllegalStateException(mensajeError(ex), ex);
        }
    }

    public static String mensajeError(Throwable error) {
        for (Throwable causa = error; causa != null; causa = causa.getCause()) {
            if (causa instanceof SecurityException) return "Su sesión o sus permisos cambiaron. Inicie sesión nuevamente.";
            if (!(causa instanceof SQLException sql)) continue;
            int codigo = sql.getErrorCode();
            String estado = sql.getSQLState();
            if (codigo == 1146 || codigo == 1054 || codigo == 1049 || "S0022".equals(estado) || "42S22".equals(estado))
                return "La base necesita actualizar su estructura. Solicite aplicar el esquema o las migraciones de database/README.md.";
            if (codigo == 1045 || codigo == 1044 || codigo == 1142 || codigo == 1143)
                return "La cuenta de conexión MySQL no tiene acceso. Revise sus credenciales y permisos con el administrador.";
            if (codigo == 1062)
                return "Ya existe un registro con esos datos únicos (DNI, código, asignación, usuario o correo).";
            if (estado != null && estado.startsWith("08"))
                return "No se pudo conectar con MySQL. Verifique que el servicio esté iniciado y revise la configuración de conexión.";
            if (estado != null && estado.startsWith("23"))
                return "La operación incumple una relación o restricción de la base de datos.";
        }
        return "No se pudo completar la operación. Vuelva a consultar; si el problema continúa, contacte al administrador.";
    }

    public static int insertar(Connection c, String sql, Object... valores) throws SQLException {
        try (PreparedStatement p = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            parametros(p, valores);
            p.executeUpdate();
            try (ResultSet r = p.getGeneratedKeys()) {
                if (!r.next()) throw new SQLException("No se recibió el identificador generado.");
                return r.getInt(1);
            }
        }
    }

    public static void actualizar(Connection c, String sql, Object... valores) throws SQLException {
        try (PreparedStatement p = c.prepareStatement(sql)) {
            parametros(p, valores);
            if (p.executeUpdate() != 1) throw new IllegalArgumentException("El registro no existe.");
        }
    }

    public static void parametros(PreparedStatement p, Object... valores) throws SQLException {
        for (int i = 0; i < valores.length; i++) p.setObject(i + 1, valores[i]);
    }
}
