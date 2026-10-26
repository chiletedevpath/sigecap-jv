package pe.utp.sigecapjv.vista;

import pe.utp.sigecapjv.controlador.*;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

public class FrmPrincipal extends JFrame {
    public FrmPrincipal() {
        this((SesionUsuario) null);
    }

    public FrmPrincipal(SesionUsuario sesion) {
        this(new PersonalJdbcControlador(java.util.Objects.requireNonNull(sesion, "Debe iniciar sesión.")), sesion);
    }

    private FrmPrincipal(PersonalJdbcControlador personal, SesionUsuario sesion) {
        this(personal, new CapacitacionJdbcControlador(personal, sesion), new SeguimientoControlador(sesion),
                new ReporteControlador(sesion), sesion);
    }

    public FrmPrincipal(PersonalControlador personal, CapacitacionControlador capacitaciones) {
        this(personal, capacitaciones, null);
    }

    public FrmPrincipal(PersonalControlador personal, CapacitacionControlador capacitaciones,
                        pe.utp.sigecapjv.controlador.SeguimientoControlador seguimiento) {
        this(personal, capacitaciones, seguimiento, (ReporteControlador) null);
    }

    // Reportes consulta MySQL directamente; sin controlador el módulo queda deshabilitado.
    public FrmPrincipal(PersonalControlador personal, CapacitacionControlador capacitaciones,
                        SeguimientoControlador seguimiento, ReporteControlador reportes) {
        this(personal, capacitaciones, seguimiento, reportes, null);
    }

    private FrmPrincipal(PersonalControlador personal, CapacitacionControlador capacitaciones,
                         SeguimientoControlador seguimiento, ReporteControlador reportes, SesionUsuario sesion) {
        super("SIGECAP J&V");
        if (sesion == null) {
            throw new SecurityException("Debe iniciar sesión.");
        }
        PnlPersonal pnlPersonal = sesion.puede("personal", "consultar") ? new PnlPersonal(
                personal, sesion.getRol()) : null;
        PnlCapacitaciones pnlCapacitaciones = sesion.puede("capacitaciones", "consultar") ? new PnlCapacitaciones(
                capacitaciones, personal, seguimiento, sesion.getRol()) : null;
        CardLayout tarjetas = new CardLayout();
        JPanel contenido = new JPanel(tarjetas);
        PnlInicio.Perfil perfilInicio = PnlInicio.Perfil.ADMINISTRADOR;

        if (sesion != null) {
            perfilInicio = switch (sesion.getRol()) {
                case "Administrador" -> PnlInicio.Perfil.ADMINISTRADOR;
                case "Responsable de capacitaciones" -> PnlInicio.Perfil.RESPONSABLE_CAPACITACIONES;
                case "Supervisor de Operaciones" -> PnlInicio.Perfil.SUPERVISOR_OPERACIONES;
                case "Trabajador" -> PnlInicio.Perfil.TRABAJADOR;
                default -> throw new IllegalStateException("Rol no reconocido: " + sesion.getRol());
            };
        }

        PnlInicio pnlInicio = new PnlInicio(perfilInicio);
        contenido.add(pnlInicio, "Inicio");

        if (perfilInicio == PnlInicio.Perfil.ADMINISTRADOR) {
            InicioControlador inicioControlador = new InicioControlador(sesion);
            pnlInicio.cargarResumen(inicioControlador);
        }
        if (pnlPersonal != null) contenido.add(pnlPersonal, "Personal");
        if (pnlCapacitaciones != null) contenido.add(pnlCapacitaciones, "Capacitaciones");
        PnlUsuarios usuarios = "Administrador".equals(sesion.getRol()) && sesion.puede(
                "usuarios", "consultar") ? new PnlUsuarios(new UsuarioControlador(sesion)) : null;
        if (usuarios != null) contenido.add(usuarios, "Usuarios");

        PnlReportes reportesPanel = reportes != null && sesion.puede(
                "capacitaciones", "consultar") ? new PnlReportes(reportes) : null;
        if (reportesPanel != null) contenido.add(reportesPanel, "Reportes");
        if (sesion != null) {
            if (pnlPersonal != null && !sesion.puede("personal", "gestionar")) soloConsulta(pnlPersonal);
            if (pnlCapacitaciones != null && !sesion.puede("capacitaciones", "gestionar"))
                soloConsulta(pnlCapacitaciones);
        }
        PnlAlertas alertas = sesion.puede("capacitaciones", "consultar")
                ? new PnlAlertas(new NotificacionesControlador(sesion)) : null;
        if (alertas != null) contenido.add(alertas, "Alertas");
        JPanel menu = new JPanel();
        menu.setLayout(new BoxLayout(menu, BoxLayout.Y_AXIS));
        Color azul = new Color(28, 48, 71), seleccionado = new Color(46, 102, 158);
        menu.setBackground(azul);
        menu.setPreferredSize(new Dimension(225, 720));
        menu.setBorder(new EmptyBorder(28, 18, 22, 18));
        JLabel marca = new JLabel("SIGECAP J&V");
        marca.setForeground(Color.WHITE);
        marca.setFont(new Font("Segoe UI", Font.BOLD, 22));
        marca.setAlignmentX(Component.LEFT_ALIGNMENT);
        menu.add(marca);
        menu.add(Box.createVerticalStrut(5));
        JLabel area = new JLabel("Área de Operaciones");
        area.setForeground(new Color(182, 198, 214));
        area.setAlignmentX(Component.LEFT_ALIGNMENT);
        menu.add(area);
        menu.add(Box.createVerticalStrut(30));
        for (String nombre : new String[]{"Inicio", "Personal", "Capacitaciones", "Seguimiento", "Reportes",
                "Usuarios", "Alertas"}) {
            if (nombre.equals("Seguimiento")) {
                continue;
            }

            if (nombre.equals("Personal") && pnlPersonal == null) {
                continue;
            }

            if (nombre.equals("Capacitaciones") && pnlCapacitaciones == null) {
                continue;
            }

            if (nombre.equals("Usuarios") && usuarios == null) {
                continue;
            }

            if (nombre.equals("Alertas") && alertas == null) continue;
            JButton boton = new JButton(nombre);

            boton.setAlignmentX(Component.LEFT_ALIGNMENT);
            boton.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));
            boton.setHorizontalAlignment(SwingConstants.LEFT);
            boton.setBackground(nombre.equals("Inicio") ? seleccionado : azul);
            boton.setForeground(Color.WHITE);
            boton.setFont(new Font("Segoe UI", Font.PLAIN, 14));
            boton.setFocusPainted(false);
            boton.setBorder(new EmptyBorder(0, 12, 0, 10));
            boolean disponible = nombre.equals("Inicio")
                    || nombre.equals("Personal") && pnlPersonal != null
                    || nombre.equals("Capacitaciones") && pnlCapacitaciones != null
                    || nombre.equals("Usuarios") && usuarios != null
                    || nombre.equals("Reportes") && reportesPanel != null
                    || nombre.equals("Alertas") && alertas != null;

