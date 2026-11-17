package pe.utp.sigecapjv.modelo;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Filtros aplicados y filas obtenidas. La exportación usa este mismo objeto,
 * por lo que conserva la información y los filtros con los que se generó.
 * Un filtro nulo significa "todos".
 */
public class Reporte {
    public static final String[] ESTADOS = {"PENDIENTE", "COMPLETADA", "CANCELADA"};
    public static final String[] VIGENCIAS = {"VIGENTE", "PROXIMO_A_VENCER", "VENCIDO", "NO_APLICA", "SIN_CERTIFICADO"};

    public record Fila(String trabajador, String curso, LocalDate fecha, String estado, String resultado,
                       String vigencia) {
    }

    private final LocalDate desde;
    private final LocalDate hasta;
    private final Curso curso;
    private final Trabajador trabajador;
    private final String estado;
    private final String vigencia;
    private List<Fila> filas = List.of();
    private LocalDateTime fechaGeneracion;

    public Reporte(LocalDate desde, LocalDate hasta, Curso curso, Trabajador trabajador, String estado,
                   String vigencia) {
        this.desde = desde;
        this.hasta = hasta;
        this.curso = curso;
        this.trabajador = trabajador;
        this.estado = estado;
        this.vigencia = vigencia;
    }

    public void validar() {
        if (desde != null && hasta != null && desde.isAfter(hasta))
            throw new IllegalArgumentException("La fecha Desde no puede ser posterior a la fecha Hasta.");
        if (estado != null && !List.of(ESTADOS).contains(estado))
            throw new IllegalArgumentException("Estado inválido: " + estado);
        if (vigencia != null && !List.of(VIGENCIAS).contains(vigencia))
            throw new IllegalArgumentException("Vigencia inválida: " + vigencia);
    }

    /** La vigencia se calcula al consultar, por eso se filtra después de obtener las filas. */
    public void generar(List<Fila> obtenidas) {
        validar();
        filas = obtenidas.stream().filter(f -> vigencia == null || vigencia.equals(f.vigencia())).toList();
        fechaGeneracion = LocalDateTime.now().withNano(0);
    }

    public List<String> describirFiltros() {
        String periodo = desde == null && hasta == null ? "Todos"
                : desde == null ? "hasta " + hasta
                : hasta == null ? "desde " + desde
                : desde + " a " + hasta;
        return List.of(
                "Periodo: " + periodo,
                "Curso: " + (curso == null ? "Todos" : curso),
                "Trabajador: " + (trabajador == null ? "Todos" : trabajador),
                "Estado: " + (estado == null ? "Todos" : estado),
                "Vigencia: " + (vigencia == null ? "Todas" : vigencia));
    }

    public boolean estaGenerado() {
        return fechaGeneracion != null;
    }

    public LocalDate getDesde() {
        return desde;
    }

    public LocalDate getHasta() {
        return hasta;
    }

    public Curso getCurso() {
        return curso;
    }

    public Trabajador getTrabajador() {
        return trabajador;
    }

    public String getEstado() {
        return estado;
    }

    public String getVigencia() {
        return vigencia;
    }

    public List<Fila> getFilas() {
        return filas;
    }

    public LocalDateTime getFechaGeneracion() {
        return fechaGeneracion;
    }
}
