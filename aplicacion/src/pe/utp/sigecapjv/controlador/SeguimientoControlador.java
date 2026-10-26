package pe.utp.sigecapjv.controlador;

import pe.utp.sigecapjv.dao.*;
import pe.utp.sigecapjv.modelo.Asistencia;
import pe.utp.sigecapjv.modelo.Certificado;
import pe.utp.sigecapjv.modelo.Participacion;
import pe.utp.sigecapjv.modelo.Resultado;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;

public class SeguimientoControlador {
    private final SesionUsuario sesion;

    public SeguimientoControlador() {
        this(null);
    }

    public SeguimientoControlador(SesionUsuario sesion) {
        this.sesion = sesion;
    }

    private final AsistenciaDAO asistencias = new AsistenciaDAO();
    private final ResultadoDAO resultados = new ResultadoDAO();
    private final CertificadoDAO certificados = new CertificadoDAO();
    private final ParticipacionDAO participaciones = new ParticipacionDAO();

    public Asistencia consultarAsistencia(int idParticipacion) {
        AccesoControlador.exigir(sesion, "capacitaciones", "consultar");
        if (idParticipacion <= 0) throw new IllegalArgumentException("Participación inválida.");
        return Jdbc.transaccion(conexion -> asistencias.buscarPorParticipacion(conexion, idParticipacion));
    }

    public Asistencia registrarAsistencia(int idParticipacion, String condicion) {
        AccesoControlador.exigir(sesion, "capacitaciones", "gestionar");
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
        AccesoControlador.exigir(sesion, "capacitaciones", "gestionar");
        return Jdbc.transaccion(conexion -> {
            validarParticipacion(conexion, idParticipacion);
            Asistencia asistencia = asistencias.buscarPorParticipacion(conexion, idParticipacion);
            if (asistencia == null) throw new IllegalArgumentException("La participación aún no tiene asistencia.");
            asistencia.setCondicion(condicion);
            asistencia.actualizar();
            if (!"ASISTIO".equals(asistencia.getCondicion()) && resultados.buscarPorParticipacion(
                    conexion, idParticipacion) != null) {
                throw new IllegalStateException(
                        "La asistencia tiene un resultado registrado y debe mantenerse como ASISTIO.");
            }
            asistencias.actualizar(conexion, asistencia);
            return asistencia;
        });
    }

    private void validarParticipacion(Connection conexion, int idParticipacion) throws SQLException {

        validarParticipacion(conexion, idParticipacion, false);
    }

    private Participacion validarParticipacion(Connection conexion, int idParticipacion, boolean admiteCerrada) throws
            SQLException {

        Participacion participacion = participaciones.buscar(conexion, idParticipacion, true);

        String estadoParticipacion = participacion.getEstado();

        String estadoCapacitacion = participacion.getCapacitacion().getEstado();

        if ("CANCELADA".equals(estadoParticipacion) || !("PROGRAMADA".equals(
                estadoCapacitacion) || (admiteCerrada && "CERRADA".equals(estadoCapacitacion)))) {

            throw new IllegalStateException(
                    "Solo se registra seguimiento en participaciones " + "vigentes de capacitaciones programadas.");
        }

        if (participacion.getCapacitacion().getFecha().isAfter(LocalDate.now())) {

            throw new IllegalStateException("No se puede registrar asistencia antes " + "de la fecha de capacitación.");
        }

        return participacion;
    }

    public Resultado consultarResultado(int idParticipacion) {
        AccesoControlador.exigir(sesion, "capacitaciones", "consultar");
        return Jdbc.transaccion(c -> resultados.buscarPorParticipacion(c, idParticipacion));
    }

    public Resultado registrarResultado(int idParticipacion, String condicion, BigDecimal nota) {
        AccesoControlador.exigir(sesion, "capacitaciones", "gestionar");
        return guardarResultado(idParticipacion, condicion, nota, false);
    }

    public Resultado actualizarResultado(int idParticipacion, String condicion, BigDecimal nota) {
        AccesoControlador.exigir(sesion, "capacitaciones", "gestionar");
        return guardarResultado(idParticipacion, condicion, nota, true);
    }

    private Resultado guardarResultado(int id, String condicion, BigDecimal nota, boolean editar) {
        return Jdbc.transaccion(c -> {
            validarParticipacion(c, id, true);
            Resultado anterior = resultados.buscarPorParticipacion(c, id);
            if (editar != (anterior != null)) throw new IllegalStateException(
                    editar ? "El resultado no existe." : "Ya existe un resultado. Utilice actualizar.");
            Resultado dato = new Resultado(
                    anterior == null ? 0 : anterior.getIdResultado(), id, condicion, nota,
                                           anterior == null ? null : anterior.getFechaRegistro());
            if (editar) dato.actualizar();
            else dato.registrar();
            if (!dato.validarAsistencia(asistencias.buscarPorParticipacion(c, id))) {
                throw new IllegalStateException("Para registrar un resultado, la asistencia debe ser ASISTIO.");
            }
            if (!dato.esFavorable() && certificados.buscarPorParticipacion(c, id) != null) {
                throw new IllegalStateException(
                        "El resultado tiene un certificado registrado y debe mantenerse favorable.");
            }
            if (editar) resultados.actualizar(c, dato);
            else dato.setIdResultado(resultados.insertar(c, dato));
            participaciones.actualizarEstado(c, id, "COMPLETADA");
            return dato;
        });
    }

    public Certificado consultarCertificado(int idParticipacion) {
        AccesoControlador.exigir(sesion, "capacitaciones", "consultar");
        return Jdbc.transaccion(c -> certificados.buscarPorParticipacion(c, idParticipacion));
    }

    public Certificado registrarCertificado(int id, String numero, LocalDate emision, LocalDate vencimiento,
                                            String evidencia) {
        AccesoControlador.exigir(sesion, "capacitaciones", "gestionar");
        return guardarCertificado(id, numero, emision, vencimiento, evidencia, false);
    }

    public Certificado actualizarCertificado(int id, String numero, LocalDate emision, LocalDate vencimiento,
                                             String evidencia) {
        AccesoControlador.exigir(sesion, "capacitaciones", "gestionar");
        return guardarCertificado(id, numero, emision, vencimiento, evidencia, true);
    }

    private Certificado guardarCertificado(int id, String numero, LocalDate emision, LocalDate vencimiento,
                                           String evidencia, boolean editar) {
        return Jdbc.transaccion(c -> {
            Participacion participante = validarParticipacion(c, id, true);
            Resultado resultado = resultados.buscarPorParticipacion(c, id);
            if (resultado == null || !resultado.esFavorable() || !resultado.validarAsistencia(
                    asistencias.buscarPorParticipacion(c, id))) {
                throw new IllegalStateException("El certificado requiere asistencia y un resultado APROBADO o APTO.");
            }
            Certificado anterior = certificados.buscarPorParticipacion(c, id);
            if (editar != (anterior != null)) throw new IllegalStateException(
                    editar ? "El certificado no existe." : "Ya existe un certificado. Utilice actualizar.");
            Certificado dato = new Certificado(
                    anterior == null ? 0 : anterior.getIdCertificado(), id,
                                               participante.getTrabajador().getIdTrabajador(), numero, emision,
                                               vencimiento, evidencia);
            if (editar) dato.actualizar();
            else dato.registrar();
            if (emision.isBefore(participante.getCapacitacion().getFecha()))
                throw new IllegalArgumentException("La emisión no puede ser anterior a la capacitación.");
            if (editar) certificados.actualizar(c, dato);
            else dato.setIdCertificado(certificados.insertar(c, dato));
            return dato;
        });
    }
}