            boton.setEnabled(disponible);

            if (!disponible) {
                boton.setToolTipText(
                        java.util.Set.of("Personal", "Capacitaciones", "Usuarios", "Reportes").contains(nombre)
                                ? "Sin acceso para su rol"
                                : "Funcionalidad pendiente de implementación");
            }
            boton.addActionListener(e -> {
                if (nombre.equals("Personal")) pnlPersonal.refrescar();
                if (nombre.equals("Capacitaciones")) pnlCapacitaciones.refrescar();
                if (nombre.equals("Usuarios")) usuarios.refrescar();
                if (nombre.equals("Reportes")) reportesPanel.refrescar();
                if (nombre.equals("Alertas")) alertas.refrescar();
                tarjetas.show(contenido, nombre);
                for (Component c : menu.getComponents())
                    if (c instanceof JButton b) b.setBackground(b == boton ? seleccionado : azul);
            });
            menu.add(boton);
            menu.add(Box.createVerticalStrut(6));
        }
        menu.add(Box.createVerticalGlue());
        JButton salir = new JButton("Cerrar sesión");
        salir.setForeground(Color.WHITE);
        salir.setBackground(azul);
        salir.setFocusPainted(false);
        salir.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        salir.setBorder(new EmptyBorder(10, 12, 10, 12));
        salir.addActionListener(e -> {
            if (sesion != null) sesion.close();
            dispose();
            if (sesion != null) new FrmLogin().setVisible(true);
        });
        salir.setAlignmentX(Component.LEFT_ALIGNMENT);
        menu.add(salir);
        JPanel principal = new JPanel(new BorderLayout());
        principal.add(menu, BorderLayout.WEST);
        principal.add(contenido, BorderLayout.CENTER);

        if (sesion != null) {
            VigilanciaSesion vigilancia = new VigilanciaSesion(
                    sesion, new SesionControlador(), mensaje -> {
                if (!isDisplayable()) return;
                dispose();
                FrmLogin acceso = new FrmLogin();
                acceso.setVisible(true);
                JOptionPane.showMessageDialog(acceso, mensaje, "Sesión finalizada", JOptionPane.INFORMATION_MESSAGE);
            });
            addWindowListener(new java.awt.event.WindowAdapter() {
                @Override
                public void windowOpened(java.awt.event.WindowEvent e) {
                    vigilancia.iniciar();
                }

                @Override
                public void windowClosed(java.awt.event.WindowEvent e) {
                    vigilancia.close();
                    sesion.close();
                }
            });
            addWindowFocusListener(new java.awt.event.WindowAdapter() {
                @Override
                public void windowGainedFocus(java.awt.event.WindowEvent e) {
                    vigilancia.revisar();
                }
            });
        }
        setContentPane(principal);
        setSize(1220, 750);
        setMinimumSize(new Dimension(1100, 680));
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null);
    }

    private static void soloConsulta(Container contenedor) {
        for (Component componente : contenedor.getComponents()) {
            if (componente instanceof JButton boton && !java.util.Set.of(
                    "Buscar", "Actualizar listado", "Limpiar", "Consultar").contains(boton.getText()))
                boton.setEnabled(false);
            if (componente instanceof Container hijo) soloConsulta(hijo);
        }
    }

    public static void main(String[] args) {
        // Los errores de lectura también pueden ocurrir al cambiar de pestaña o consultar.
        Toolkit.getDefaultToolkit().getSystemEventQueue().push(new EventQueue() {
            @Override
            protected void dispatchEvent(AWTEvent evento) {
                try {
                    super.dispatchEvent(evento);
                } catch (SecurityException ex) {
                    JOptionPane.showMessageDialog(null, ex.getMessage(), "Acceso", JOptionPane.WARNING_MESSAGE);
                } catch (IllegalStateException ex) {
                    if (!(ex.getCause() instanceof java.sql.SQLException)) throw ex;
                    // Registrar códigos de diagnóstico sin publicar SQL ni datos de los registros.
                    java.sql.SQLException sql = (java.sql.SQLException) ex.getCause();
                    System.err.printf("MySQL: SQLState=%s, codigo=%d%n", sql.getSQLState(), sql.getErrorCode());
                    JOptionPane.showMessageDialog(null, ex.getMessage(), "MySQL", JOptionPane.ERROR_MESSAGE);
                }
            }
        });
        SwingUtilities.invokeLater(() -> new FrmLogin().setVisible(true));
    }
}
