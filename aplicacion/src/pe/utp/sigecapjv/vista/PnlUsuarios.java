
package pe.utp.sigecapjv.vista;

import pe.utp.sigecapjv.controlador.UsuarioControlador;
import pe.utp.sigecapjv.modelo.Rol;
import pe.utp.sigecapjv.modelo.Trabajador;
import pe.utp.sigecapjv.modelo.Usuario;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.ExecutionException;

public class PnlUsuarios extends JPanel {

    private static final Color AZUL_CLARO = new Color(46, 102, 158);
    private static final Color FONDO = new Color(245, 247, 249);
    private static final Color BLANCO = Color.WHITE;
    private static final Color TEXTO = new Color(42, 49, 57);
    private static final Color TEXTO_SUAVE = new Color(104, 116, 130);
    private static final Color BORDE = new Color(224, 229, 234);

    private final UsuarioControlador controlador;
    private List<Usuario> usuarios = List.of();

    private final DefaultTableModel listado = new DefaultTableModel(
            new String[]{"Usuario", "Nombre", "Rol", "Estado"}, 0) {

        @Override
        public boolean isCellEditable(int fila, int columna) {
            return false;
        }
    };

    private final JTable tabla = new JTable(listado);

    private final JTextField buscar = new JTextField(28);
    private final JTextField login = new JTextField(18);
    private final JTextField nombre = new JTextField(18);
    private final JTextField correo = new JTextField(18);

    private final JComboBox<Trabajador> trabajador = new JComboBox<>();
    private final JComboBox<Rol> roles = new JComboBox<>();

    private final JComboBox<String> estado =
            new JComboBox<>(new String[]{"ACTIVO", "INACTIVO"});

    private final JPasswordField clave = new JPasswordField(18);
    private final JTabbedPane tabs = new JTabbedPane();

    private JLabel etiquetaTrabajador;

    private final JLabel ayudaTrabajador =
            new JLabel("Seleccione una persona registrada en Personal.");

    private int editando;

    public PnlUsuarios(UsuarioControlador controlador) {
        this.controlador = Objects.requireNonNull(controlador);

        for (Rol rol : controlador.roles()) {
            roles.addItem(rol);
        }

        configurarSelectorTrabajadores();
        limpiar();

        setLayout(new BorderLayout());
        add(crearContenido(), BorderLayout.CENTER);

        ampliarEtiquetas(this);
        refrescar();
    }

    public void refrescar() {
        usuarios = controlador.listar();
        listado.setRowCount(0);

        String filtro = buscar.getText()
                .strip()
                .toLowerCase(Locale.ROOT);

        for (Usuario u : usuarios) {
            String contenido = (u.nombreUsuario() + " " +
                    u.nombreCompleto()).toLowerCase(Locale.ROOT);

            if (contenido.contains(filtro)) {
                listado.addRow(new Object[]{
                        u.nombreUsuario(),
                        u.nombreCompleto(),
                        nombreRol(u.idRol()),
                        u.estado()
                });
            }
        }
    }

    private String nombreRol(int id) {
        for (int i = 0; i < roles.getItemCount(); i++) {
            Rol rol = roles.getItemAt(i);

            if (rol.idRol() == id) {
                return rol.nombre();
            }
        }

        return "Rol " + id;
    }

    private boolean esRolTrabajador() {
        Rol rol = (Rol) roles.getSelectedItem();

        return rol != null &&
                "Trabajador".equals(rol.nombre());
    }

