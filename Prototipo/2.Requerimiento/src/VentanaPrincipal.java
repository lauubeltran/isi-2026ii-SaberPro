import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;

public class VentanaPrincipal extends JFrame {
    private final String rutaCsv;
    private final RepositorioEstudiantes repositorio;
    private final VerificadorRequisitos verificador = new VerificadorRequisitos();
    private CardLayout cardLayout;
    private JPanel panelCentral;
    private VentanaListadoHabilitados listadoHabilitados;
    private JPanel itemEvaluacion;
    private JPanel itemHabilitados;
    private JPanel itemPreinscripcion;
    
    private List<Estudiante> estudiantes;
    private final List<Estudiante> visibles = new ArrayList<>();
    private String filtro = "TODOS";

    private DefaultTableModel modelo;
    private JTable tabla;
    private final JLabel lblProgreso = new JLabel();
    private final Tema.Pildora lblBadge = new Tema.Pildora("0", Tema.ACENTO, Tema.SOBRE_ACENTO);
    private final BarraProgreso barra = new BarraProgreso();
    private final Map<String, JButton> chips = new LinkedHashMap<>();
    private final JLabel lblTotal = new JLabel("0");
    private final JLabel lblHab = new JLabel("0");
    private final JLabel lblNo = new JLabel("0");
    private final JLabel lblPend = new JLabel("0");

    public VentanaPrincipal(String rutaCsv) {
        super("Saber Pro · Panel del Director del programa");
        System.out.println("EJECUTANDO VENTANAPRINCIPAL MODIFICADA");
        this.rutaCsv = rutaCsv;
        repositorio = new RepositorioEstudiantes(rutaCsv);
        try {
            estudiantes = repositorio.cargar();
        } catch (IOException | RuntimeException ex) {
            JOptionPane.showMessageDialog(null,
                    "No se pudo leer " + rutaCsv + ":\n" + ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
            System.exit(1);
        }

        setDefaultCloseOperation(EXIT_ON_CLOSE);
        getContentPane().setBackground(Tema.FONDO);
        setLayout(new BorderLayout());
        add(crearSidebar(), BorderLayout.WEST);

cardLayout = new CardLayout();

panelCentral = new JPanel(cardLayout);

panelCentral.add(
        crearContenido(),
        "evaluacion");

listadoHabilitados = new VentanaListadoHabilitados(estudiantes);
panelCentral.add(listadoHabilitados, "habilitados");
panelCentral.add(new VentanaPreinscripcionICFES(rutaCsv, "Director del programa"), "preinscripcion");

add(panelCentral, BorderLayout.CENTER);

        refrescarTabla();
        setSize(1280, 820);
        setMinimumSize(new Dimension(1050, 650));
        setLocationRelativeTo(null);
    }

    // ---------------------------------------------------------------- Sidebar
    private JPanel crearSidebar() {
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setBackground(Tema.PANEL);
        p.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 0, 1, Tema.BORDE),
                BorderFactory.createEmptyBorder(26, 18, 18, 18)));
        p.setPreferredSize(new Dimension(280, 0));

        JLabel logo = new JLabel("Saber Pro");
        logo.setFont(new Font("Segoe UI", Font.BOLD, 26));
        logo.setForeground(Tema.ACENTO);
        logo.setAlignmentX(Component.LEFT_ALIGNMENT);
        JLabel sub = new JLabel("Gestión de pruebas");
        sub.setFont(Tema.fuente(Font.PLAIN, 12f));
        sub.setForeground(Tema.TEXTO_SUAVE);
        sub.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel seccion = new JLabel("PROGRAMA");
        seccion.setFont(Tema.fuente(Font.BOLD, 11f));
        seccion.setForeground(Tema.TEXTO_SUAVE);
        seccion.setAlignmentX(Component.LEFT_ALIGNMENT);

        p.add(logo);
        p.add(sub);
        p.add(Box.createVerticalStrut(30));
        p.add(seccion);
        p.add(Box.createVerticalStrut(10));
        p.add(itemMenu("Inicio", null, false));
        p.add(Box.createVerticalStrut(4));
   itemEvaluacion = itemMenu(
        "Evaluación de estudiantes",
        lblBadge,
        true,
        () -> {
            activarMenu(itemEvaluacion);
            cardLayout.show(panelCentral, "evaluacion");
        });

