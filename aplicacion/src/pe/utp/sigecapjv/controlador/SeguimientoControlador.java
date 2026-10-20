package pe.utp.sigecapjv.controlador;

import pe.utp.sigecapjv.dao.AsistenciaDAO;
import pe.utp.sigecapjv.dao.Jdbc;
import pe.utp.sigecapjv.modelo.Asistencia;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.math.BigDecimal;
import pe.utp.sigecapjv.modelo.Resultado;
import pe.utp.sigecapjv.modelo.Certificado;
import pe.utp.sigecapjv.dao.ResultadoDAO;
import pe.utp.sigecapjv.dao.CertificadoDAO;

public class SeguimientoControlador {
    private final AsistenciaDAO asistencias = new AsistenciaDAO();
    private final ResultadoDAO resultados = new ResultadoDAO();
    private final CertificadoDAO certificados = new CertificadoDAO();

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
            if (!"ASISTIO".equals(asistencia.getCondicion()) && resultados.buscarPorParticipacion(conexion, idParticipacion) != null) {
                throw new IllegalStateException("La asistencia tiene un resultado registrado y debe mantenerse como ASISTIO.");
            }
            asistencias.actualizar(conexion, asistencia);
            return asistencia;
        });
    }

    private void validarParticipacion(Connection conexion, int idParticipacion) throws SQLException {
        validarParticipacion(conexion, idParticipacion, false);
    }

    private record DatosParticipacion(int trabajador, LocalDate fecha) {}

    private DatosParticipacion validarParticipacion(Connection conexion, int idParticipacion, boolean admiteCerrada) throws SQLException {
        // El bloqueo serializa registros concurrentes y cambios de estado de la capacitación.
        try (PreparedStatement sentencia = conexion.prepareStatement(
                "SELECT p.idTrabajador,p.estado AS estadoParticipacion,c.estado AS estadoCapacitacion,c.fecha "
                        + "FROM participacion p JOIN capacitacion c ON c.idCapacitacion=p.idCapacitacion "
                        + "WHERE p.idParticipacion=? FOR UPDATE")) {
            sentencia.setInt(1, idParticipacion);
            try (ResultSet fila = sentencia.executeQuery()) {
                if (!fila.next()) throw new IllegalArgumentException("La participación no existe.");
                if ("CANCELADA".equals(fila.getString("estadoParticipacion"))
                        || !("PROGRAMADA".equals(fila.getString("estadoCapacitacion"))
                        || admiteCerrada && "CERRADA".equals(fila.getString("estadoCapacitacion")))) {
                    throw new IllegalStateException(
                            "Solo se registra asistencia en participaciones vigentes de capacitaciones programadas.");
                }
                if (fila.getObject("fecha", LocalDate.class).isAfter(LocalDate.now())) {
                    throw new IllegalStateException(
                            "No se puede registrar asistencia antes de la fecha de capacitación.");
                }
                return new DatosParticipacion(fila.getInt("idTrabajador"), fila.getObject("fecha", LocalDate.class));
            }
        }
    }

    public Resultado consultarResultado(int idParticipacion) {
        return Jdbc.transaccion(c -> resultados.buscarPorParticipacion(c, idParticipacion));
    }

    public Resultado registrarResultado(int idParticipacion, String condicion, BigDecimal nota) {
        return guardarResultado(idParticipacion, condicion, nota, false);
    }

    public Resultado actualizarResultado(int idParticipacion, String condicion, BigDecimal nota) {
        return guardarResultado(idParticipacion, condicion, nota, true);
    }

    private Resultado guardarResultado(int id, String condicion, BigDecimal nota, boolean editar) {
        return Jdbc.transaccion(c -> {
            validarParticipacion(c, id, true);
            Resultado anterior = resultados.buscarPorParticipacion(c, id);
            if (editar != (anterior != null)) throw new IllegalStateException(editar ? "El resultado no existe." : "Ya existe un resultado. Utilice actualizar.");
            Resultado dato = new Resultado(anterior == null ? 0 : anterior.getIdResultado(), id, condicion, nota,
                    anterior == null ? null : anterior.getFechaRegistro());
            if (editar) dato.actualizar(); else dato.registrar();
            if (!dato.validarAsistencia(asistencias.buscarPorParticipacion(c, id))) {
                throw new IllegalStateException("Para registrar un resultado, la asistencia debe ser ASISTIO.");
            }
            if (!dato.esFavorable() && certificados.buscarPorParticipacion(c, id) != null) {
                throw new IllegalStateException("El resultado tiene un certificado registrado y debe mantenerse favorable.");
            }
            if (editar) resultados.actualizar(c, dato); else dato.setIdResultado(resultados.insertar(c, dato));
            Jdbc.actualizar(c, "UPDATE participacion SET estado='COMPLETADA' WHERE idParticipacion=?", id);
            return dato;
        });
    }

    public Certificado consultarCertificado(int idParticipacion) {
        return Jdbc.transaccion(c -> certificados.buscarPorParticipacion(c, idParticipacion));
    }

    public Certificado registrarCertificado(int id, String numero, LocalDate emision, LocalDate vencimiento, String evidencia) {
        return guardarCertificado(id, numero, emision, vencimiento, evidencia, false);
    }

    public Certificado actualizarCertificado(int id, String numero, LocalDate emision, LocalDate vencimiento, String evidencia) {
        return guardarCertificado(id, numero, emision, vencimiento, evidencia, true);
    }

    private Certificado guardarCertificado(int id, String numero, LocalDate emision, LocalDate vencimiento, String evidencia, boolean editar) {
        return Jdbc.transaccion(c -> {
            DatosParticipacion participante = validarParticipacion(c, id, true);
            Resultado resultado = resultados.buscarPorParticipacion(c, id);
            if (resultado == null || !resultado.esFavorable() || !resultado.validarAsistencia(asistencias.buscarPorParticipacion(c, id))) {
                throw new IllegalStateException("El certificado requiere asistencia y un resultado APROBADO o APTO.");
            }
            Certificado anterior = certificados.buscarPorParticipacion(c, id);
            if (editar != (anterior != null)) throw new IllegalStateException(editar ? "El certificado no existe." : "Ya existe un certificado. Utilice actualizar.");
            Certificado dato = new Certificado(anterior == null ? 0 : anterior.getIdCertificado(), id, participante.trabajador(),
                    numero, emision, vencimiento, evidencia);
            if (editar) dato.actualizar(); else dato.registrar();
            if (emision.isBefore(participante.fecha())) throw new IllegalArgumentException("La emisión no puede ser anterior a la capacitación.");
            if (editar) certificados.actualizar(c, dato); else dato.setIdCertificado(certificados.insertar(c, dato));
            return dato;
        });
    }
}