    private void configurarSelectorTrabajadores() {
        trabajador.setMaximumRowCount(12);

        trabajador.setRenderer(new DefaultListCellRenderer() {

            @Override
            public Component getListCellRendererComponent(
                    JList<?> lista,
                    Object valor,
                    int indice,
                    boolean seleccionado,
                    boolean enfocado) {

                super.getListCellRendererComponent(
                        lista, valor, indice,
                        seleccionado, enfocado);

                if (valor instanceof Trabajador t) {
                    setText(
                            t.getDni() + " - " +
                                    t.getApellidos() + ", " +
                                    t.getNombres() +
                                    ("ACTIVO".equals(t.getEstado())
                                            ? ""
                                            : " (INACTIVO)")
                    );
                } else {
                    setText(esRolTrabajador()
                                    ? "Seleccione un trabajador"
                                    : "Sin vincular");
                }

                return this;
            }
        });

        roles.addActionListener(e ->
                                        actualizarEtiquetaTrabajador());

        trabajador.addActionListener(e -> {
            Trabajador seleccionado =
                    (Trabajador) trabajador.getSelectedItem();

            if (seleccionado != null) {
                trabajador.setToolTipText(
                        seleccionado.getDni() + " - " +
                                seleccionado.getApellidos() + ", " +
                                seleccionado.getNombres()
                );

                if (editando == 0 && esRolTrabajador()) {
                    nombre.setText(
                            seleccionado.getNombres() + " " +
                                    seleccionado.getApellidos()
                    );
                }
            } else {
                trabajador.setToolTipText(null);
            }
        });
    }

    private void actualizarEtiquetaTrabajador() {
        if (etiquetaTrabajador != null) {
            etiquetaTrabajador.setText(
                    esRolTrabajador()
                            ? "Trabajador vinculado (obligatorio)"
                            : "Trabajador vinculado (opcional)"
            );
        }
    }

    private void cargarTrabajadores(
            int idUsuario, Integer idSeleccionado) {

        DefaultComboBoxModel<Trabajador> modelo =
                new DefaultComboBoxModel<>();

        modelo.addElement(null);
        Trabajador asociado = null;

        for (Trabajador t :
                controlador.trabajadoresDisponibles(idUsuario)) {

            modelo.addElement(t);

            if (idSeleccionado != null &&
                    t.getIdTrabajador() == idSeleccionado) {
                asociado = t;
            }
        }

        trabajador.setModel(modelo);
        trabajador.setSelectedItem(asociado);

        ayudaTrabajador.setText(
                modelo.getSize() == 1
                        ? "Sin trabajadores disponibles. Regístrelos primero en Personal."
                        : "Trabajadores disponibles para vinculación."
        );

        if (idSeleccionado != null && asociado == null) {
            throw new IllegalStateException(
                    "El trabajador vinculado ya no está disponible.");
        }
    }

    private void editar() {
        int fila = tabla.getSelectedRow();

        if (fila < 0) {
            JOptionPane.showMessageDialog(
                    this, "Seleccione un usuario.");
            return;
        }

        String seleccionado =
                String.valueOf(tabla.getValueAt(fila, 0));

        Usuario u = usuarios.stream()
                .filter(x -> x.nombreUsuario().equals(seleccionado))
                .findFirst()
                .orElseThrow();

        editando = u.idUsuario();

        login.setText(u.nombreUsuario());
        nombre.setText(u.nombreCompleto());
        correo.setText(u.correo());

        for (int i = 0; i < roles.getItemCount(); i++) {
            if (roles.getItemAt(i).idRol() == u.idRol()) {
                roles.setSelectedIndex(i);
                break;
            }
        }

        cargarTrabajadores(
                u.idUsuario(), u.idTrabajador());

        clave.setText("");
        estado.setSelectedItem(u.estado());

        tabs.setSelectedIndex(1);
    }

    private void limpiar() {
        editando = 0;

        login.setText("");
        nombre.setText("");
        correo.setText("");
        clave.setText("");

        trabajador.setSelectedItem(null);
        estado.setSelectedIndex(0);

        for (int i = 0; i < roles.getItemCount(); i++) {
            if ("Trabajador".equals(
                    roles.getItemAt(i).nombre())) {

                roles.setSelectedIndex(i);
                break;
            }
        }

        actualizarEtiquetaTrabajador();
    }

