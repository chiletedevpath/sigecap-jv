package pe.utp.sigecapjv;

import java.awt.*;
import java.lang.reflect.Modifier;
import java.time.*;
import javax.swing.*;
import pe.utp.sigecapjv.modelo.*;
import pe.utp.sigecapjv.controlador.*;
import pe.utp.sigecapjv.vista.*;

public class PruebaFlujo {
    private static int comprobaciones;
    private static void comprobar(boolean condicion, String mensaje) {
        if (!condicion) throw new AssertionError(mensaje);
        comprobaciones++;
    }
    private static void rechazar(Runnable accion, String mensaje) {
        try { accion.run(); } catch (IllegalArgumentException | IllegalStateException | UnsupportedOperationException ex) {
            comprobaciones++; return;
        }
        throw new AssertionError("No se rechazó: " + mensaje);
    }
    private static Trabajador trabajador(String dni) {
        return new Trabajador(0, dni, "Persona", "Ejemplo", "Agente de seguridad", "ACTIVO");
    }
    private static Curso curso(String codigo) {
        return new Curso(0, codigo, "Curso de demostración", "Interno", null, "ACTIVO");
    }
    private static Capacitacion capacitacion(Curso curso) {
        return new Capacitacion(0, LocalDate.now().plusDays(2), LocalTime.of(9, 0), LocalTime.of(13, 0), "Presencial", null, curso);
    }
    public static void main(String[] args) throws Exception {
        if (args.length > 0 && args[0].equals("--ventana")) {
            SwingUtilities.invokeAndWait(() -> verificarVentana());
            System.out.println("OK: ventana principal creada y navegación Personal/Capacitaciones verificada.");
            return;
        }
        PersonalControlador personal = new PersonalControlador();
        CapacitacionControlador gestion = new CapacitacionControlador(personal);
        Trabajador t = trabajador("00000001"); personal.registrarTrabajador(t);
        Curso c = curso("DEMO-001"); gestion.registrarCurso(c);
        Capacitacion cap = capacitacion(c); gestion.programarCapacitacion(cap);
        Participacion p = gestion.asignarTrabajador(t, cap);
        comprobar(t.getIdTrabajador() == 1 && c.getIdCurso() == 1 && cap.getIdCapacitacion() == 1, "Identificadores");
        comprobar("PROGRAMADA".equals(cap.getEstado()), "Estado de programación");
        comprobar("PENDIENTE".equals(p.getEstado()) && LocalDate.now().equals(p.getFechaAsignacion()), "Asignación");
        comprobar(gestion.listarCapacitaciones().get(0).getParticipaciones().size() == 1, "Composición UML");
        comprobar(p.getTrabajador().getIdTrabajador() == t.getIdTrabajador() && p.getCapacitacion().getIdCapacitacion() == cap.getIdCapacitacion(), "Relaciones UML");
        rechazar(() -> personal.registrarTrabajador(trabajador("00000001")), "DNI duplicado");
        for (String dni : new String[]{null, "", "1234567", "123456789", "AB123456"})
            rechazar(() -> trabajador(dni).registrar(), "Formato DNI");
        Trabajador invalido = trabajador("00000002"); invalido.setNombres(" "); rechazar(invalido::registrar, "Nombre vacío");
        invalido.setNombres("Persona"); invalido.setApellidos(null); rechazar(invalido::registrar, "Apellidos vacíos");
        invalido.setApellidos("Ejemplo"); invalido.setCargo(""); rechazar(invalido::registrar, "Cargo vacío");
        invalido.setCargo("Agente"); invalido.setEstado("OTRO"); rechazar(invalido::registrar, "Estado inválido");
        rechazar(() -> personal.registrarTrabajador(null), "Trabajador nulo");
        Trabajador segundo = trabajador("00000002"); personal.registrarTrabajador(segundo);
        Trabajador editar = personal.buscarTrabajador(segundo.getIdTrabajador()); editar.setDni("00000001");
        rechazar(() -> personal.actualizarTrabajador(editar), "DNI duplicado en actualización");
        comprobar("00000002".equals(personal.buscarTrabajador(2).getDni()), "Actualización fallida conserva datos");
        editar.setDni("00000002"); editar.setNombres(""); rechazar(() -> personal.actualizarTrabajador(editar), "Actualización inválida");
        editar.setNombres("Otra persona"); personal.actualizarTrabajador(editar);
        comprobar("Otra persona".equals(personal.buscarTrabajador(2).getNombres()), "Actualización válida");
        t.setNombres("Cambio externo"); comprobar("Persona".equals(personal.buscarTrabajador(1).getNombres()), "Registro defensivo");
        personal.listarTrabajadores().get(0).setEstado("INACTIVO"); comprobar("ACTIVO".equals(personal.buscarTrabajador(1).getEstado()), "Lectura defensiva");
        rechazar(() -> personal.listarTrabajadores().clear(), "Lista protegida");
        Curso duplicado = curso("demo-001"); duplicado.setIdCurso(c.getIdCurso());
        rechazar(() -> gestion.registrarCurso(duplicado), "Código duplicado con ID externo");
        Curso malo = curso(" "); rechazar(malo::registrar, "Código vacío");
        malo.setCodigo("DEMO-002"); malo.setNombre(""); rechazar(malo::registrar, "Nombre de curso vacío");
        malo.setNombre("Prueba"); malo.setTipo(null); rechazar(malo::registrar, "Tipo vacío");
        malo.setTipo("Interno"); malo.setVigenciaMeses(0); rechazar(malo::registrar, "Vigencia cero");
        malo.setVigenciaMeses(-1); rechazar(malo::registrar, "Vigencia negativa");
        malo.setVigenciaMeses(12); malo.registrar(); malo.desactivar(); comprobar("INACTIVO".equals(malo.getEstado()), "Desactivar curso");
        malo.activar(); comprobar("ACTIVO".equals(malo.getEstado()), "Activar curso");
        Capacitacion sinCurso = capacitacion(null); rechazar(sinCurso::programar, "Curso ausente");
        Capacitacion horario = capacitacion(c); horario.setHoraFin(horario.getHoraInicio()); rechazar(horario::programar, "Horas iguales");
        horario.setHoraFin(LocalTime.of(8, 0)); rechazar(horario::programar, "Fin anterior");
        horario.setHoraFin(LocalTime.of(13, 0)); horario.setFecha(LocalDate.now().minusDays(1)); rechazar(horario::programar, "Fecha pasada");
        horario.setFecha(null); rechazar(horario::programar, "Fecha ausente");
        horario.setFecha(LocalDate.now().plusDays(1)); horario.setHoraInicio(null); rechazar(horario::programar, "Inicio ausente");
        horario.setHoraInicio(LocalTime.of(9, 0)); horario.setHoraFin(null); rechazar(horario::programar, "Fin ausente");
        horario.setHoraFin(LocalTime.of(13, 0)); horario.setModalidad(" "); rechazar(horario::programar, "Modalidad ausente");
        rechazar(() -> gestion.programarCapacitacion(capacitacion(curso("NO-REGISTRADO"))), "Curso no registrado");
        c.desactivar(); gestion.actualizarCurso(c); rechazar(() -> gestion.programarCapacitacion(capacitacion(c)), "Curso inactivo");
        c.activar(); gestion.actualizarCurso(c);
        rechazar(() -> gestion.asignarTrabajador(t, cap), "Asignación duplicada");
        comprobar(gestion.listarParticipaciones().size() == 1, "Sin asignación extra tras rechazo");
        Trabajador cambioDni = personal.buscarTrabajador(1); cambioDni.setDni("00000003"); personal.actualizarTrabajador(cambioDni);
        comprobar("00000003".equals(gestion.listarParticipaciones().get(0).getTrabajador().getDni()), "Relación conserva datos actualizados");
        rechazar(() -> gestion.asignarTrabajador(cambioDni, cap), "Duplicado por ID después de cambiar DNI");
        segundo.setEstado("INACTIVO"); personal.actualizarTrabajador(segundo);
        rechazar(() -> gestion.asignarTrabajador(segundo, cap), "Trabajador inactivo");
        rechazar(() -> gestion.asignarTrabajador(trabajador("00000004"), cap), "Trabajador no registrado");
        rechazar(() -> gestion.asignarTrabajador(t, capacitacion(c)), "Capacitación no registrada");
        rechazar(() -> gestion.asignarTrabajador(null, cap), "Trabajador ausente");
        rechazar(() -> gestion.asignarTrabajador(t, null), "Capacitación ausente");
        Capacitacion edicion = gestion.listarCapacitaciones().get(0); edicion.setHoraFin(LocalTime.of(8, 0));
        rechazar(() -> gestion.actualizarCapacitacion(edicion), "Actualización de sesión inválida");
        comprobar(LocalTime.of(13, 0).equals(gestion.listarCapacitaciones().get(0).getHoraFin()), "Conserva horario previo");
        edicion.setHoraFin(LocalTime.of(14, 0)); gestion.actualizarCapacitacion(edicion);
        comprobar(LocalTime.of(14, 0).equals(gestion.listarCapacitaciones().get(0).getHoraFin()), "Actualiza horario");
        gestion.listarCapacitaciones().get(0).getCurso().desactivar();
        comprobar("ACTIVO".equals(gestion.listarCursos().get(0).getEstado()), "Curso protegido en consulta");
        gestion.listarParticipaciones().get(0).getTrabajador().setNombres("Cambio externo");
        comprobar("Persona".equals(gestion.listarParticipaciones().get(0).getTrabajador().getNombres()), "Participación protegida");
        rechazar(t::consultarHistorial, "Historial explícitamente pendiente");
        rechazar(t::consultarCapacitaciones, "Consulta explícitamente pendiente");
        for (Class<?> clase : new Class<?>[]{Trabajador.class, Curso.class, Capacitacion.class, Participacion.class}) {
            for (var atributo : clase.getDeclaredFields()) comprobar(Modifier.isPrivate(atributo.getModifiers()), "Encapsulamiento " + atributo);
        }
        comprobar(Trabajador.class.getMethod("consultarHistorial").getReturnType() == void.class, "Firma UML historial");
        comprobar(Participacion.class.getMethod("actualizarEstado").getReturnType() == void.class, "Firma UML participación");
        Participacion directa = new Participacion(0, null, null, trabajador("00000005"), capacitacion(c));
        rechazar(directa::asignarTrabajador, "Sesión no programada");
        Capacitacion nueva = capacitacion(c); nueva.programar(); directa.setCapacitacion(nueva); directa.asignarTrabajador();
        directa.setEstado("CANCELADA"); directa.actualizarEstado(); comprobar("CANCELADA".equals(directa.getEstado()), "Estado de participación");
        directa.setEstado("COMPLETADA"); rechazar(directa::actualizarEstado, "No simular cumplimiento sin resultado");
        SwingUtilities.invokeAndWait(() -> { verificarVistas(personal, gestion); verificarEventos(); });
        gestion.cerrarCapacitacion(cap.getIdCapacitacion()); rechazar(() -> gestion.asignarTrabajador(t, cap), "Sesión cerrada");
        rechazar(() -> gestion.actualizarCapacitacion(edicion), "Editar sesión cerrada");
        Capacitacion cancelada = capacitacion(c); gestion.programarCapacitacion(cancelada); gestion.cancelarCapacitacion(cancelada.getIdCapacitacion());
        rechazar(() -> gestion.asignarTrabajador(t, cancelada), "Sesión cancelada");
        System.out.println("OK: " + comprobaciones + " comprobaciones. Flujo trabajador -> curso -> capacitación -> participación verificado.");
    }
    private static void verificarVistas(PersonalControlador personal, CapacitacionControlador gestion) {
        PnlPersonal p = new PnlPersonal(personal); PnlCapacitaciones c = new PnlCapacitaciones(gestion, personal);
        JTabbedPane tp = encontrar(p, JTabbedPane.class), tc = encontrar(c, JTabbedPane.class);
        comprobar(!tp.isEnabledAt(2) && !tc.isEnabledAt(3), "Funciones futuras deshabilitadas");
        comprobar(encontrar(tp.getComponentAt(0), JTable.class).getRowCount() == 2, "Personal usa registros reales");
        comprobar(encontrar(tc.getComponentAt(0), JTable.class).getRowCount() == 1, "Cursos usan controlador");
        comprobar(encontrar(tc.getComponentAt(1), JTable.class).getRowCount() == 1, "Programación usa controlador");
        comprobar(encontrar(tc.getComponentAt(2), JTable.class).getRowCount() == 1, "Participantes usan controlador");

    }
    private static void verificarVentana() {
        FrmPrincipal ventana = new FrmPrincipal();
        try {
            ventana.addNotify(); ventana.validate();
            JPanel contenido = (JPanel) ventana.getContentPane();
            comprobar(encontrar(contenido, PnlPersonal.class) != null, "Ventana integra Personal");
            comprobar(encontrar(contenido, PnlCapacitaciones.class) != null, "Ventana integra Capacitaciones");
            for (int ancho : new int[]{1100, 1220}) {
                pulsar(boton(contenido, "Personal")); ventana.setSize(ancho, 750); ventana.validate();
                comprobar(encontrar(contenido, PnlPersonal.class).isVisible(), "Navegación a Personal");
                pulsar(boton(contenido, "Capacitaciones")); ventana.validate();
                comprobar(encontrar(contenido, PnlCapacitaciones.class).isVisible(), "Navegación a Capacitaciones");
            }
        } catch (Exception ex) { throw new RuntimeException(ex); }
        finally { ventana.dispose(); }
    }
    private static <T> T encontrar(Component c, Class<T> tipo) {
        if (tipo.isInstance(c)) return tipo.cast(c);
        if (c instanceof Container padre) for (Component hijo : padre.getComponents()) {
            T resultado = encontrar(hijo, tipo); if (resultado != null) return resultado;
        }
        return null;
    }
    private static JButton boton(Component c, String texto) {
        if (c instanceof JButton b && texto.equals(b.getText())) return b;
        if (c instanceof Container padre) for (Component hijo : padre.getComponents()) {
            JButton resultado = boton(hijo, texto); if (resultado != null) return resultado;
        }
        return null;
    }
    private static JTextField campo(Object panel, String nombre) {
        try { var f = panel.getClass().getDeclaredField(nombre); f.setAccessible(true); return (JTextField) f.get(panel); }
        catch (ReflectiveOperationException ex) { throw new AssertionError(ex); }
    }
    private static void pulsar(JButton boton) {
        // Headless permite comprobar los eventos y datos; los diálogos nativos se omiten.
        try { boton.doClick(0); } catch (HeadlessException esperado) { }
    }
    private static void verificarEventos() {
        PersonalControlador personal = new PersonalControlador();
        CapacitacionControlador gestion = new CapacitacionControlador(personal);
        PnlPersonal panel = new PnlPersonal(personal);
        JTabbedPane tabs = encontrar(panel, JTabbedPane.class);
        campo(panel, "dni").setText("00000010"); campo(panel, "nombres").setText("Persona de prueba");
        campo(panel, "apellidos").setText("Ficticia"); campo(panel, "cargo").setText("Agente");
        pulsar(boton(tabs.getComponentAt(1), "Guardar"));
        comprobar(personal.listarTrabajadores().size() == 1, "Evento Guardar registra trabajador");
        JTable tabla = encontrar(tabs.getComponentAt(0), JTable.class);
        comprobar(tabla.getRowCount() == 1, "Listado actualizado después de Guardar");
        tabla.setRowSelectionInterval(0, 0); pulsar(boton(tabs.getComponentAt(0), "Editar trabajador"));
        campo(panel, "nombres").setText("Persona actualizada"); pulsar(boton(tabs.getComponentAt(1), "Guardar"));
        comprobar("Persona actualizada".equals(personal.buscarTrabajador(1).getNombres()), "Evento Editar actualiza trabajador");
        campo(panel, "dni").setText("00000010"); campo(panel, "nombres").setText("Otra persona");
        campo(panel, "apellidos").setText("Ficticia"); campo(panel, "cargo").setText("Agente");
        pulsar(boton(tabs.getComponentAt(1), "Guardar"));
        comprobar(personal.listarTrabajadores().size() == 1, "Evento rechaza duplicado");
        campo(panel, "buscar").setText("inexistente"); pulsar(boton(tabs.getComponentAt(0), "Buscar"));
        comprobar(tabla.getRowCount() == 0, "Evento Buscar filtra personal");
        Curso curso = curso("UI-001"); gestion.registrarCurso(curso);
        PnlCapacitaciones capacitaciones = new PnlCapacitaciones(gestion, personal);
        JTabbedPane tc = encontrar(capacitaciones, JTabbedPane.class); tc.setSelectedIndex(1);
        campo(capacitaciones, "fecha").setText(LocalDate.now().plusDays(3).toString());
        campo(capacitaciones, "horario").setText("09:00 - 13:00");
        pulsar(boton(tc.getComponentAt(1), "Guardar"));
        comprobar(gestion.listarCapacitaciones().size() == 1, "Evento Guardar programa capacitación");
        comprobar(encontrar(tc.getComponentAt(1), JTable.class).getRowCount() == 1, "Tabla actualizada desde programación");
        campo(capacitaciones, "fecha").setText("fecha inválida"); campo(capacitaciones, "horario").setText("09:00 - 13:00");
        pulsar(boton(tc.getComponentAt(1), "Guardar"));
        comprobar(gestion.listarCapacitaciones().size() == 1, "Evento rechaza fecha con formato inválido");
        campo(capacitaciones, "fecha").setText(LocalDate.now().plusDays(3).toString()); campo(capacitaciones, "horario").setText("sin horario");
        pulsar(boton(tc.getComponentAt(1), "Guardar"));
        comprobar(gestion.listarCapacitaciones().size() == 1, "Evento rechaza horario con formato inválido");
    }
}
