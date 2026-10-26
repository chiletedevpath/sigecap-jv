package pe.utp.sigecapjv.controlador;

import pe.utp.sigecapjv.dao.*;
import pe.utp.sigecapjv.modelo.Capacitacion;
import pe.utp.sigecapjv.modelo.Curso;
import pe.utp.sigecapjv.modelo.Participacion;
import pe.utp.sigecapjv.modelo.Trabajador;
import pe.utp.sigecapjv.util.Validador;

import java.util.List;

public class CapacitacionJdbcControlador extends CapacitacionControlador {
    private final SesionUsuario sesion;

    private final CursoDAO cursos = new CursoDAO();
    private final CapacitacionDAO capacitaciones = new CapacitacionDAO();
    private final ParticipacionDAO participaciones = new ParticipacionDAO();

    public CapacitacionJdbcControlador(PersonalJdbcControlador personal) {
        this(personal, null);
    }

    public CapacitacionJdbcControlador(PersonalJdbcControlador personal, SesionUsuario sesion) {
        super(personal);
        this.sesion = sesion;
    }

    @Override
    public void registrarCurso(Curso curso) {
        AccesoControlador.exigir(sesion, "capacitaciones", "gestionar");
        Validador.requerido(curso, "Curso");
        Curso nuevo = curso.copia();
        nuevo.registrar();
        curso.setIdCurso(Jdbc.transaccion(c -> cursos.insertar(c, nuevo)));
    }

    @Override
    public void actualizarCurso(Curso curso) {
        AccesoControlador.exigir(sesion, "capacitaciones", "gestionar");
        Validador.requerido(curso, "Curso");
        Curso nuevo = curso.copia();
        nuevo.actualizar();
        Jdbc.transaccion(c -> {
            cursos.buscar(c, nuevo.getIdCurso(), true);
            cursos.actualizar(c, nuevo);
            return null;
        });
    }

    @Override
    public void programarCapacitacion(Capacitacion datos) {
        AccesoControlador.exigir(sesion, "capacitaciones", "gestionar");
        Validador.requerido(datos, "Capacitación");
        Validador.requerido(datos.getCurso(), "Curso");
        Capacitacion guardada = Jdbc.transaccion(c -> {
            Curso curso = cursos.buscar(c, datos.getCurso().getIdCurso(), true);
            Capacitacion nueva = new Capacitacion(
                    0, datos.getFecha(), datos.getHoraInicio(), datos.getHoraFin(),
                                                  datos.getModalidad(), datos.getEstado(), curso);
            nueva.programar();
            nueva.setIdCapacitacion(capacitaciones.insertar(c, nueva));
            return nueva;
        });
        datos.setIdCapacitacion(guardada.getIdCapacitacion());
        datos.setEstado(guardada.getEstado());
    }

    @Override
    public void actualizarCapacitacion(Capacitacion datos) {
        AccesoControlador.exigir(sesion, "capacitaciones", "gestionar");
        Validador.requerido(datos, "Capacitación");
        Validador.requerido(datos.getCurso(), "Curso");
        Jdbc.transaccion(c -> {
            Capacitacion actual = capacitaciones.buscar(c, datos.getIdCapacitacion(), true);
            verificarSinSeguimiento(c, actual.getIdCapacitacion());
            Curso curso = cursos.buscar(c, datos.getCurso().getIdCurso(), true);
            Capacitacion nueva = new Capacitacion(
                    actual.getIdCapacitacion(), datos.getFecha(), datos.getHoraInicio(),
                                                  datos.getHoraFin(), datos.getModalidad(), actual.getEstado(), curso);
            nueva.actualizar();
            capacitaciones.actualizar(c, nueva);
            return null;
        });
    }

    @Override
    public void cancelarCapacitacion(int id) {
        AccesoControlador.exigir(sesion, "capacitaciones", "gestionar");
        cambiarEstado(id, false);
    }

    @Override
    public void cerrarCapacitacion(int id) {
        AccesoControlador.exigir(sesion, "capacitaciones", "gestionar");
        cambiarEstado(id, true);
    }

    private void cambiarEstado(int id, boolean cerrar) {
        Jdbc.transaccion(c -> {
            Capacitacion actual = capacitaciones.buscar(c, id, true);
            if (!cerrar) verificarSinSeguimiento(c, id);
            if (cerrar) actual.cerrar();
            else actual.cancelar();
            capacitaciones.actualizar(c, actual);
            return null;
        });
    }

    private void verificarSinSeguimiento(java.sql.Connection conexion, int id) throws java.sql.SQLException {

        if (capacitaciones.tieneSeguimiento(conexion, id)) {
            throw new IllegalStateException(
                    "La capacitación tiene seguimiento registrado; " + "no se puede reprogramar ni cancelar.");
        }
    }

    @Override
    public Participacion asignarTrabajador(Trabajador trabajador, Capacitacion capacitacion) {
        AccesoControlador.exigir(sesion, "capacitaciones", "gestionar");
        Validador.requerido(trabajador, "Trabajador");
        Validador.requerido(capacitacion, "Capacitación");
        return Jdbc.transaccion(c -> {
            Capacitacion actual = capacitaciones.buscar(c, capacitacion.getIdCapacitacion(), true);
            Trabajador registrado = new TrabajadorDAO().buscar(c, trabajador.getIdTrabajador(), true);
            Participacion nueva = new Participacion(0, null, null, registrado, actual);
            nueva.asignarTrabajador();
            nueva.setIdParticipacion(participaciones.insertar(c, nueva));
            return nueva.copia();
        });
    }

    @Override
    public List<Curso> listarCursos() {
        AccesoControlador.exigir(sesion, "capacitaciones", "consultar");
        return Jdbc.transaccion(cursos::listar);
    }

    @Override
    public List<Capacitacion> listarCapacitaciones() {
        AccesoControlador.exigir(sesion, "capacitaciones", "consultar");
        return Jdbc.transaccion(c -> {
            List<Capacitacion> lista = capacitaciones.listar(c);
            List<Participacion> asignaciones = participaciones.listar(c);
            for (Capacitacion capacitacion : lista) {
                capacitacion.restaurarParticipaciones(asignaciones.stream().filter(
                        p -> p.getCapacitacion().getIdCapacitacion() == capacitacion.getIdCapacitacion()).toList());
            }
            return lista;
        });
    }

    @Override
    public List<Participacion> listarParticipaciones() {
        AccesoControlador.exigir(sesion, "capacitaciones", "consultar");
        return Jdbc.transaccion(participaciones::listar);
    }
}