    private void guardar(JButton boton) {
        char[] password = clave.getPassword();

        try {
            Rol rol = (Rol) roles.getSelectedItem();

            if (rol == null) {
                throw new IllegalArgumentException(
                        "Seleccione un rol.");
            }

            Trabajador seleccionado =
                    (Trabajador) trabajador.getSelectedItem();

            if (esRolTrabajador() && seleccionado == null) {
                throw new IllegalArgumentException(
                        "Seleccione un trabajador registrado en Personal.");
            }

            if (editando == 0 && password.length == 0) {
                throw new IllegalArgumentException(
                        "Ingrese una contraseña para el nuevo usuario.");
            }

            Usuario u = new Usuario(
                    editando,
                    rol.idRol(),
                    seleccionado == null
                            ? null
                            : seleccionado.getIdTrabajador(),
                    nombre.getText(),
                    login.getText(),
                    correo.getText(),
                    (String) estado.getSelectedItem()
            );

            boton.setEnabled(false);

            new SwingWorker<Integer, Void>() {

                @Override
                protected Integer doInBackground() {
                    try {
                        return controlador.guardar(u, password);
                    } finally {
                        Arrays.fill(password, '\0');
                    }
                }

                @Override
                protected void done() {
                    boton.setEnabled(true);
                    clave.setText("");

                    try {
                        get();

                        limpiar();
                        refrescar();
                        tabs.setSelectedIndex(0);

                        JOptionPane.showMessageDialog(
                                PnlUsuarios.this,
                                "Usuario guardado correctamente."
                        );

                    } catch (InterruptedException ex) {
                        Thread.currentThread().interrupt();
                        error(ex);

                    } catch (ExecutionException ex) {
                        error(ex.getCause());
                    }
                }
            }.execute();

        } catch (RuntimeException ex) {
            Arrays.fill(password, '\0');
            error(ex);
        }
    }

    private void error(Throwable ex) {
        JOptionPane.showMessageDialog(
                this,
                ex.getMessage(),
                "Usuarios",
                JOptionPane.WARNING_MESSAGE
        );
    }

    private JPanel crearContenido() {
        JPanel contenedor = new JPanel(new BorderLayout());
        contenedor.setBackground(FONDO);
        contenedor.setBorder(
                new EmptyBorder(24, 28, 26, 28));

        JPanel cabecera = new JPanel(new BorderLayout());
        cabecera.setOpaque(false);

        JPanel titulos = new JPanel();
        titulos.setOpaque(false);
        titulos.setLayout(
                new BoxLayout(titulos, BoxLayout.Y_AXIS));

        JLabel titulo = new JLabel("Usuarios");
        titulo.setForeground(TEXTO);
        titulo.setFont(
                new Font("Segoe UI", Font.BOLD, 26));

        JLabel subtitulo = new JLabel(
                "Administración de usuarios y roles de acceso");
        subtitulo.setForeground(TEXTO_SUAVE);
        subtitulo.setFont(
                new Font("Segoe UI", Font.PLAIN, 13));

        titulos.add(titulo);
        titulos.add(Box.createVerticalStrut(4));
        titulos.add(subtitulo);

        JLabel usuario =
                new JLabel(controlador.rolActual());

        usuario.setForeground(TEXTO);
        usuario.setFont(
                new Font("Segoe UI", Font.BOLD, 13));

        cabecera.add(titulos, BorderLayout.WEST);
        cabecera.add(usuario, BorderLayout.EAST);

        tabs.setFont(
                new Font("Segoe UI", Font.PLAIN, 13));
        tabs.setBackground(BLANCO);

        tabs.addTab("Listado", crearListado());
        tabs.addTab("Registrar usuario", crearRegistro());
        tabs.addTab("Roles y permisos", crearRoles());

        tabs.setEnabledAt(
                1, controlador.puedeGestionar());

        tabs.addChangeListener(e -> {
            if (tabs.getSelectedIndex() != 1 || editando != 0) {
                return;
            }

            // Evita reemplazar una selección o datos aún no guardados.
            if (!login.getText().isBlank()
                    || !nombre.getText().isBlank()
                    || !correo.getText().isBlank()
                    || trabajador.getSelectedItem() != null) {
                return;
            }

            try {
                cargarTrabajadores(0, null);
            } catch (RuntimeException ex) {
                error(ex);
            }
        });

        JPanel centro = new JPanel(new BorderLayout());
        centro.setOpaque(false);
        centro.setBorder(
                new EmptyBorder(22, 0, 0, 0));

        centro.add(tabs, BorderLayout.CENTER);

        contenedor.add(cabecera, BorderLayout.NORTH);
        contenedor.add(centro, BorderLayout.CENTER);

        return contenedor;
    }

