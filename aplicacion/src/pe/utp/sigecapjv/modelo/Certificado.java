package pe.utp.sigecapjv.modelo;

import pe.utp.sigecapjv.util.Validador;

import java.time.LocalDate;

public class Certificado {
    private int idCertificado;
    private final int idParticipacion;
    private final int idTrabajador;
    private String numero;
    private final LocalDate fechaEmision;
    private final LocalDate fechaVencimiento;
    private String rutaEvidencia;

    public Certificado(int idCertificado, int idParticipacion, int idTrabajador, String numero,
                       LocalDate emision, LocalDate vencimiento, String rutaEvidencia) {
        this.idCertificado = idCertificado;
        this.idParticipacion = idParticipacion;
        this.idTrabajador = idTrabajador;
        this.numero = numero;
        this.fechaEmision = emision;
        this.fechaVencimiento = vencimiento;
        this.rutaEvidencia = rutaEvidencia;
    }

    public void registrar() {
        if (idCertificado != 0) throw new IllegalStateException("El certificado ya está registrado.");
        validar();
    }

    public void actualizar() {
        if (idCertificado <= 0) throw new IllegalStateException("El certificado no está registrado.");
        validar();
    }

    private void validar() {
        if (idParticipacion <= 0 || idTrabajador <= 0)
            throw new IllegalArgumentException("Participación o trabajador inválido.");
        numero = Validador.texto(numero, "Número de certificado");
        if (numero.length() > 80) throw new IllegalArgumentException("El número admite hasta 80 caracteres.");
        if (!validarFechas()) throw new IllegalArgumentException(
                "Revise las fechas: emisión no futura y vencimiento igual o posterior a emisión.");
        rutaEvidencia = rutaEvidencia == null || rutaEvidencia.isBlank() ? null : rutaEvidencia.trim();
        if (rutaEvidencia != null && rutaEvidencia.length() > 500)
            throw new IllegalArgumentException("La ruta admite hasta 500 caracteres.");
    }

    public boolean validarFechas() {
        return fechaEmision != null && !fechaEmision.isAfter(LocalDate.now())
                && (fechaVencimiento == null || !fechaVencimiento.isBefore(fechaEmision));
    }

    public String calcularEstadoVigencia() {
        return calcularEstadoVigencia(LocalDate.now());
    }

    public String calcularEstadoVigencia(LocalDate hoy) {
        Validador.requerido(hoy, "Fecha de consulta");
        if (fechaVencimiento == null) return "NO_APLICA";
        if (fechaVencimiento.isBefore(hoy)) return "VENCIDO";
        // El día de vencimiento todavía es vigente, como en la vista SQL.
        return "VIGENTE";
    }

    public int getIdCertificado() {
        return idCertificado;
    }

    public void setIdCertificado(int id) {
        idCertificado = id;
    }

    public int getIdParticipacion() {
        return idParticipacion;
    }

    public int getIdTrabajador() {
        return idTrabajador;
    }

    public String getNumero() {
        return numero;
    }

    public LocalDate getFechaEmision() {
        return fechaEmision;
    }

    public LocalDate getFechaVencimiento() {
        return fechaVencimiento;
    }

    public String getRutaEvidencia() {
        return rutaEvidencia;
    }

    public String getEstadoVigencia() {
        return calcularEstadoVigencia();
    }
}
