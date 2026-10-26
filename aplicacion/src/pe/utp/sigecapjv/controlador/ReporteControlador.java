package pe.utp.sigecapjv.controlador;

import pe.utp.sigecapjv.dao.CursoDAO;
import pe.utp.sigecapjv.dao.Jdbc;
import pe.utp.sigecapjv.dao.ReporteDAO;
import pe.utp.sigecapjv.dao.TrabajadorDAO;
import pe.utp.sigecapjv.modelo.Curso;
import pe.utp.sigecapjv.modelo.Reporte;
import pe.utp.sigecapjv.modelo.Trabajador;
import pe.utp.sigecapjv.util.ReportePdf;
import pe.utp.sigecapjv.util.Validador;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

/**
 * HU-JVR-011: consulta de solo lectura sobre los datos registrados por los demás módulos.
 * Todavía no existe un permiso propio de reportes; se exige consultar capacitaciones,
 * que según las reglas de roles también incluye consultar personal.
 */
public class ReporteControlador {
    private final SesionUsuario sesion;
    public ReporteControlador() { this(null); }
    public ReporteControlador(SesionUsuario sesion) { this.sesion = sesion; }

    private final ReporteDAO reportes = new ReporteDAO();
    private final CursoDAO cursos = new CursoDAO();
    private final TrabajadorDAO trabajadores = new TrabajadorDAO();

    public String rolActual() {
        return sesion == null ? "" : sesion.getRol();
    }

    public List<Curso> listarCursos() {
        AccesoControlador.exigir(sesion, "capacitaciones", "consultar");
        return Jdbc.transaccion(cursos::listar);
    }

    public List<Trabajador> listarTrabajadores() {
        AccesoControlador.exigir(sesion, "capacitaciones", "consultar");
        return Jdbc.transaccion(trabajadores::listar);
    }

    public Reporte generar(Reporte reporte) {
        AccesoControlador.exigir(sesion, "capacitaciones", "consultar");
        Validador.requerido(reporte, "Reporte");
        reporte.validar();
        reporte.generar(Jdbc.transaccion(c -> reportes.consultar(c, reporte)));
        return reporte;
    }

    public void exportarPdf(Reporte reporte, Path destino) {
        AccesoControlador.exigir(sesion, "capacitaciones", "consultar");
        Validador.requerido(reporte, "Reporte");
        Validador.requerido(destino, "Archivo de destino");
        if (!reporte.estaGenerado() || reporte.getFilas().isEmpty())
            throw new IllegalStateException("Genere un reporte con resultados antes de exportarlo.");
        try {
            ReportePdf.escribir(reporte, destino);
        } catch (IOException ex) {
            throw new IllegalStateException("No se pudo guardar el PDF. Revise la carpeta y los permisos.", ex);
        }
    }
}