    private JPanel crearListado() {
        JPanel panel = panelBase();

        JPanel superior =
                new JPanel(new FlowLayout(FlowLayout.LEFT));
        superior.setOpaque(false);

        superior.add(new JLabel("Buscar:"));
        superior.add(buscar);

        JButton consultar = botonSecundario("Buscar");
        JButton nuevo = botonPrimario("Nuevo usuario");
        JButton editar = botonSecundario(
                "Editar / cambiar estado");

        nuevo.setEnabled(controlador.puedeGestionar());
        editar.setEnabled(controlador.puedeGestionar());

        consultar.addActionListener(e -> refrescar());

        nuevo.addActionListener(e -> {
            limpiar();
            tabs.setSelectedIndex(1);
        });

        editar.addActionListener(e -> {
            try {
                editar();
            } catch (RuntimeException ex) {
                error(ex);
            }
        });

        superior.add(consultar);
        superior.add(nuevo);

        tabla.setFont(
                new Font("Segoe UI", Font.PLAIN, 13));
        tabla.setRowHeight(31);
        tabla.setForeground(TEXTO);
        tabla.setGridColor(new Color(232, 235, 239));
        tabla.setSelectionBackground(new Color(224, 235, 246));
        tabla.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 12));
        tabla.getTableHeader().setBackground(new Color(239, 243, 247));
        tabla.getTableHeader().setForeground(TEXTO);
        tabla.setFillsViewportHeight(true);
        tabla.setSelectionMode(
                ListSelectionModel.SINGLE_SELECTION);

        panel.add(superior, BorderLayout.NORTH);
        JScrollPane scroll = new JScrollPane(tabla);
        scroll.setBorder(BorderFactory.createLineBorder(BORDE));
        panel.add(scroll, BorderLayout.CENTER);

        JPanel acciones =
                new JPanel(new FlowLayout(FlowLayout.RIGHT));
        acciones.setOpaque(false);
        acciones.add(editar);

        panel.add(acciones, BorderLayout.SOUTH);

        return panel;
    }

    private JPanel crearRegistro() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(BLANCO);
        panel.setBorder(
                new EmptyBorder(26, 26, 26, 26));

        JPanel formulario =
                new JPanel(new GridBagLayout());
        formulario.setOpaque(false);

        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(8, 8, 8, 8);
        g.fill = GridBagConstraints.HORIZONTAL;
        g.anchor = GridBagConstraints.WEST;
        g.weightx = 1;

        agregarCampo(
                formulario, g, 0, 0,
                "Usuario", login);

        agregarCampo(
                formulario, g, 1, 0,
                "Nombre completo", nombre);

        agregarCampo(
                formulario, g, 0, 1,
                "Correo", correo);

        agregarCampo(
                formulario, g, 1, 1,
                "Contraseña (obligatoria al crear)", CampoContrasena.conVisibilidad(clave));

        clave.setToolTipText(
                "Al editar, deje el campo vacío para conservar la contraseña.");

        agregarCampo(
                formulario, g, 0, 2,
                "Rol", roles);

        agregarCampo(
                formulario, g, 1, 2,
                "Estado", estado);

        etiquetaTrabajador = agregarCampo(
                formulario, g, 0, 3,
                "Trabajador vinculado (obligatorio)",
                trabajador);

        ayudaTrabajador.setFont(
                new Font("Segoe UI", Font.PLAIN, 11));
        ayudaTrabajador.setForeground(TEXTO_SUAVE);

        g.gridx = 1;
        g.gridy = 3;
        g.gridwidth = 1;

        formulario.add(ayudaTrabajador, g);

        JPanel botones =
                new JPanel(new FlowLayout(
                        FlowLayout.RIGHT, 8, 0));
        botones.setOpaque(false);

        JButton cancelar =
                botonSecundario("Cancelar");

        cancelar.addActionListener(e -> {
            limpiar();
            tabs.setSelectedIndex(0);
        });

        botones.add(cancelar);

        JButton guardar = botonPrimario("Guardar");
        guardar.addActionListener(e -> guardar(guardar));

        botones.add(guardar);

        g.gridx = 0;
        g.gridy = 4;
        g.gridwidth = 2;
        g.insets = new Insets(22, 8, 8, 8);

        formulario.add(botones, g);
        panel.add(formulario);

        return panel;
    }

    private JPanel crearRoles() {
        return new PnlPermisos(
                controlador.controladorRoles());
    }

    private static void ampliarEtiquetas(
            Container contenedor) {

        for (Component c : contenedor.getComponents()) {

            if (c instanceof JLabel etiqueta) {
                Dimension d =
                        etiqueta.getPreferredSize();

                etiqueta.setPreferredSize(
                        new Dimension(d.width + 16, d.height));

                etiqueta.setMaximumSize(
                        new Dimension(
                                Integer.MAX_VALUE, d.height));
            }

            if (c instanceof Container hijo) {
                ampliarEtiquetas(hijo);
            }
        }
    }

    private JPanel panelBase() {
        JPanel panel =
                new JPanel(new BorderLayout(0, 16));

        panel.setBackground(BLANCO);
        panel.setBorder(
                new EmptyBorder(22, 22, 22, 22));

        return panel;
    }

    private JLabel agregarCampo(
            JPanel panel,
            GridBagConstraints g,
            int col,
            int fila,
            String etiqueta,
            JComponent campo) {

        JPanel bloque = new JPanel();
        bloque.setOpaque(false);
        bloque.setLayout(
                new BoxLayout(bloque, BoxLayout.Y_AXIS));

        JLabel label = new JLabel(etiqueta);
        label.setForeground(TEXTO);
        label.setFont(
                new Font("Segoe UI", Font.PLAIN, 13));
        label.setAlignmentX(Component.LEFT_ALIGNMENT);

        campo.setPreferredSize(
                new Dimension(270, 38));
        campo.setMaximumSize(
                new Dimension(270, 38));
        campo.setFont(
                new Font("Segoe UI", Font.PLAIN, 13));
        campo.setAlignmentX(Component.LEFT_ALIGNMENT);

        if (campo instanceof JTextField tf) {
            tf.setBorder(
                    BorderFactory.createCompoundBorder(
                            BorderFactory.createLineBorder(BORDE),
                            new EmptyBorder(0, 9, 0, 9)
                    )
            );
        }

        bloque.add(label);
        bloque.add(Box.createVerticalStrut(6));
        bloque.add(campo);

        g.gridx = col;
        g.gridy = fila;
        g.gridwidth = 1;

        panel.add(bloque, g);

        return label;
    }

    private JButton botonPrimario(String texto) {
        JButton boton = new JButton(texto);

        boton.setBackground(AZUL_CLARO);
        boton.setForeground(BLANCO);
        boton.setFont(
                new Font("Segoe UI", Font.BOLD, 13));
        boton.setFocusPainted(false);
        boton.setBorder(
                new EmptyBorder(10, 16, 10, 16));

        return boton;
    }

    private JButton botonSecundario(String texto) {
        JButton boton = new JButton(texto);

        boton.setBackground(BLANCO);
        boton.setForeground(TEXTO);
        boton.setFont(
                new Font("Segoe UI", Font.PLAIN, 13));
        boton.setFocusPainted(false);

        boton.setBorder(
                BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(BORDE),
                        new EmptyBorder(9, 14, 9, 14)
                )
        );

        return boton;
    }
}
