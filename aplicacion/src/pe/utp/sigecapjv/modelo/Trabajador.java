package pe.utp.sigecapjv.modelo;

import pe.utp.sigecapjv.util.Validador;

public class Trabajador {
    private int idTrabajador;
    private String dni;
    private String nombres;
    private String apellidos;
    private String cargo;
    private String estado;

    public Trabajador() {}

    public Trabajador(int idTrabajador, String dni, String nombres, String apellidos, String cargo, String estado) {
        this.idTrabajador = idTrabajador;
        this.dni = dni;
        this.nombres = nombres;
        this.apellidos = apellidos;
        this.cargo = cargo;
        this.estado = estado;
    }

    // Valida el objeto; el controlador administra su registro en memoria.
    public void registrar() { validar(); }
    public void actualizar() { validar(); }

    private void validar() {
        if (!Validador.texto(dni, "DNI").matches("[0-9]{8}"))
            throw new IllegalArgumentException("El DNI debe contener ocho dígitos.");
        Validador.texto(nombres, "Nombres");
        Validador.texto(apellidos, "Apellidos");
        Validador.texto(cargo, "Cargo");
        Validador.estado(estado, "ACTIVO", "INACTIVO");
        dni = dni.trim(); nombres = nombres.trim(); apellidos = apellidos.trim(); cargo = cargo.trim();
    }

    // Las firmas del UML se conservan; el historial completo pertenece a otra etapa.
    public void consultarHistorial() {
        throw new UnsupportedOperationException("La consulta del historial todavía no está implementada.");
    }
    public void consultarCapacitaciones() {
        throw new UnsupportedOperationException("La consulta de capacitaciones del trabajador todavía no está implementada.");
    }

    public Trabajador copia() {
        return new Trabajador(idTrabajador, dni, nombres, apellidos, cargo, estado);
    }
    @Override public String toString() { return dni + " - " + apellidos + ", " + nombres; }

    public int getIdTrabajador() { return idTrabajador; }
    public void setIdTrabajador(int idTrabajador) { this.idTrabajador = idTrabajador; }
    public String getDni() { return dni; }
    public void setDni(String dni) { this.dni = dni; }
    public String getNombres() { return nombres; }
    public void setNombres(String nombres) { this.nombres = nombres; }
    public String getApellidos() { return apellidos; }
    public void setApellidos(String apellidos) { this.apellidos = apellidos; }
    public String getCargo() { return cargo; }
    public void setCargo(String cargo) { this.cargo = cargo; }
    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }
}
