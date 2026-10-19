package pe.utp.sigecapjv.controlador;

import pe.utp.sigecapjv.dao.AsistenciaDAO;
import pe.utp.sigecapjv.dao.Jdbc;
import pe.utp.sigecapjv.modelo.Asistencia;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;

public class SeguimientoControlador {
    private final AsistenciaDAO asistencias = new AsistenciaDAO();

    public Asistencia consultarAsistencia(int idParticipacion) {
        if (idParticipacion <= 0) throw new IllegalArgumentException("Participación inválida.");
        return Jdbc.transaccion(conexion -> asistencias.buscarPorParticipacion(conexion, idParticipacion));
    }

    public Asistencia registrarAsistencia(int idParticipacion, String condicion) {
        return Jdbc.transaccion(conexion -> {
            validarParticipacion(conexion, idParticipacion);
            if (asistencias.buscarPorParticipacion(conexion, idParticipacion) != null) {
                throw new IllegalStateException("La participación ya tiene asistencia. Utilice actualizar.");
            }
            Asistencia asistencia = new Asistencia(0, idParticipacion, condicion, null);
            asistencia.registrar();
            asistencia.setIdAsistencia(asistencias.insertar(conexion, asistencia));
            return asistencia;
        });
    }

    public Asistencia actualizarAsistencia(int idParticipacion, String condicion) {
        return Jdbc.transaccion(conexion -> {
            validarParticipacion(conexion, idParticipacion);
            Asistencia asistencia = asistencias.buscarPorParticipacion(conexion, idParticipacion);
            if (asistencia == null) throw new IllegalArgumentException("La participación aún no tiene asistencia.");
            asistencia.setCondicion(condicion);
            asistencia.actualizar();
            asistencias.actualizar(conexion, asistencia);
            return asistencia;
        });
    }

    private void validarParticipacion(Connection conexion, int idParticipacion) throws SQLException {
        // El bloqueo serializa registros concurrentes y cambios de estado de la capacitación.
        try (PreparedStatement sentencia = conexion.prepareStatement(
                "SELECT p.estado AS estadoParticipacion,c.estado AS estadoCapacitacion,c.fecha "
                        + "FROM participacion p JOIN capacitacion c ON c.idCapacitacion=p.idCapacitacion "
                        + "WHERE p.idParticipacion=? FOR UPDATE")) {
            sentencia.setInt(1, idParticipacion);
            try (ResultSet fila = sentencia.executeQuery()) {
                if (!fila.next()) throw new IllegalArgumentException("La participación no existe.");
                if ("CANCELADA".equals(fila.getString("estadoParticipacion"))
                        || !"PROGRAMADA".equals(fila.getString("estadoCapacitacion"))) {
                    throw new IllegalStateException(
                            "Solo se registra asistencia en participaciones vigentes de capacitaciones programadas.");
                }
                if (fila.getObject("fecha", LocalDate.class).isAfter(LocalDate.now())) {
                    throw new IllegalStateException(
                            "No se puede registrar asistencia antes de la fecha de capacitación.");
                }
            }
        }
    }
}
