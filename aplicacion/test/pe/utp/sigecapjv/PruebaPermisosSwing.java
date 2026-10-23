package pe.utp.sigecapjv;

import java.awt.*;
import java.util.List;
import java.util.Set;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import javax.swing.*;
import pe.utp.sigecapjv.controlador.*;
import pe.utp.sigecapjv.dao.AutenticacionDAO;
import pe.utp.sigecapjv.modelo.*;
import pe.utp.sigecapjv.util.Contrasenas;
import pe.utp.sigecapjv.vista.*;

public final class PruebaPermisosSwing {
    private static int total;
    private static void comprobar(boolean valor, String mensaje) {
        if (!valor) throw new AssertionError(mensaje);
        total++;
    }

    public static void verificar() throws Exception {
        char[] clave = "Clave-solo-pruebas-2026".toCharArray();
        String hash = Contrasenas.crearHash(clave);
        LoginControlador login = new LoginControlador(new AutenticacionDAO() {
            @Override public Cuenta buscar(String usuario) {
                return new Cuenta(1,null,"Prueba","Administrador",hash,true,Set.of());
            }
        });
        try (SesionUsuario sesion = login.autenticar("prueba", clave)) {
            AtomicReference<Set<Integer>> guardados = new AtomicReference<>(Set.of(1));
            AtomicBoolean obsoleto = new AtomicBoolean();
            RolControlador controlador = new RolControlador(sesion) {
                @Override public boolean puedeEditar() { return true; }
                @Override public List<Rol> listar() {
                    return List.of(new Rol(2,"Supervisor de Operaciones"),new Rol(1,"Administrador"),new Rol(3,"Trabajador"));
                }
                @Override public Configuracion consultar(int rol) {
                    return new Configuracion(listar().stream().filter(r -> r.idRol()==rol).findFirst().orElseThrow(),
                            List.of(new Permiso(1,"personal","consultar"),new Permiso(2,"personal","gestionar"),
                                    new Permiso(3,"usuarios","consultar"),new Permiso(4,"usuarios","gestionar")),
                            rol == 2 ? guardados.get() : rol == 1 ? Set.of(1,2,3,4) : Set.of());
                }
                @Override public void guardar(int rol, Set<Integer> esperados, Set<Integer> elegidos) {
                    if (SwingUtilities.isEventDispatchThread()) throw new AssertionError("Guardado en EDT");
                    if (obsoleto.get() || !guardados.get().equals(esperados)) throw new IllegalStateException("Otro administrador cambió estos permisos. Recargue el rol antes de guardar.");
                    guardados.set(Set.copyOf(elegidos));
                }
            };
            AtomicReference<PnlPermisos> referencia = new AtomicReference<>();
            SwingUtilities.invokeAndWait(() -> {
                PnlPermisos panel = new PnlPermisos(controlador); referencia.set(panel);
                JTable tabla = encontrar(panel,JTable.class);
                comprobar(tabla.getRowCount()==4 && Boolean.TRUE.equals(tabla.getValueAt(0,0)), "No se cargó el catálogo.");
                tabla.setValueAt(true,1,0); boton(panel,"Guardar permisos").doClick();
                comprobar(!encontrar(panel,JComboBox.class).isEnabled(), "Se puede cambiar de rol durante el guardado.");
            });
            esperarGuardado(referencia.get());
            comprobar(guardados.get().equals(Set.of(1,2)), "No se guardó la selección de la tabla.");
            obsoleto.set(true);
            SwingUtilities.invokeAndWait(() -> {
                encontrar(referencia.get(),JTable.class).setValueAt(false,1,0);
                boton(referencia.get(),"Guardar permisos").doClick();
            });
            esperarGuardado(referencia.get());
            SwingUtilities.invokeAndWait(() -> {
                comprobar(contieneTexto(referencia.get(),"Otro administrador"), "No se mostró el conflicto.");
                boton(referencia.get(),"Recargar").doClick();
                JTable tabla = encontrar(referencia.get(),JTable.class);
                comprobar(Boolean.TRUE.equals(tabla.getValueAt(1,0)), "Recargar perdió los permisos vigentes.");
                JComboBox<?> roles = encontrar(referencia.get(),JComboBox.class);
                roles.setSelectedIndex(1);
                comprobar(!tabla.isCellEditable(2,0) && !tabla.isCellEditable(3,0), "El acceso administrativo se puede quitar desde la tabla.");
                roles.setSelectedIndex(2);
                comprobar(!tabla.isCellEditable(0,0) && !boton(referencia.get(),"Guardar permisos").isEnabled(), "Trabajador puede recibir permisos globales.");
            });

            CountDownLatch expulsada = new CountDownLatch(1);
            AtomicBoolean fueraDelEdt = new AtomicBoolean(), avisoEnEdt = new AtomicBoolean();
            AtomicReference<VigilanciaSesion> vigilancia = new AtomicReference<>();
            SwingUtilities.invokeAndWait(() -> {
                VigilanciaSesion observador = new VigilanciaSesion(sesion,new SesionControlador() {
                    @Override public boolean vigente(SesionUsuario actual) {
                        fueraDelEdt.set(!SwingUtilities.isEventDispatchThread()); return false;
                    }
                }, mensaje -> { avisoEnEdt.set(SwingUtilities.isEventDispatchThread()); expulsada.countDown(); });
                vigilancia.set(observador); observador.iniciar();
            });
            comprobar(expulsada.await(5,TimeUnit.SECONDS), "La ventana no recibió el cierre de sesión.");
            comprobar(!sesion.estaActiva() && fueraDelEdt.get() && avisoEnEdt.get(), "La revisión bloquea el EDT o no cierra la sesión.");
            SwingUtilities.invokeAndWait(() -> vigilancia.get().close());
        }
        try (SesionUsuario sesion = login.autenticar("prueba", clave)) {
            CountDownLatch fallo = new CountDownLatch(1);
            SwingUtilities.invokeAndWait(() -> new VigilanciaSesion(sesion,new SesionControlador() {
                @Override public boolean vigente(SesionUsuario actual) { throw new IllegalStateException("dato interno que no debe mostrarse"); }
            }, mensaje -> {
                comprobar(!mensaje.contains("dato interno"), "Se mostró información interna."); fallo.countDown();
            }).iniciar());
            comprobar(fallo.await(5,TimeUnit.SECONDS) && !sesion.estaActiva(), "Se mantuvo la sesión sin poder verificarla.");
        } finally { java.util.Arrays.fill(clave,'\0'); }
        System.out.println("OK: " + total + " comprobaciones Swing de permisos y vigilancia de sesión.");
    }

