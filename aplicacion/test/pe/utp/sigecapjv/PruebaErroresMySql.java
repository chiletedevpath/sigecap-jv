package pe.utp.sigecapjv;

import pe.utp.sigecapjv.dao.Jdbc;
import java.sql.SQLException;

public final class PruebaErroresMySql {
    public static void verificar() {
        int[] codigos = {1146, 1054, 1049, 1045, 1142, 1062, 0, 1452, 0};
        String[] estados = {"42S02", "42S22", "42000", "28000", "42000", "23000", "08001", "23000", "S0022"};
        String[] fragmentos = {"estructura", "estructura", "estructura", "cuenta", "cuenta", "Ya existe", "conectar", "restricción", "estructura"};
        for (int i = 0; i < codigos.length; i++) {
            var sql = new SQLException("dato-interno-que-no-debe-mostrarse", estados[i], codigos[i]);
            String mensaje = Jdbc.mensajeError(new java.util.concurrent.ExecutionException(sql));
            if (!mensaje.contains(fragmentos[i]) || mensaje.contains("dato-interno"))
                throw new AssertionError("Diagnóstico incorrecto para " + codigos[i]);
        }
        if (!Jdbc.mensajeError(new SecurityException("detalle privado")).contains("sesión"))
            throw new AssertionError("Sesión inválida sin orientación");
        System.out.println("OK: 10 comprobaciones de mensajes MySQL sin detalles internos.");
    }
}
