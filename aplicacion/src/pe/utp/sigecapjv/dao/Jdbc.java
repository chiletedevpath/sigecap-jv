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
            String mensaje = ex.getErrorCode() == 1062
                    ? "Ya existe un registro con esos datos únicos (DNI, código, asignación, usuario o correo)."
                    : ex.getSQLState() != null && ex.getSQLState().startsWith("23")
                    ? "La operación incumple una relación o restricción de la base de datos."
                    : "No se pudo completar la operación en MySQL. Revise conexión, configuración y datos.";
            throw new IllegalStateException(mensaje, ex);
        }
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