    private static void esperarGuardado(PnlPermisos panel) throws Exception {
        long fin = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
        AtomicBoolean listo = new AtomicBoolean();
        while (System.nanoTime() < fin) {
            SwingUtilities.invokeAndWait(() -> listo.set(boton(panel,"Guardar permisos").isEnabled()));
            if (listo.get()) return;
            Thread.sleep(20);
        }
        throw new AssertionError("El guardado no terminó.");
    }
    private static JButton boton(Container c,String nombre) {
        for(Component hijo:c.getComponents()) {
            if(hijo instanceof JButton b && nombre.equals(b.getText()))return b;
            if(hijo instanceof Container contenedor) { JButton b=boton(contenedor,nombre); if(b!=null)return b; }
        }
        return null;
    }
    private static boolean contieneTexto(Container c,String texto) {
        for(Component hijo:c.getComponents()) {
            if(hijo instanceof JLabel l && l.getText().contains(texto))return true;
            if(hijo instanceof Container contenedor && contieneTexto(contenedor,texto))return true;
        }
        return false;
    }
    private static <T> T encontrar(Component c,Class<T> tipo) {
        if(tipo.isInstance(c))return tipo.cast(c);
        if(c instanceof Container contenedor)for(Component hijo:contenedor.getComponents()) {
            T resultado=encontrar(hijo,tipo); if(resultado!=null)return resultado;
        }
        return null;
    }
}
