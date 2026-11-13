package pe.utp.sigecapjv;

import pe.utp.sigecapjv.controlador.*;
import pe.utp.sigecapjv.vista.*;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.nio.file.*;
import java.util.List;
import java.util.ArrayList;
import javax.imageio.ImageIO;

/** Recorre las ventanas con cuentas de la base temporal y guarda renders para revisión. */
public final class PruebaNavegacionMySql {
    private static int total;
    private static void comprobar(boolean valor,String mensaje) {
        if(!valor) throw new AssertionError(mensaje);
        total++;
    }
    private static <T> List<T> buscar(Component raiz,Class<T> tipo) {
        List<T> lista=new ArrayList<>();
        if(tipo.isInstance(raiz)) lista.add(tipo.cast(raiz));
        if(raiz instanceof Container padre) for(var hijo:padre.getComponents()) lista.addAll(buscar(hijo,tipo));
        return lista;
    }
    private static void esperar(JFrame ventana) throws Exception {
        long limite=System.nanoTime()+java.util.concurrent.TimeUnit.SECONDS.toNanos(20);
        while(System.nanoTime()<limite) {
            boolean[] cargando={true};
            SwingUtilities.invokeAndWait(() -> cargando[0]=buscar(ventana,JLabel.class).stream()
                    .anyMatch(l->l.isShowing() && l.getText()!=null && (l.getText().startsWith("Cargando") || l.getText().startsWith("Consultando"))));
            if(!cargando[0]) return;
            Thread.sleep(30);
        }
        throw new AssertionError("La pantalla no terminó de cargar");
    }
    private static FrmPrincipal ingresar(String usuario,String clave) throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            FrmLogin login=new FrmLogin();
            login.setVisible(true);
            buscar(login,JTextField.class).stream().filter(c -> !(c instanceof JPasswordField))
                    .findFirst().orElseThrow().setText(usuario);
            buscar(login,JPasswordField.class).get(0).setText(clave);
            login.getRootPane().getDefaultButton().doClick();
        });
        long limite=System.nanoTime()+java.util.concurrent.TimeUnit.SECONDS.toNanos(20);
        while(System.nanoTime()<limite) {
            FrmPrincipal[] principal={null};
            SwingUtilities.invokeAndWait(() -> {
                for(Window w:Window.getWindows())
                    if(w instanceof FrmPrincipal f && f.isShowing()) principal[0]=f;
            });
            if(principal[0]!=null) return principal[0];
            Thread.sleep(30);
        }
        throw new AssertionError("El login no abrió el panel para "+usuario);
    }

    public static void main(String[] args) throws Exception {
        if(!"true".equals(System.getenv("SIGECAP_DB_TEST")) || GraphicsEnvironment.isHeadless())
            throw new IllegalStateException("Requiere escritorio y la base temporal de pruebas.");
        Path salida=Path.of(System.getProperty("sigecap.renders"));
        Files.createDirectories(salida);
        try {
            for(String usuario:new String[]{"admin_prueba","responsable_prueba","supervisor_prueba","trabajador_prueba"}) {
                String clave=usuario.equals("supervisor_prueba")?"Clave-renovada-prueba-2026":"Clave-usuarios-prueba-2026";
                {
                    JFrame[] ventana={ingresar(usuario,clave)};
                    esperar(ventana[0]);
                    List<String> esperados=usuario.equals("trabajador_prueba")
                            ? List.of("Inicio","Alertas","Mis capacitaciones","Cambiar contraseña","Cerrar sesión")
                            : usuario.equals("admin_prueba")
                            ? List.of("Inicio","Personal","Capacitaciones","Seguimiento","Reportes","Usuarios","Alertas","Cambiar contraseña","Cerrar sesión")
                            : List.of("Inicio","Personal","Capacitaciones","Seguimiento","Reportes","Alertas","Cambiar contraseña","Cerrar sesión");
                    JPanel menu=(JPanel)((BorderLayout)ventana[0].getContentPane().getLayout()).getLayoutComponent(BorderLayout.WEST);
                    SwingUtilities.invokeAndWait(() -> {
                        comprobar(buscar(menu,JButton.class).stream().map(JButton::getText).toList().equals(esperados),"Menú exacto de "+usuario);
                        comprobar(buscar(menu,JButton.class).stream().allMatch(JButton::isEnabled),"Módulos disponibles de "+usuario);
                    });
                    for(String modulo:esperados) {
                        SwingUtilities.invokeAndWait(() -> buscar(menu,JButton.class).stream().filter(b->modulo.equals(b.getText())).findFirst().orElseThrow().doClick());
                        if(modulo.equals("Cerrar sesión")) break;
                        esperar(ventana[0]);
                        SwingUtilities.invokeAndWait(() -> {
                            comprobar(buscar(ventana[0],JLabel.class).stream().filter(Component::isShowing)
                                    .noneMatch(l->l.getText()!=null && (l.getText().startsWith("No se pudo") || l.getText().startsWith("No se pudieron"))),"Carga sin errores: "+usuario+" / "+modulo);
                            BufferedImage render=new BufferedImage(ventana[0].getWidth(),ventana[0].getHeight(),BufferedImage.TYPE_INT_RGB);
                            Graphics2D g=render.createGraphics(); ventana[0].printAll(g); g.dispose();
                            try { ImageIO.write(render,"png",salida.resolve(usuario+"-"+modulo.replace(' ','-')+".png").toFile()); }
                            catch(java.io.IOException ex) { throw new java.io.UncheckedIOException(ex); }
                        });
                    }
                    SwingUtilities.invokeAndWait(() -> {
                        comprobar(!ventana[0].isDisplayable(),"Cerrar sesión cierra la ventana");
                        comprobar(java.util.Arrays.stream(Window.getWindows()).anyMatch(w->w instanceof FrmLogin && w.isShowing()),"Vuelve al login");
                        for(Window w:Window.getWindows()) w.dispose();
                    });
                }
            }
            System.out.println("OK: "+total+" comprobaciones de navegación de los cuatro perfiles.");
        } finally { SwingUtilities.invokeAndWait(() -> { for(Window w:Window.getWindows()) w.dispose(); }); }
    }
}
