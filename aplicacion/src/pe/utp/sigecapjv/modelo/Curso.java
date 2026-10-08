package pe.utp.sigecapjv.modelo;

import pe.utp.sigecapjv.util.Validador;

public class Curso {
    private int idCurso;
    private String codigo;
    private String nombre;
    private String tipo;
    private Integer vigenciaMeses;
    private String estado;

    public Curso() {}

    public Curso(int idCurso, String codigo, String nombre, String tipo, Integer vigenciaMeses, String estado) {
        this.idCurso = idCurso;
        this.codigo = codigo;
        this.nombre = nombre;
        this.tipo = tipo;
        this.vigenciaMeses = vigenciaMeses;
        this.estado = estado;
    }

    public void registrar() {
        validar();
    }

    public void actualizar() {
        validar();
    }

    public void activar() {
        validarDatos();
        estado = "ACTIVO";
    }

    public void desactivar() {
        validarDatos();
        estado = "INACTIVO";
    }

    private void validar() {
        validarDatos();
        Validador.estado(estado, "ACTIVO", "INACTIVO");
    }

    private void validarDatos() {
        Validador.texto(codigo, "Código");
        Validador.texto(nombre, "Nombre");
        Validador.texto(tipo, "Tipo");
        // null representa un curso sin vigencia; un periodo informado debe ser positivo.
        if (vigenciaMeses != null && vigenciaMeses <= 0) {
            throw new IllegalArgumentException("La vigencia debe ser mayor que cero meses.");
        }
        codigo = codigo.trim();
        nombre = nombre.trim();
        tipo = tipo.trim();
    }

    public Curso copia() {
        return new Curso(idCurso, codigo, nombre, tipo, vigenciaMeses, estado);
    }

    @Override
    public String toString() {
        return codigo + " - " + nombre;
    }

    public int getIdCurso() {
        return idCurso;
    }

    public void setIdCurso(int idCurso) {
        this.idCurso = idCurso;
    }

    public String getCodigo() {
        return codigo;
    }

    public void setCodigo(String codigo) {
        this.codigo = codigo;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getTipo() {
        return tipo;
    }

    public void setTipo(String tipo) {
        this.tipo = tipo;
    }

    public Integer getVigenciaMeses() {
        return vigenciaMeses;
    }

    public void setVigenciaMeses(Integer vigenciaMeses) {
        this.vigenciaMeses = vigenciaMeses;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }
}