p.add(itemEvaluacion);
        p.add(Box.createVerticalStrut(4));
       itemHabilitados = itemMenu(
        "Listado de habilitados",
        null,
        false,
        () -> {
            activarMenu(itemHabilitados);
            cardLayout.show(panelCentral, "habilitados");
        });

p.add(itemHabilitados);
        p.add(Box.createVerticalStrut(4));        itemPreinscripcion = itemMenu(
                "Pre-inscripción ICFES",
                null,
                false,
                () -> {
                    activarMenu(itemPreinscripcion);
                    cardLayout.show(panelCentral, "preinscripcion");
                });
        p.add(itemPreinscripcion);
        p.add(Box.createVerticalGlue());

        // Tarjeta del Director
        JLabel quien = new JLabel("Director del programa");
        quien.setFont(Tema.fuente(Font.BOLD, 14f));
        quien.setForeground(Tema.TEXTO);
        JLabel prog = new JLabel("Ingeniería de Sistemas");
        prog.setFont(Tema.fuente(Font.PLAIN, 12f));
        prog.setForeground(Tema.TEXTO_SUAVE);
        JPanel textos = new JPanel(new GridLayout(2, 1));
        textos.setOpaque(false);
        textos.add(quien);
        textos.add(prog);

        Tema.Redondeado avatar = new Tema.Redondeado(Tema.PANEL_CLARO, 40);
        avatar.setLayout(new BorderLayout());
        avatar.setPreferredSize(new Dimension(40, 40));
        JLabel ini = new JLabel("DP", SwingConstants.CENTER);
        ini.setFont(Tema.fuente(Font.BOLD, 13f));
        ini.setForeground(Tema.TEXTO);
        avatar.add(ini);

        JPanel usuario = new JPanel(new BorderLayout(12, 0));
        usuario.setOpaque(false);
        usuario.add(avatar, BorderLayout.WEST);
        usuario.add(textos, BorderLayout.CENTER);
        usuario.setAlignmentX(Component.LEFT_ALIGNMENT);
        usuario.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
        p.add(usuario);
        p.add(Box.createVerticalStrut(14));

        JButton salir = Tema.boton("Cerrar sesión", Tema.PANEL_CLARO, Tema.TEXTO_SUAVE);
        salir.setAlignmentX(Component.LEFT_ALIGNMENT);
        salir.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        salir.addActionListener(ev -> cerrarSesion());
        p.add(salir);
        return p;
    }

    private JPanel itemMenu(String texto, JComponent extra, boolean activo) {
        Tema.Redondeado fila = new Tema.Redondeado(
                activo ? Tema.PANEL_CLARO : new Color(0, 0, 0, 0), 12);
        fila.setLayout(new BorderLayout());
        fila.setBorder(BorderFactory.createEmptyBorder(10, 12, 10, 12));
        fila.setAlignmentX(Component.LEFT_ALIGNMENT);
        fila.setMaximumSize(new Dimension(Integer.MAX_VALUE, 48));
        JLabel l = new JLabel(texto);
        l.setFont(Tema.fuente(activo ? Font.BOLD : Font.PLAIN, 13f));
        l.setForeground(activo ? Tema.TEXTO : Tema.TEXTO_SUAVE);
        fila.add(l, BorderLayout.WEST);
        if (extra != null) {
            JPanel w = new JPanel(new GridBagLayout());
            w.setOpaque(false);
            w.add(extra);
            fila.add(w, BorderLayout.EAST);
        }
        return fila;
    }

    
private JPanel itemMenu(
        String texto,
        JComponent extra,
        boolean activo,
        Runnable accion) {

    JPanel fila = itemMenu(texto, extra, activo);

    fila.setCursor(new Cursor(Cursor.HAND_CURSOR));

    MouseAdapter listener = new MouseAdapter() {
        @Override
        public void mouseClicked(MouseEvent ev) {
            accion.run();
        }
    };

    agregarClickRecursivo(fila, listener);
    
    return fila;
}

