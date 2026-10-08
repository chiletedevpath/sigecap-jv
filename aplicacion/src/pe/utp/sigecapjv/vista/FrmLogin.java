package pe.utp.sigecapjv.vista;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

/** Diseño de acceso pendiente de autenticación real. */
public class FrmLogin extends JFrame {

    private static final Color AZUL = new Color(28, 48, 71);
    private static final Color FONDO = new Color(245, 247, 249);
    private static final Color TEXTO = new Color(42, 49, 57);
    private static final Color BORDE = new Color(205, 211, 218);
    private static final Color BOTON = new Color(46, 102, 158);

    private final JTextField txtUsuario = new JTextField();
    private final JPasswordField txtContrasena = new JPasswordField();

    public FrmLogin() {
        setTitle("SIGECAP J&V");
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setSize(900, 540);
        setMinimumSize(new Dimension(820, 500));
        setLocationRelativeTo(null);

        JPanel principal = new JPanel(new BorderLayout());
        principal.add(crearPanelIdentidad(), BorderLayout.WEST);
        principal.add(crearPanelLogin(), BorderLayout.CENTER);

        setContentPane(principal);
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

        JLabel lblUsuario = crearEtiqueta("Usuario");
        configurarCampo(txtUsuario);

        JLabel lblContrasena = crearEtiqueta("Contraseña");
        configurarCampo(txtContrasena);

        JButton btnIngresar = new JButton("Ingresar");
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

        fondo.add(formulario);
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
