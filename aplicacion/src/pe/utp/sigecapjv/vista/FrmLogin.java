package pe.utp.sigecapjv.vista;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import pe.utp.sigecapjv.controlador.*;

/**
 * Acceso a las ventanas mediante una cuenta activa de MySQL.
 */
public class FrmLogin extends JFrame {

    private static final Color AZUL = new Color(28, 48, 71);
    private static final Color FONDO = new Color(245, 247, 249);
    private static final Color TEXTO = new Color(42, 49, 57);
    private static final Color BORDE = new Color(205, 211, 218);
    private static final Color BOTON = new Color(46, 102, 158);

    private final JTextField txtUsuario = new JTextField();
    private final JPasswordField txtContrasena = new JPasswordField();
    private final JButton inicial = new JButton("Crear primer administrador");

    public FrmLogin() {
        setTitle("SIGECAP J&V");
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setSize(900, 540);
        setMinimumSize(new Dimension(820, 500));
        setLocationRelativeTo(null);

        JPanel principal = new JPanel(new BorderLayout());
        principal.add(crearPanelIdentidad(), BorderLayout.WEST);
        principal.add(crearPanelLogin(), BorderLayout.CENTER);

        ampliarEtiquetas(principal);
        setContentPane(principal);
        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override public void windowOpened(java.awt.event.WindowEvent e) { verificarConfiguracionInicial(); }
        });
    }

    private void verificarConfiguracionInicial() {
        inicial.setVisible(false);
        new SwingWorker<Boolean,Void>() {
            @Override protected Boolean doInBackground() {
                return requiereAdministradorInicial();
            }
            @Override protected void done() {
                if (!isDisplayable()) return;
                try { inicial.setVisible(get()); }
                catch (Exception ex) {
                    inicial.setVisible(false);
                    JOptionPane.showMessageDialog(FrmLogin.this,
                            "No se pudo verificar la conexión. Revise MySQL y vuelva a abrir el login.",
                            "Conexión", JOptionPane.WARNING_MESSAGE);
                }
                getContentPane().revalidate(); getContentPane().repaint();
            }
        }.execute();
    }

    protected boolean requiereAdministradorInicial() {
        return UsuarioControlador.requiereAdministradorInicial();
    }

    private static void ampliarEtiquetas(Container contenedor) {
        for (Component c : contenedor.getComponents()) {
            if (c instanceof JLabel etiqueta) {
                Dimension d=etiqueta.getPreferredSize();
                etiqueta.setPreferredSize(new Dimension(d.width+16,d.height));
                etiqueta.setMaximumSize(new Dimension(Integer.MAX_VALUE,d.height));
            }
            if (c instanceof Container hijo) ampliarEtiquetas(hijo);
        }
    }
    private void ingresar(JButton boton) {
        char[] clave = txtContrasena.getPassword();
        String usuario = txtUsuario.getText();
        boton.setEnabled(false);
        new SwingWorker<SesionUsuario, Void>() {
            @Override protected SesionUsuario doInBackground() {
                try { return new LoginControlador().autenticar(usuario,clave); }
                finally { java.util.Arrays.fill(clave,'\0'); }
            }
            @Override protected void done() {
                boton.setEnabled(true); txtContrasena.setText("");
                SesionUsuario sesion = null;
                try {
                    sesion = get();
                    if (!isDisplayable()) { sesion.close(); return; }
                    new FrmPrincipal(sesion).setVisible(true); dispose();
                } catch (Exception ex) {
                    if (sesion != null) sesion.close();
                    mostrarError(ex);
                }
            }
        }.execute();
    }

    private void prepararAdministrador(JButton boton) {
        JTextField nombre = new JTextField(), usuario = new JTextField(), correo = new JTextField();
        JPasswordField clave = new JPasswordField(), confirmar = new JPasswordField();
        Object[] campos = {"Solo para una base sin usuarios.","Nombre completo",nombre,"Usuario",usuario,
                "Correo de contacto (no se usa para ingresar)",correo,"Contraseña (12 a 128 caracteres)",clave,"Confirmar contraseña",confirmar};
        if (JOptionPane.showConfirmDialog(this,campos,"Primer administrador",JOptionPane.OK_CANCEL_OPTION) != JOptionPane.OK_OPTION) return;
        char[] valor = clave.getPassword(), repetida = confirmar.getPassword();
        boolean iguales = java.util.Arrays.equals(valor,repetida);
        java.util.Arrays.fill(repetida,'\0'); clave.setText(""); confirmar.setText("");
        if (!iguales) { java.util.Arrays.fill(valor,'\0'); JOptionPane.showMessageDialog(this,"Las contraseñas no coinciden."); return; }
        String n=nombre.getText(), u=usuario.getText(), c=correo.getText();
        boton.setEnabled(false);
        new SwingWorker<Void,Void>() {
            @Override protected Void doInBackground() {
                try { UsuarioControlador.crearAdministradorInicial(n,u,c,valor); return null; }
                finally { java.util.Arrays.fill(valor,'\0'); }
            }
            @Override protected void done() {
                boton.setEnabled(true);
                try {
                    get(); inicial.setVisible(false); txtUsuario.setText(u);
                    JOptionPane.showMessageDialog(FrmLogin.this,"Administrador creado. Ingrese con su nombre de usuario y contraseña.");
                    txtContrasena.requestFocusInWindow();
                }
                catch (Exception ex) { mostrarError(ex); }
                finally { verificarConfiguracionInicial(); }
            }
        }.execute();
    }
    private void mostrarError(Exception ex) {
        Throwable causa = ex instanceof java.util.concurrent.ExecutionException ? ex.getCause() : ex;
        JOptionPane.showMessageDialog(this, causa instanceof IllegalArgumentException || causa instanceof IllegalStateException || causa instanceof SecurityException
                ? causa.getMessage() : "No se pudo completar el acceso.","Acceso",JOptionPane.WARNING_MESSAGE);
    }
    private JPanel crearPanelIdentidad() {
        JPanel panel = new JPanel();
        panel.setPreferredSize(new Dimension(330, 540));
        panel.setBackground(AZUL);
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(new EmptyBorder(58, 38, 40, 38));

        JLabel nombre = new JLabel("SIGECAP J&V");
        nombre.setForeground(Color.WHITE);
        nombre.setFont(new Font("Segoe UI", Font.BOLD, 30));
        nombre.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel sistema = new JLabel("<html>Sistema de gestión de<br>cursos de capacitación</html>");
        sistema.setForeground(new Color(221, 229, 237));
        sistema.setFont(new Font("Segoe UI", Font.PLAIN, 15));
        sistema.setAlignmentX(Component.LEFT_ALIGNMENT);

        JSeparator separador = new JSeparator();
        separador.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
        separador.setForeground(new Color(91, 111, 132));
        separador.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel empresa = new JLabel("J&V Resguardo");
        empresa.setForeground(new Color(221, 229, 237));
        empresa.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        empresa.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel area = new JLabel("Área de Operaciones");
        area.setForeground(new Color(174, 190, 206));
        area.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        area.setAlignmentX(Component.LEFT_ALIGNMENT);

        panel.add(nombre);
        panel.add(Box.createVerticalStrut(10));
        panel.add(sistema);
        panel.add(Box.createVerticalStrut(26));
        panel.add(separador);
        panel.add(Box.createVerticalStrut(24));
        panel.add(empresa);
        panel.add(Box.createVerticalStrut(6));
        panel.add(area);
        panel.add(Box.createVerticalGlue());

        return panel;
    }

    private JPanel crearPanelLogin() {
        JPanel fondo = new JPanel(new GridBagLayout());
        fondo.setBackground(FONDO);
        fondo.setBorder(new EmptyBorder(35, 45, 35, 45));

        JPanel formulario = new JPanel();
        formulario.setBackground(Color.WHITE);
        formulario.setLayout(new BoxLayout(formulario, BoxLayout.Y_AXIS));
        formulario.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(226, 230, 234), 1),
                new EmptyBorder(38, 42, 36, 42)
        ));
        formulario.setPreferredSize(new Dimension(390, 360));

        JLabel titulo = new JLabel("Inicio de sesión");
        titulo.setForeground(TEXTO);
        titulo.setFont(new Font("Segoe UI", Font.BOLD, 24));
        titulo.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel lblUsuario = crearEtiqueta("Nombre de usuario");
        txtUsuario.setToolTipText("Ingrese el nombre de usuario registrado, no el correo de contacto.");
        configurarCampo(txtUsuario);

        JLabel lblContrasena = crearEtiqueta("Contraseña");
        configurarCampo(txtContrasena);

        JButton btnIngresar = new JButton("Ingresar");
        btnIngresar.addActionListener(e -> ingresar(btnIngresar));
        getRootPane().setDefaultButton(btnIngresar);
        btnIngresar.setAlignmentX(Component.LEFT_ALIGNMENT);
        btnIngresar.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
        btnIngresar.setPreferredSize(new Dimension(300, 44));
        btnIngresar.setBackground(BOTON);
        btnIngresar.setForeground(Color.WHITE);
        btnIngresar.setFont(new Font("Segoe UI", Font.BOLD, 14));
        btnIngresar.setFocusPainted(false);
        btnIngresar.setBorderPainted(false);
        btnIngresar.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        JButton btnSalir = new JButton("Salir");
        btnSalir.setAlignmentX(Component.LEFT_ALIGNMENT);
        btnSalir.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        btnSalir.setBackground(Color.WHITE);
        btnSalir.setForeground(TEXTO);
        btnSalir.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        btnSalir.setFocusPainted(false);
        btnSalir.setBorder(BorderFactory.createLineBorder(BORDE));
        btnSalir.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnSalir.addActionListener(e -> dispose());

        formulario.add(titulo);
        formulario.add(Box.createVerticalStrut(30));
        formulario.add(lblUsuario);
        formulario.add(Box.createVerticalStrut(8));
        formulario.add(txtUsuario);
        formulario.add(Box.createVerticalStrut(20));
        formulario.add(lblContrasena);
        formulario.add(Box.createVerticalStrut(8));
        formulario.add(txtContrasena);
        formulario.add(Box.createVerticalStrut(28));
        formulario.add(btnIngresar);
        formulario.add(Box.createVerticalStrut(12));
        formulario.add(btnSalir);
        inicial.setVisible(false);
        inicial.setToolTipText("Disponible únicamente si aún no existen usuarios en la base de datos.");
        inicial.addActionListener(e -> prepararAdministrador(inicial));


        GridBagConstraints posicion = new GridBagConstraints();
        posicion.gridx = 0; posicion.gridy = 0;
        fondo.add(formulario, posicion);
        posicion.gridy = 1; posicion.insets = new Insets(8,0,0,0);
        fondo.add(inicial, posicion);
        return fondo;
    }

    private JLabel crearEtiqueta(String texto) {
        JLabel label = new JLabel(texto);
        label.setForeground(TEXTO);
        label.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        return label;
    }

    private void configurarCampo(JTextField campo) {
        campo.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));
        campo.setPreferredSize(new Dimension(300, 42));
        campo.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        campo.setForeground(TEXTO);
        campo.setBackground(Color.WHITE);
        campo.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDE),
                new EmptyBorder(0, 10, 0, 10)
        ));
        campo.setAlignmentX(Component.LEFT_ALIGNMENT);
    }
}