private void agregarClickRecursivo(
        Component componente,
        MouseAdapter listener) {

    componente.addMouseListener(listener);

    if (componente instanceof Container) {
        for (Component hijo :
                ((Container) componente).getComponents()) {
            agregarClickRecursivo(hijo, listener);
        }
    }
}
private void activarMenu(JPanel seleccionado) {

    actualizarItem(itemEvaluacion,
            seleccionado == itemEvaluacion);

    actualizarItem(itemHabilitados,
            seleccionado == itemHabilitados);
    
    
    actualizarItem(itemPreinscripcion,
            seleccionado == itemPreinscripcion);
}
private void actualizarItem(
        JPanel panel,
        boolean activo) {

    if (panel == null) {
        return;
    }

    if (panel instanceof Tema.Redondeado r) {

        r.setColor(
                activo
                        ? Tema.PANEL_CLARO
                        : new Color(0, 0, 0, 0));
    }

    for (Component c : panel.getComponents()) {

        if (c instanceof JLabel l) {

            l.setFont(
                    Tema.fuente(
                            activo
                                    ? Font.BOLD
                                    : Font.PLAIN,
                            13f));

            l.setForeground(
                    activo
                            ? Tema.TEXTO
                            : Tema.TEXTO_SUAVE);
        }
    }

    panel.repaint();
}

    // -------------------------------------------------------------- Contenido
    private JPanel crearContenido() {
        JPanel c = new JPanel(new BorderLayout(0, 16));
        c.setBackground(Tema.FONDO);
        c.setBorder(BorderFactory.createEmptyBorder(26, 32, 22, 32));

        // Saludo + título
        JLabel kicker = new JLabel("PANEL DEL DIRECTOR DEL PROGRAMA · INGENIERÍA DE SISTEMAS");
        kicker.setFont(Tema.fuente(Font.BOLD, 11f));
        kicker.setForeground(Tema.ACENTO);
        kicker.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel titulo = new JLabel("Evaluación de estudiantes");
        titulo.setFont(new Font("Segoe UI", Font.BOLD, 34));
        titulo.setForeground(Tema.TEXTO);
        titulo.setAlignmentX(Component.LEFT_ALIGNMENT);
        JLabel sub = new JLabel("Estudiantes próximos a terminar su plan de estudios. "
                + "Revisa su información académica y define su condición.");
        sub.setFont(Tema.fuente(Font.PLAIN, 13f));
        sub.setForeground(Tema.TEXTO_SUAVE);
        sub.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel textos = new JPanel();
        textos.setLayout(new BoxLayout(textos, BoxLayout.Y_AXIS));
        textos.setOpaque(false);
        textos.add(kicker);
        textos.add(Box.createVerticalStrut(6));
        textos.add(titulo);
        textos.add(Box.createVerticalStrut(4));
        textos.add(sub);

        JButton btnReiniciar = Tema.boton("↻  Reiniciar demo", Tema.PANEL_CLARO, Tema.TEXTO);
        btnReiniciar.addActionListener(ev -> reiniciar());
        JButton btnRegistrar = Tema.boton( "+ Registrar estudiantes",Tema.ACENTO,Tema.SOBRE_ACENTO);
        btnRegistrar.addActionListener(ev -> abrirRegistro());
        JPanel acciones = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        acciones.setOpaque(false);
        JPanel accionesFila = new JPanel();
        accionesFila.setLayout(new BoxLayout(accionesFila, BoxLayout.X_AXIS));
        accionesFila.setOpaque(false);
        accionesFila.add(new Tema.Pildora("Prototipo · datos de ejemplo",
        Tema.ACENTO_SUAVE, Tema.ACENTO));
        accionesFila.add(Box.createHorizontalStrut(8));
        accionesFila.add(btnRegistrar);
        accionesFila.add(Box.createHorizontalStrut(8));
        accionesFila.add(btnReiniciar);
        acciones.add(accionesFila);

        JPanel fila1 = new JPanel();
        fila1.setLayout(new BoxLayout(fila1, BoxLayout.Y_AXIS));
        fila1.setOpaque(false);
        textos.setAlignmentX(Component.LEFT_ALIGNMENT);
        acciones.setAlignmentX(Component.LEFT_ALIGNMENT);
        fila1.add(textos);
        fila1.add(Box.createVerticalStrut(12));
        fila1.add(acciones);

        // Tarjetas de resumen
        JPanel stats = new JPanel(new GridLayout(1, 4, 14, 0));
        stats.setOpaque(false);
        stats.add(tarjetaStat("Estudiantes en lista", lblTotal, Tema.TEXTO));
        stats.add(tarjetaStat("Habilitados", lblHab, Tema.VERDE));
        stats.add(tarjetaStat("No habilitados", lblNo, Tema.ROJO));
        stats.add(tarjetaStat("Sin evaluar", lblPend, Tema.AMBAR));
        stats.setPreferredSize(new Dimension(0, 88));
        stats.setMaximumSize(new Dimension(Integer.MAX_VALUE, 88));
        stats.setAlignmentX(Component.LEFT_ALIGNMENT);

        // Progreso
        lblProgreso.setFont(Tema.fuente(Font.BOLD, 13f));
        lblProgreso.setForeground(Tema.TEXTO);
        barra.setPreferredSize(new Dimension(320, 8));
        barra.setMaximumSize(new Dimension(320, 8));
        JPanel progreso = new JPanel();
        progreso.setLayout(new BoxLayout(progreso, BoxLayout.X_AXIS));
        progreso.setOpaque(false);
        progreso.add(lblProgreso);
        progreso.add(Box.createHorizontalStrut(14));
        progreso.add(barra);
        progreso.add(Box.createHorizontalGlue());
        progreso.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel filtros = crearFiltros();
        filtros.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel cab = new JPanel();
        cab.setLayout(new BoxLayout(cab, BoxLayout.Y_AXIS));
        cab.setOpaque(false);
        cab.add(fila1);
        cab.add(Box.createVerticalStrut(18));
        cab.add(stats);
        cab.add(Box.createVerticalStrut(16));
        cab.add(progreso);
        cab.add(Box.createVerticalStrut(12));
        cab.add(filtros);
        c.add(cab, BorderLayout.NORTH);

        // Tabla
        crearTabla();
        JScrollPane sp = new JScrollPane(tabla);
        sp.setBorder(BorderFactory.createLineBorder(Tema.BORDE));
        sp.getViewport().setBackground(Tema.PANEL);
        sp.getVerticalScrollBar().setUnitIncrement(18);
        c.add(sp, BorderLayout.CENTER);

        // Pie
        JLabel pista = new JLabel("Doble clic en un estudiante para revisarlo y, "
                + "si es necesario, ajustar su condición.");
        pista.setFont(Tema.fuente(Font.PLAIN, 12f));
        pista.setForeground(Tema.TEXTO_SUAVE);
        JButton btnVerificar = Tema.boton("Verificar seleccionado", Tema.ACENTO, Tema.SOBRE_ACENTO);
        btnVerificar.addActionListener(ev -> {
            int fila = tabla.getSelectedRow();
            if (fila < 0) {
                JOptionPane.showMessageDialog(this, "Selecciona un estudiante primero.");
            } else {
                revisar(fila);
            }
        });
        JPanel sur = new JPanel(new BorderLayout());
        sur.setOpaque(false);
        sur.add(pista, BorderLayout.WEST);
        sur.add(btnVerificar, BorderLayout.EAST);
        c.add(sur, BorderLayout.SOUTH);
        return c;
    }

    private JPanel tarjetaStat(String titulo, JLabel valor, Color color) {
        Tema.Redondeado t = new Tema.Redondeado(Tema.PANEL, 18);
        t.setLayout(new BorderLayout(0, 2));
        t.setBorder(BorderFactory.createEmptyBorder(12, 20, 12, 20));
        valor.setFont(new Font("Segoe UI", Font.BOLD, 30));
        valor.setForeground(color);
        JLabel l = new JLabel(titulo);
        l.setFont(Tema.fuente(Font.PLAIN, 12f));
        l.setForeground(Tema.TEXTO_SUAVE);
        t.add(valor, BorderLayout.CENTER);
        t.add(l, BorderLayout.SOUTH);
        return t;
    }

    private JPanel crearFiltros() {
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.X_AXIS));
        p.setOpaque(false);
        String[][] defs = {
                {"TODOS", "Todos"},
                {Estudiante.PENDIENTE, "Sin evaluar"},
                {Estudiante.HABILITADO, "Habilitados"},
                {Estudiante.NO_HABILITADO, "No habilitados"}
        };
        for (String[] d : defs) {
            JButton b = Tema.boton(d[1], Tema.PANEL_CLARO, Tema.TEXTO);
            b.setBorder(BorderFactory.createEmptyBorder(6, 16, 6, 16));
            b.addActionListener(ev -> {
                filtro = d[0];
                refrescarTabla();
            });
            chips.put(d[0], b);
            p.add(b);
            p.add(Box.createHorizontalStrut(8));
        }
        p.add(Box.createHorizontalGlue());
        return p;
    }

    private void crearTabla() {
        modelo = new DefaultTableModel(new String[]{
                "ESTUDIANTE", "AVANCE DEL PLAN", "CRÉDITOS APROBADOS", "CONDICIÓN", "OBSERVACIONES"}, 0) {
            @Override
            public boolean isCellEditable(int fila, int col) {
                return false;
            }
        };
        tabla = new JTable(modelo);
        tabla.setRowHeight(58);
        tabla.setShowGrid(false);
        tabla.setIntercellSpacing(new Dimension(0, 0));
        tabla.setBackground(Tema.PANEL);
        tabla.setForeground(Tema.TEXTO);
        tabla.setSelectionBackground(Tema.PANEL_CLARO);
        tabla.setSelectionForeground(Tema.TEXTO);
        tabla.setFillsViewportHeight(true);
        tabla.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        JTableHeader h = tabla.getTableHeader();
        h.setReorderingAllowed(false);
        h.setDefaultRenderer(new EncabezadoRenderer());
        h.setPreferredSize(new Dimension(0, 44));

        tabla.getColumnModel().getColumn(0).setCellRenderer(new CeldaEstudiante());
        tabla.getColumnModel().getColumn(1).setCellRenderer(new CeldaAvance());
        tabla.getColumnModel().getColumn(2).setCellRenderer(new CeldaTexto(Tema.TEXTO, Font.BOLD));
        tabla.getColumnModel().getColumn(3).setCellRenderer(new CeldaCondicion());
        tabla.getColumnModel().getColumn(4).setCellRenderer(new CeldaTexto(Tema.TEXTO_SUAVE, Font.PLAIN));
        int[] anchos = {300, 240, 160, 180, 260};
        for (int i = 0; i < anchos.length; i++) {
            tabla.getColumnModel().getColumn(i).setPreferredWidth(anchos[i]);
        }

        tabla.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent ev) {
                if (ev.getClickCount() == 2 && tabla.getSelectedRow() >= 0) {
                    revisar(tabla.getSelectedRow());
                }
            }
        });
    }


    // ------------------------------------------------------------------ Lógica
    private void abrirRegistro() {
    VentanaRegistroEstudiantes ventana =
            new VentanaRegistroEstudiantes(this, repositorio);

    ventana.setVisible(true);

    // La ventana es modal: esta línea se ejecuta al cerrarla.
    try {
        estudiantes = repositorio.cargar();
        refrescarTabla();
        } catch (IOException | RuntimeException ex) {
            JOptionPane.showMessageDialog(
                    this,
                    "No se pudo actualizar la lista de estudiantes:\n"
                            + ex.getMessage(),
                    "Error",
                    JOptionPane.ERROR_MESSAGE
            );
        }
    }
    private void revisar(int fila) {
        while (fila >= 0 && fila < visibles.size()) {
            Estudiante e = visibles.get(fila);
            VerificadorRequisitos.Resultado r = verificador.verificar(e);

            boolean haySiguiente = fila < visibles.size() - 1;
            DialogoReporte dlg = new DialogoReporte(this, e, r, haySiguiente);
            dlg.setVisible(true);

            DialogoReporte.Accion accion = dlg.getAccion();
            if (accion == DialogoReporte.Accion.CERRAR) return;

            e.setCondicion(dlg.getCondicionFinal());
            e.setObservaciones(dlg.getObservaciones());
            if (!guardar()) return;

            Estudiante siguiente = (haySiguiente && accion == DialogoReporte.Accion.GUARDAR_Y_SIGUIENTE)
                    ? visibles.get(fila + 1) : null;
            refrescarTabla();
            if (siguiente == null) return;

            fila = visibles.indexOf(siguiente);
            if (fila < 0) return;
            tabla.setRowSelectionInterval(fila, fila);
        }
    }

    private void reiniciar() {
        int op = JOptionPane.showConfirmDialog(this,
                "¿Volver todos los estudiantes a «Sin evaluar» y borrar las observaciones?",
                "Reiniciar demo", JOptionPane.YES_NO_OPTION);
        if (op != JOptionPane.YES_OPTION) return;
        for (Estudiante e : estudiantes) {
            e.setCondicion(Estudiante.PENDIENTE);
            e.setObservaciones("");
        }
        guardar();
        refrescarTabla();
    }

    private void cerrarSesion() {
        new LoginDirector(rutaCsv).setVisible(true);
        dispose();
    }

    private boolean guardar() {
        try {
            repositorio.guardar(estudiantes);
            return true;
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this,
                    "No se pudo guardar el archivo:\n" + ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
            return false;
        }
    }

    private void refrescarTabla() {
        Estudiante seleccionado = tabla.getSelectedRow() >= 0
                ? visibles.get(tabla.getSelectedRow()) : null;
        visibles.clear();
        modelo.setRowCount(0);
        int hab = 0;
        int no = 0;
        int pend = 0;
        for (Estudiante e : estudiantes) {
            if (Estudiante.HABILITADO.equals(e.getCondicion())) hab++;
            else if (Estudiante.NO_HABILITADO.equals(e.getCondicion())) no++;
            else pend++;

            if (!filtro.equals("TODOS") && !filtro.equals(e.getCondicion())) continue;
            visibles.add(e);
            modelo.addRow(new Object[]{
                    e,
                    e.getPorcentajeAvance(),
                    e.getCreditosAprobados() + " / " + e.getCreditosTotales(),
                    e.getCondicion(),
                    e.getObservaciones()
            });
        }
        int evaluados = hab + no;
        barra.setValor(evaluados, estudiantes.size());
        lblProgreso.setText(evaluados + " de " + estudiantes.size() + " estudiantes evaluados");
        lblBadge.setText(String.valueOf(pend));
        lblTotal.setText(String.valueOf(estudiantes.size()));
        lblHab.setText(String.valueOf(hab));
        lblNo.setText(String.valueOf(no));
        lblPend.setText(String.valueOf(pend));

        for (Map.Entry<String, JButton> en : chips.entrySet()) {
            boolean activo = en.getKey().equals(filtro);
            en.getValue().setBackground(activo ? Tema.ACENTO : Tema.PANEL_CLARO);
            en.getValue().setForeground(activo ? Tema.SOBRE_ACENTO : Tema.TEXTO);
            en.getValue().repaint();
        }
        if (seleccionado != null) {
            int i = visibles.indexOf(seleccionado);
            if (i >= 0) tabla.setRowSelectionInterval(i, i);
        }
                if (listadoHabilitados != null) {
            listadoHabilitados.actualizar(estudiantes);
        }
    }

    // -------------------------------------------------------------- Componentes
    private static class BarraProgreso extends JComponent {
        private int valor = 0;
        private int max = 1;

        void setValor(int v, int m) {
            valor = v;
            max = Math.max(m, 1);
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(Tema.BORDE);
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), getHeight(), getHeight());
            g2.setColor(Tema.ACENTO);
            int w = (int) (getWidth() * (valor / (double) max));
            if (w > 0) g2.fillRoundRect(0, 0, w, getHeight(), getHeight(), getHeight());
            g2.dispose();
        }
    }

    private static class EncabezadoRenderer extends JLabel implements TableCellRenderer {
        EncabezadoRenderer() {
            setOpaque(true);
            setBackground(Tema.PANEL_CLARO);
            setForeground(Tema.TEXTO_SUAVE);
            setFont(Tema.fuente(Font.BOLD, 11f));
            setBorder(BorderFactory.createEmptyBorder(0, 16, 0, 8));
        }

        @Override
        public Component getTableCellRendererComponent(JTable t, Object v, boolean s,
                                                       boolean f, int r, int c) {
            setText(String.valueOf(v));
            return this;
        }
    }

    /** Base: pinta el fondo de la fila y delega el contenido. */
    private abstract static class Celda extends JComponent implements TableCellRenderer {
        protected boolean seleccionada;
        protected Object valor;

        @Override
        public Component getTableCellRendererComponent(JTable t, Object v, boolean s,
                                                       boolean f, int r, int c) {
            seleccionada = s;
            valor = v;
            return this;
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                    RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g2.setColor(seleccionada ? Tema.SELECCION : Tema.PANEL);
            g2.fillRect(0, 0, getWidth(), getHeight());
            g2.setColor(Tema.BORDE);
            g2.drawLine(0, getHeight() - 1, getWidth(), getHeight() - 1);
            dibujar(g2);
            g2.dispose();
        }

        protected abstract void dibujar(Graphics2D g2);
    }

    private static class CeldaEstudiante extends Celda {
        @Override
        protected void dibujar(Graphics2D g2) {
            Estudiante e = (Estudiante) valor;
            int h = getHeight();
            int d = 38;
            int y = (h - d) / 2;
            g2.setColor(Tema.AVATAR_FONDO);
            g2.fillOval(16, y, d, d);

            String[] p = e.getNombre().trim().split("\\s+");
            String ini = "" + p[0].charAt(0) + (p.length > 1 ? p[1].charAt(0) : "");
            g2.setFont(Tema.fuente(Font.BOLD, 13f));
            g2.setColor(Tema.AVATAR_TEXTO);
            FontMetrics fm = g2.getFontMetrics();
            g2.drawString(ini, 16 + (d - fm.stringWidth(ini)) / 2,
                    y + (d + fm.getAscent() - fm.getDescent()) / 2);

            g2.setFont(Tema.fuente(Font.BOLD, 14f));
            g2.setColor(Tema.TEXTO);
            g2.drawString(e.getNombre(), 64, h / 2 - 2);
            g2.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
            g2.setColor(Tema.TEXTO_SUAVE);
            g2.drawString(e.getCodigo(), 64, h / 2 + 15);
        }
    }

    private static class CeldaAvance extends Celda {
        @Override
        protected void dibujar(Graphics2D g2) {
            double pct = (Double) valor;
            int x0 = 16;
            int w = getWidth() - 16 - 80;
            int h = 8;
            int y = (getHeight() - h) / 2;
            g2.setColor(Tema.BORDE);
            g2.fillRoundRect(x0, y, w, h, h, h);
            g2.setColor(Tema.AZUL);
            int fill = (int) (w * Math.min(pct, 100) / 100.0);
            if (fill > 0) g2.fillRoundRect(x0, y, fill, h, h, h);
            g2.setFont(Tema.fuente(Font.BOLD, 13f));
            g2.setColor(Tema.TEXTO);
            g2.drawString(String.format("%.0f%%", pct), getWidth() - 62, getHeight() / 2 + 5);
        }
    }

    private static class CeldaTexto extends Celda {
        private final Color color;
        private final int estilo;

        CeldaTexto(Color color, int estilo) {
            this.color = color;
            this.estilo = estilo;
        }

        @Override
        protected void dibujar(Graphics2D g2) {
            String s = valor == null || valor.toString().isEmpty() ? "—" : valor.toString();
            g2.setFont(Tema.fuente(estilo, 13f));
            g2.setColor(color);
            FontMetrics fm = g2.getFontMetrics();
            int max = getWidth() - 32;
            if (fm.stringWidth(s) > max) {
                while (s.length() > 1 && fm.stringWidth(s + "…") > max) {
                    s = s.substring(0, s.length() - 1);
                }
                s = s + "…";
            }
            g2.drawString(s, 16, (getHeight() + fm.getAscent() - fm.getDescent()) / 2);
        }
    }

    private static class CeldaCondicion extends Celda {
        @Override
        protected void dibujar(Graphics2D g2) {
            String v = String.valueOf(valor);
            String texto;
            Color letra;
            Color fondo;
            if (Estudiante.HABILITADO.equals(v)) {
                texto = "✔  Habilitado";
                letra = Tema.VERDE;
                fondo = new Color(34, 197, 94, 45);
            } else if (Estudiante.NO_HABILITADO.equals(v)) {
                texto = "✘  No habilitado";
                letra = Tema.ROJO;
                fondo = new Color(239, 68, 68, 45);
            } else {
                texto = "○  Sin evaluar";
                letra = Tema.AMBAR;
                fondo = new Color(245, 158, 11, 40);
            }
            g2.setFont(Tema.fuente(Font.BOLD, 13f));
            FontMetrics fm = g2.getFontMetrics();
            int w = fm.stringWidth(texto) + 28;
            int h = 30;
            int y = (getHeight() - h) / 2;
            g2.setColor(fondo);
            g2.fillRoundRect(16, y, w, h, h, h);
            g2.setColor(letra);
            g2.drawString(texto, 30, y + (h + fm.getAscent() - fm.getDescent()) / 2);
        }
    }
}