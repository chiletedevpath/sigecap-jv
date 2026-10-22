package pe.utp.sigecapjv;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.List;
import java.util.Set;
import javax.imageio.ImageIO;
import javax.swing.*;
import pe.utp.sigecapjv.controlador.*;
import pe.utp.sigecapjv.dao.AutenticacionDAO;
import pe.utp.sigecapjv.modelo.*;
import pe.utp.sigecapjv.util.Contrasenas;
import pe.utp.sigecapjv.vista.*;

/** Comprueba la distribución Swing sin conectarse a MySQL ni mostrar ventanas. */
public final class PruebaVentanasAcceso {
    public static void main(String[] args) throws Exception {
        String hash = Contrasenas.crearHash("Clave-solo-pruebas-2026".toCharArray());
        SesionUsuario sesion = new LoginControlador(new AutenticacionDAO() {
            @Override public Cuenta buscar(String nombre) {
                return new Cuenta(1,null,"Administrador de prueba","Administrador",hash,true,Set.of());
            }
        }).autenticar("prueba","Clave-solo-pruebas-2026".toCharArray());
        UsuarioControlador datos = new UsuarioControlador(sesion) {
            @Override public boolean puedeGestionar() { return true; }
            @Override public List<Rol> roles() { return List.of(new Rol(1,"Administrador"),new Rol(2,"Trabajador")); }
            @Override public List<Usuario> listar() { return List.of(new Usuario(1,1,null,"Persona de prueba","prueba","prueba@example.test","ACTIVO")); }
            @Override public List<String[]> permisos() { return java.util.Collections.singletonList(new String[]{"Administrador","usuarios","gestionar"}); }
            @Override public RolControlador controladorRoles() {
                return new RolControlador(sesion) {
                    @Override public List<Rol> listar() { return roles(); }
                    @Override public boolean puedeEditar() { return true; }
                    @Override public Configuracion consultar(int id) {
                        return new Configuracion(new Rol(id, id == 1 ? "Administrador" : "Trabajador"),
                                List.of(new Permiso(1, "usuarios", "consultar"), new Permiso(2, "usuarios", "gestionar")),
                                id == 1 ? Set.of(1, 2) : Set.of());
                    }
                };
            }
        };
        SwingUtilities.invokeAndWait(() -> {
            FrmLogin login = new FrmLogin();
            JFrame usuarios = new JFrame("SIGECAP J&V");
            try {
                login.addNotify(); login.validate();
                verificarBotones(login.getContentPane(),login.getContentPane());
                if(args.length>0) imagen(login.getContentPane(),args[0],"login.png");
                login.setSize(820,500); login.validate(); verificarBotones(login.getContentPane(),login.getContentPane());
                PnlUsuarios panel = new PnlUsuarios(datos);
                usuarios.setContentPane(panel); usuarios.setSize(995,750); usuarios.addNotify(); usuarios.validate();
                JTabbedPane tabs = encontrar(panel,JTabbedPane.class);
                for(int i=0;i<tabs.getTabCount();i++) {
                    tabs.setSelectedIndex(i); usuarios.validate();
                    verificarBotones(panel,panel);
                    if(args.length>0) imagen(panel,args[0],"usuarios-"+i+".png");
                }
                usuarios.setSize(875,680); usuarios.validate(); verificarBotones(panel,panel);
                System.out.println("OK: distribución de login y tres pestañas de usuarios.");
            } finally { login.dispose(); usuarios.dispose(); sesion.close(); }
        });
    }
    private static void verificarBotones(Container c,Container raiz) {
        for(Component hijo:c.getComponents()) {
            if(!hijo.isVisible())continue;
            if(hijo instanceof JButton boton) {
                Rectangle r=SwingUtilities.convertRectangle(boton.getParent(),boton.getBounds(),raiz);
                if(r.width<=0 || r.height<=0 || !new Rectangle(0,0,raiz.getWidth(),raiz.getHeight()).contains(r))
                    throw new AssertionError("Botón fuera de la ventana: "+boton.getText());
            }
            if(hijo instanceof Container contenedor) verificarBotones(contenedor,raiz);
        }
    }
    private static <T> T encontrar(Component c,Class<T> tipo) {
        if(tipo.isInstance(c))return tipo.cast(c);
        if(c instanceof Container contenedor)for(Component hijo:contenedor.getComponents()) {
            T resultado=encontrar(hijo,tipo); if(resultado!=null)return resultado;
        }
        return null;
    }
    private static void imagen(Container panel,String ruta,String nombre) {
        try {
            BufferedImage imagen=new BufferedImage(panel.getWidth(),panel.getHeight(),BufferedImage.TYPE_INT_RGB);
            Graphics2D g=imagen.createGraphics(); panel.printAll(g); g.dispose();
            ImageIO.write(imagen,"png",new File(ruta,nombre));
        } catch(Exception ex) { throw new IllegalStateException(ex); }
    }
}
