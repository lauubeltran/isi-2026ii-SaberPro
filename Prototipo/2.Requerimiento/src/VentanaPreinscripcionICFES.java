import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;

/**
 * Sección «Pre-inscripción ICFES» (requerimiento 3.4, que incluye 3.5).
 *
 * El pre-registro oficial lo hace la Universidad en la plataforma del ICFES, usando el
 * listado que se guarda en «Listado de habilitados» (3.3). Este módulo NO lo realiza:
 * da el enlace a la plataforma, permite indicar los estudiantes que están en el exterior
 * (que en el ICFES se pre-registran por la opción «Saber Pro Exterior») y deja constancia
 * en el sistema de que el pre-registro se hizo (fecha y responsable). Al confirmar, los
 * estudiantes pasan a «pre-registrado» y se registra la notificación (3.5).
 *
 * Es autónoma: lee ella misma estudiantes.csv cada vez que se abre, y no modifica
 * Estudiante ni RepositorioEstudiantes. Guarda lo suyo en dos archivos, junto a estudiantes.csv:
 *   - preregistro.csv     (estado de pre-registro y datos de exterior)
 *   - notificaciones.csv  (notificaciones generadas, con el campo «leida»)
 */
public class VentanaPreinscripcionICFES extends JPanel {

    // ------------------------------------------------------------ Configuración
    /**
     * Página del ICFES con la información del pre-registro. Cuando tengas la dirección
     * directa de la plataforma de la institución, cámbiala aquí.
     */
    public static final String URL_ICFES =
            "https://www.icfes.gov.co/evaluaciones-icfes/acerca-del-examen-saber-pro/"
            + "etapas-previas-examen-saber-pro/";

    public static final String PERIODO_DEFECTO = "2026-2";

    private static final String ESTADO_PRE = "PRE_REGISTRADO";
    private static final String TODOS = "TODOS";
    private static final String POR_PRE = "POR_PRE";
    private static final String PRE = "PRE";
    private static final String EXT = "EXT";
    private static final String ASUNTO = "Pre-registro Saber Pro";

    private static final DateTimeFormatter FMT_GUARDADO =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final DateTimeFormatter FMT_CORTO =
            DateTimeFormatter.ofPattern("dd/MM/yyyy");

    // ------------------------------------------------------------------ Estado
    private final RepositorioEstudiantes repoEstudiantes;
    private final Almacen almacen;
    private final String responsable;

    private List<Estudiante> estudiantes = new ArrayList<>();
    private Map<String, Registro> registros = new LinkedHashMap<>();
    private final List<Estudiante> visibles = new ArrayList<>();
    private final Set<String> seleccionados = new LinkedHashSet<>();
    private String filtro = TODOS;

    // ---------------------------------------------------------------------- UI
    private final CardLayout cartas = new CardLayout();
    private final JPanel centro = new JPanel(cartas);
    private DefaultTableModel modelo;
    private JTable tabla;
    private final JTextField campoPeriodo = new JTextField(PERIODO_DEFECTO, 7);
    private final Map<String, JButton> chips = new LinkedHashMap<>();
    private final JLabel lblPor = new JLabel("0");
    private final JLabel lblPre = new JLabel("0");
    private final JLabel lblExt = new JLabel("0");
    private final JLabel lblSeleccion = new JLabel("0 seleccionados");
    private JButton btnPlataforma;
    private JButton btnConfirmar;
    private JButton btnTodos;
    private JButton btnLimpiar;
    private JButton btnExterior;

    // Valores que viajan a los renderers de la tabla
    private record Casilla(boolean marcada, boolean habilitada) { }
    private record EstadoFila(boolean preRegistrado, String fecha) { }
    private record NotifFila(boolean preRegistrado, boolean tieneCorreo) { }

    /**
     * @param rutaCsv      ruta de estudiantes.csv (la misma que usa VentanaPrincipal)
     * @param responsable  nombre que se guarda como responsable de cada confirmación
     */
    public VentanaPreinscripcionICFES(String rutaCsv, String responsable) {
        this.repoEstudiantes = new RepositorioEstudiantes(rutaCsv);
        this.almacen = new Almacen(rutaCsv);
        this.responsable = responsable == null || responsable.isBlank()
                ? "Coordinador de pruebas" : responsable.trim();
        try {
            registros = almacen.cargar();
        } catch (IOException | RuntimeException ex) {
            System.err.println("No se pudo leer preregistro.csv: " + ex.getMessage());
            registros = new LinkedHashMap<>();
        }

        setLayout(new BorderLayout(0, 16));
        setBackground(Tema.FONDO);
        setBorder(BorderFactory.createEmptyBorder(26, 32, 22, 32));

        add(crearCabecera(), BorderLayout.NORTH);
        add(crearCentro(), BorderLayout.CENTER);
        add(crearPie(), BorderLayout.SOUTH);

        // Cada vez que se abre la sección se vuelve a leer la lista de estudiantes.
        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentShown(ComponentEvent ev) {
                recargar();
            }
        });
        recargar();
    }

    /** Vuelve a leer estudiantes.csv y refresca la pantalla. */
    void recargar() {
        try {
            estudiantes = repoEstudiantes.cargar();
        } catch (IOException | RuntimeException ex) {
            System.err.println("No se pudo leer la lista de estudiantes: " + ex.getMessage());
        }
        refrescarVista();
    }

    // ================================================================ Cabecera
    private JPanel crearCabecera() {
        JLabel kicker = new JLabel("PRE-INSCRIPCIÓN · PRUEBAS SABER PRO");
        kicker.setFont(Tema.fuente(Font.BOLD, 11f));
        kicker.setForeground(Tema.ACENTO);
        kicker.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel titulo = new JLabel("Pre-inscripción ICFES");
        titulo.setFont(new Font("Segoe UI", Font.BOLD, 34));
        titulo.setForeground(Tema.TEXTO);
        titulo.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel sub = new JLabel("El pre-registro oficial se realiza en la plataforma del ICFES. "
                + "Aquí dejas constancia de que se hizo y notificas a los estudiantes.");
        sub.setFont(Tema.fuente(Font.PLAIN, 13f));
        sub.setForeground(Tema.TEXTO_SUAVE);
        sub.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel textos = new JPanel();
        textos.setLayout(new BoxLayout(textos, BoxLayout.Y_AXIS));
        textos.setOpaque(false);
        textos.setAlignmentX(Component.LEFT_ALIGNMENT);
        textos.add(kicker);
        textos.add(Box.createVerticalStrut(6));
        textos.add(titulo);
        textos.add(Box.createVerticalStrut(4));
        textos.add(sub);

        // Tarjetas de resumen (solo datos propios de esta sección)
        JPanel stats = new JPanel(new GridLayout(1, 3, 14, 0));
        stats.setOpaque(false);
        stats.add(tarjetaStat("Por pre-registrar", lblPor, Tema.AMBAR));
        stats.add(tarjetaStat("Pre-registrados", lblPre, Tema.AZUL));
        stats.add(tarjetaStat("En el exterior", lblExt, Tema.ACENTO));
        stats.setPreferredSize(new Dimension(0, 78));
        stats.setMaximumSize(new Dimension(Integer.MAX_VALUE, 78));
        stats.setAlignmentX(Component.LEFT_ALIGNMENT);

        // Los dos pasos de esta sección
        btnPlataforma = Tema.boton("Abrir plataforma ICFES", Tema.ACENTO, Tema.SOBRE_ACENTO);
        btnPlataforma.addActionListener(ev -> abrirPlataforma());
        btnConfirmar = Tema.boton("Confirmar pre-registro", Tema.ACENTO, Tema.SOBRE_ACENTO);
        btnConfirmar.addActionListener(ev -> confirmarPreRegistro());

        JPanel pasos = new JPanel(new GridLayout(1, 2, 14, 0));
        pasos.setOpaque(false);
        pasos.add(tarjetaPaso(1, "Cargar en el ICFES",
                "Con el listado guardado en «Listado de habilitados»", btnPlataforma));
        pasos.add(tarjetaPaso(2, "Confirmar pre-registro",
                "Marca a quienes ya pre-registraste y confirma", btnConfirmar));
        pasos.setPreferredSize(new Dimension(0, 112));
        pasos.setMaximumSize(new Dimension(Integer.MAX_VALUE, 112));
        pasos.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel filtros = crearFiltros();
        filtros.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel cab = new JPanel();
        cab.setLayout(new BoxLayout(cab, BoxLayout.Y_AXIS));
        cab.setOpaque(false);
        cab.add(textos);
        cab.add(Box.createVerticalStrut(16));
        cab.add(stats);
        cab.add(Box.createVerticalStrut(14));
        cab.add(pasos);
        cab.add(Box.createVerticalStrut(14));
        cab.add(filtros);
        return cab;
    }

    private JPanel tarjetaStat(String titulo, JLabel valor, Color color) {
        Tema.Redondeado t = new Tema.Redondeado(Tema.PANEL, 18);
        t.setLayout(new BorderLayout(0, 0));
        t.setBorder(BorderFactory.createEmptyBorder(8, 20, 8, 20));
        valor.setFont(new Font("Segoe UI", Font.BOLD, 26));
        valor.setForeground(color);
        JLabel l = new JLabel(titulo);
        l.setFont(Tema.fuente(Font.PLAIN, 12f));
        l.setForeground(Tema.TEXTO_SUAVE);
        t.add(valor, BorderLayout.CENTER);
        t.add(l, BorderLayout.SOUTH);
        return t;
    }

    private JPanel tarjetaPaso(int numero, String titulo, String descripcion, JButton boton) {
        Tema.Redondeado t = new Tema.Redondeado(Tema.PANEL, 18);
        t.setLayout(new BoxLayout(t, BoxLayout.Y_AXIS));
        t.setBorder(BorderFactory.createEmptyBorder(12, 18, 12, 18));

        Tema.Redondeado circulo = new Tema.Redondeado(Tema.ACENTO_SUAVE, 28);
        circulo.setLayout(new BorderLayout());
        circulo.setPreferredSize(new Dimension(28, 28));
        circulo.setMaximumSize(new Dimension(28, 28));
        JLabel n = new JLabel(String.valueOf(numero), SwingConstants.CENTER);
        n.setFont(Tema.fuente(Font.BOLD, 13f));
        n.setForeground(Tema.ACENTO);
        circulo.add(n);

        JLabel tit = new JLabel(titulo);
        tit.setFont(Tema.fuente(Font.BOLD, 14f));
        tit.setForeground(Tema.TEXTO);

        JPanel fila = new JPanel();
        fila.setLayout(new BoxLayout(fila, BoxLayout.X_AXIS));
        fila.setOpaque(false);
        fila.setAlignmentX(Component.LEFT_ALIGNMENT);
        fila.add(circulo);
        fila.add(Box.createHorizontalStrut(10));
        fila.add(tit);
        fila.add(Box.createHorizontalGlue());

        JLabel desc = new JLabel(descripcion);
        desc.setFont(Tema.fuente(Font.PLAIN, 12f));
        desc.setForeground(Tema.TEXTO_SUAVE);
        desc.setAlignmentX(Component.LEFT_ALIGNMENT);

        boton.setAlignmentX(Component.LEFT_ALIGNMENT);

        t.add(fila);
        t.add(Box.createVerticalStrut(6));
        t.add(desc);
        t.add(Box.createVerticalGlue());
        t.add(boton);
        return t;
    }

    private JPanel crearFiltros() {
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.X_AXIS));
        p.setOpaque(false);

        String[][] defs = {
                {TODOS, "Todos"},
                {POR_PRE, "Por pre-registrar"},
                {PRE, "Pre-registrados"},
                {EXT, "En el exterior"}
        };
        for (String[] d : defs) {
            JButton b = Tema.boton(d[1], Tema.PANEL_CLARO, Tema.TEXTO);
            b.setBorder(BorderFactory.createEmptyBorder(6, 16, 6, 16));
            b.addActionListener(ev -> {
                filtro = d[0];
                refrescarVista();
            });
            chips.put(d[0], b);
            p.add(b);
            p.add(Box.createHorizontalStrut(8));
        }
        p.add(Box.createHorizontalGlue());

        Tema.estilizarCampo(campoPeriodo);
        campoPeriodo.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Tema.BORDE),
                BorderFactory.createEmptyBorder(6, 10, 6, 10)));
        campoPeriodo.setPreferredSize(new Dimension(90, 34));
        campoPeriodo.setMaximumSize(new Dimension(90, 34));

        JButton reiniciar = Tema.boton("↻  Reiniciar pre-registros", Tema.PANEL_CLARO, Tema.TEXTO);
        reiniciar.setBorder(BorderFactory.createEmptyBorder(6, 16, 6, 16));
        reiniciar.addActionListener(ev -> reiniciarConConfirmacion());

        p.add(etiquetaFiltro("Periodo de aplicación"));
        p.add(Box.createHorizontalStrut(8));
        p.add(campoPeriodo);
        p.add(Box.createHorizontalStrut(14));
        p.add(reiniciar);
        return p;
    }

    private JLabel etiquetaFiltro(String texto) {
        JLabel l = new JLabel(texto);
        l.setFont(Tema.fuente(Font.BOLD, 12f));
        l.setForeground(Tema.TEXTO_SUAVE);
        return l;
    }

    // =================================================================== Centro
    private JPanel crearCentro() {
        centro.setOpaque(false);

        modelo = new DefaultTableModel(new String[]{
                "", "ESTUDIANTE", "ESTADO", "NOTIFICACIÓN", "EXTERIOR"}, 0) {
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

        tabla.getColumnModel().getColumn(0).setCellRenderer(new CeldaCasilla());
        tabla.getColumnModel().getColumn(1).setCellRenderer(new CeldaEstudiante());
        tabla.getColumnModel().getColumn(2).setCellRenderer(new CeldaEstado());
        tabla.getColumnModel().getColumn(3).setCellRenderer(new CeldaNotificacion());
        tabla.getColumnModel().getColumn(4).setCellRenderer(new CeldaTexto(Tema.TEXTO_SUAVE, Font.PLAIN));
        int[] anchos = {56, 330, 280, 250, 280};
        for (int i = 0; i < anchos.length; i++) {
            tabla.getColumnModel().getColumn(i).setPreferredWidth(anchos[i]);
        }
        tabla.getColumnModel().getColumn(0).setMaxWidth(64);

        tabla.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent ev) {
                int fila = tabla.rowAtPoint(ev.getPoint());
                int col = tabla.columnAtPoint(ev.getPoint());
                if (fila >= 0 && col == 0) alternar(fila);
            }

            @Override
            public void mouseClicked(MouseEvent ev) {
                int fila = tabla.rowAtPoint(ev.getPoint());
                int col = tabla.columnAtPoint(ev.getPoint());
                if (ev.getClickCount() == 2 && fila >= 0 && col != 0) {
                    abrirExterior(visibles.get(fila));
                }
            }
        });

        JScrollPane sp = new JScrollPane(tabla);
        sp.setBorder(BorderFactory.createLineBorder(Tema.BORDE));
        sp.getViewport().setBackground(Tema.PANEL);
        sp.getVerticalScrollBar().setUnitIncrement(18);

        // Mensaje cuando todavía no hay habilitados
        Tema.Redondeado vacio = new Tema.Redondeado(Tema.PANEL, 18);
        vacio.setLayout(new GridBagLayout());
        JPanel msg = new JPanel();
        msg.setLayout(new BoxLayout(msg, BoxLayout.Y_AXIS));
        msg.setOpaque(false);
        JLabel m1 = new JLabel("Aún no hay estudiantes habilitados");
        m1.setFont(Tema.fuente(Font.BOLD, 18f));
        m1.setForeground(Tema.TEXTO);
        m1.setAlignmentX(Component.CENTER_ALIGNMENT);
        JLabel m2 = new JLabel("Evalúa a los estudiantes en «Evaluación de estudiantes». "
                + "Los habilitados aparecerán aquí para pre-registrarlos.");
        m2.setFont(Tema.fuente(Font.PLAIN, 13f));
        m2.setForeground(Tema.TEXTO_SUAVE);
        m2.setAlignmentX(Component.CENTER_ALIGNMENT);
        msg.add(m1);
        msg.add(Box.createVerticalStrut(6));
        msg.add(m2);
        vacio.add(msg);

        centro.add(sp, "tabla");
        centro.add(vacio, "vacio");
        return centro;
    }

    // ===================================================================== Pie
    private JPanel crearPie() {
        JLabel pista = new JLabel("Doble clic en un estudiante para indicar si está en el exterior.");
        pista.setFont(Tema.fuente(Font.PLAIN, 12f));
        pista.setForeground(Tema.TEXTO_SUAVE);

        lblSeleccion.setFont(Tema.fuente(Font.BOLD, 13f));
        lblSeleccion.setForeground(Tema.TEXTO);

        btnTodos = Tema.boton("Seleccionar todos", Tema.PANEL_CLARO, Tema.TEXTO);
        btnTodos.addActionListener(ev -> seleccionarTodos());
        btnLimpiar = Tema.boton("Limpiar", Tema.PANEL_CLARO, Tema.TEXTO);
        btnLimpiar.addActionListener(ev -> {
            seleccionados.clear();
            refrescarVista();
        });
        btnExterior = Tema.boton("Estudiante en el exterior", Tema.ACENTO, Tema.SOBRE_ACENTO);
        btnExterior.addActionListener(ev -> {
            int fila = tabla.getSelectedRow();
            if (fila < 0 || fila >= visibles.size()) {
                info("Selecciona un estudiante primero.");
            } else {
                abrirExterior(visibles.get(fila));
            }
        });

        JPanel acciones = new JPanel();
        acciones.setLayout(new BoxLayout(acciones, BoxLayout.X_AXIS));
        acciones.setOpaque(false);
        acciones.add(lblSeleccion);
        acciones.add(Box.createHorizontalStrut(14));
        acciones.add(btnTodos);
        acciones.add(Box.createHorizontalStrut(8));
        acciones.add(btnLimpiar);
        acciones.add(Box.createHorizontalStrut(8));
        acciones.add(btnExterior);

        JPanel pie = new JPanel(new BorderLayout(16, 0));
        pie.setOpaque(false);
        pie.add(pista, BorderLayout.CENTER);
        pie.add(acciones, BorderLayout.EAST);
        return pie;
    }

    // =========================================================== Refrescar vista
    private void refrescarVista() {
        int filaSel = tabla.getSelectedRow();
        Estudiante antes = filaSel >= 0 && filaSel < visibles.size() ? visibles.get(filaSel) : null;

        visibles.clear();
        modelo.setRowCount(0);
        Set<String> seleccionables = new LinkedHashSet<>();
        int total = 0;
        int porPre = 0;
        int pre = 0;
        int exterior = 0;

        for (Estudiante e : estudiantes) {
            if (!Estudiante.HABILITADO.equals(e.getCondicion())) continue;

            Registro r = registros.get(clave(e));
            boolean yaPre = r != null && r.preRegistrado();
            boolean enExt = r != null && r.enExterior();

            total++;
            if (yaPre) pre++; else porPre++;
            if (enExt) exterior++;

            if (POR_PRE.equals(filtro) && yaPre) continue;
            if (PRE.equals(filtro) && !yaPre) continue;
            if (EXT.equals(filtro) && !enExt) continue;

            if (!yaPre) seleccionables.add(clave(e));
            visibles.add(e);
            modelo.addRow(new Object[]{
                    new Casilla(!yaPre && seleccionados.contains(clave(e)), !yaPre),
                    e,
                    new EstadoFila(yaPre, yaPre ? r.fecha : ""),
                    new NotifFila(yaPre, !vacio(e.getCorreo())),
                    textoExterior(r)
            });
        }
        seleccionados.retainAll(seleccionables);

        lblPor.setText(String.valueOf(porPre));
        lblPre.setText(String.valueOf(pre));
        lblExt.setText(String.valueOf(exterior));

        for (Map.Entry<String, JButton> en : chips.entrySet()) {
            boolean activo = en.getKey().equals(filtro);
            en.getValue().setBackground(activo ? Tema.ACENTO : Tema.PANEL_CLARO);
            en.getValue().setForeground(activo ? Tema.SOBRE_ACENTO : Tema.TEXTO);
            en.getValue().repaint();
        }

        cartas.show(centro, total == 0 ? "vacio" : "tabla");
        if (antes != null) {
            int i = visibles.indexOf(antes);
            if (i >= 0) tabla.setRowSelectionInterval(i, i);
        }
        actualizarEstadoBotones(total == 0);
    }

    private void actualizarEstadoBotones(boolean sinHabilitados) {
        int n = seleccionados.size();
        lblSeleccion.setText(n + (n == 1 ? " seleccionado" : " seleccionados"));
        btnPlataforma.setEnabled(true);
        btnConfirmar.setEnabled(n > 0);
        btnTodos.setEnabled(!sinHabilitados);
        btnLimpiar.setEnabled(n > 0);
        btnExterior.setEnabled(!sinHabilitados);
    }

    // ----------------------------------------------------------------- Selección
    private void alternar(int fila) {
        if (fila < 0 || fila >= visibles.size()) return;
        Estudiante e = visibles.get(fila);
        Registro r = registros.get(clave(e));
        if (r != null && r.preRegistrado()) return;

        String cod = clave(e);
        if (!seleccionados.remove(cod)) seleccionados.add(cod);
        modelo.setValueAt(new Casilla(seleccionados.contains(cod), true), fila, 0);
        actualizarEstadoBotones(false);
    }

    private void seleccionarTodos() {
        for (Estudiante e : visibles) {
            Registro r = registros.get(clave(e));
            if (r == null || !r.preRegistrado()) seleccionados.add(clave(e));
        }
        refrescarVista();
    }

    /** Estudiantes marcados que siguen siendo válidos (habilitados y sin pre-registro). */
    List<Estudiante> estudiantesSeleccionados() {
        List<Estudiante> lista = new ArrayList<>();
        for (Estudiante e : estudiantes) {
            if (!seleccionados.contains(clave(e))) continue;
            Registro r = registros.get(clave(e));
            boolean yaPre = r != null && r.preRegistrado();
            if (Estudiante.HABILITADO.equals(e.getCondicion()) && !yaPre) lista.add(e);
        }
        return lista;
    }

    // ================================================ Paso 1: abrir plataforma
    private void abrirPlataforma() {
        try {
            if (!Desktop.isDesktopSupported()
                    || !Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                throw new UnsupportedOperationException("El sistema no permite abrir el navegador.");
            }
            Desktop.getDesktop().browse(URI.create(URL_ICFES));
        } catch (Exception ex) {
            try {
                Toolkit.getDefaultToolkit().getSystemClipboard()
                        .setContents(new StringSelection(URL_ICFES), null);
            } catch (Exception ignorada) { }
            info("No se pudo abrir el navegador. El enlace se copió al portapapeles:\n" + URL_ICFES);
        }
    }

    // ============================================= Paso 2: confirmar pre-registro
    private void confirmarPreRegistro() {
        List<Estudiante> sel = estudiantesSeleccionados();
        if (sel.isEmpty()) {
            info("Marca al menos un estudiante.");
            return;
        }
        String periodo = periodoActual();
        if (periodo == null) return;

        int op = JOptionPane.showConfirmDialog(this,
                "¿Ya pre-registraste a " + sel.size() + " estudiante(s) en la plataforma del ICFES?\n\n"
                        + "Al confirmar, quedarán como «pre-registrados» y se registrará\n"
                        + "la notificación para cada uno. Esta acción no se puede deshacer.",
                "Confirmar pre-registro", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
        if (op != JOptionPane.YES_OPTION) return;

        int sinCorreo = 0;
        for (Estudiante e : sel) {
            if (vacio(e.getCorreo())) sinCorreo++;
        }

        try {
            int n = aplicarPreRegistro(sel, periodo);
            seleccionados.clear();
            refrescarVista();
            String aviso = sinCorreo == 0 ? ""
                    : "\n\nAtención: " + sinCorreo + " estudiante(s) no tienen correo registrado "
                      + "y no recibirán el aviso.";
            info("Pre-registro confirmado para " + n + " estudiante(s).\n"
                    + "Se registró la notificación de cada uno.\n\n"
                    + "El ICFES enviará a cada estudiante un correo con sus credenciales\n"
                    + "para continuar con el registro y el pago." + aviso);
        } catch (IOException ex) {
            seleccionados.clear();
            refrescarVista();
            error(ex.getMessage());
        }
    }

    /**
     * Marca como pre-registrados a los estudiantes (fecha y responsable) y registra la
     * notificación de cada uno (3.5). Guarda primero; si falla, no cambia nada en memoria.
     */
    int aplicarPreRegistro(List<Estudiante> sel, String periodo) throws IOException {
        String ahora = LocalDateTime.now().format(FMT_GUARDADO);

        Map<String, Registro> nuevo = copiaRegistros();
        List<String[]> notificaciones = new ArrayList<>();
        for (Estudiante e : sel) {
            Registro r = nuevo.computeIfAbsent(clave(e), Registro::new);
            r.estado = ESTADO_PRE;
            r.fecha = ahora;
            r.responsable = responsable;
            r.periodo = periodo;
            notificaciones.add(notificacion(e, periodo, ahora));
        }

        almacen.guardar(nuevo);
        registros = nuevo;

        try {
            almacen.agregarNotificaciones(notificaciones);
        } catch (IOException ex) {
            throw new IOException("El pre-registro quedó guardado, pero no se pudo registrar "
                    + "la notificación:\n" + ex.getMessage(), ex);
        }
        return sel.size();
    }

    /** codigo;correo;fecha_envio;asunto;mensaje;leida */
    private String[] notificacion(Estudiante e, String periodo, String fecha) {
        String mensaje = "Hola " + e.getNombre() + ". La Universidad de los Llanos te pre-registró "
                + "para presentar las pruebas Saber Pro (periodo " + periodo + "). "
                + "El ICFES te enviará un correo con tu usuario y contraseña provisional "
                + "para que continúes con el registro y el pago. "
                + "Revisa tu correo y mantén tus datos actualizados.";
        return new String[]{
                limpiar(e.getCodigo()), limpiar(e.getCorreo()), fecha,
                ASUNTO, limpiar(mensaje), "false"
        };
    }

    // ================================================== Exterior (diálogo)
    private void abrirExterior(Estudiante e) {
        Registro r = registros.get(clave(e));
        if (r != null && r.preRegistrado()) {
            info(e.getNombre() + " ya fue pre-registrado el " + fechaCorta(r.fecha)
                    + ".\nSus datos ya no se pueden modificar.");
            return;
        }

        DialogoExterior dlg = new DialogoExterior(SwingUtilities.getWindowAncestor(this), e,
                r == null ? "" : r.pais,
                r == null ? "" : r.ciudad);
        dlg.setVisible(true);
        if (!dlg.guardado()) return;

        try {
            guardarExterior(e, dlg.exterior(), dlg.pais(), dlg.ciudad());
        } catch (IOException ex) {
            error("No se pudieron guardar los datos:\n" + ex.getMessage());
        }
    }

    /** Guarda (o borra) el país y la ciudad de un estudiante que está en el exterior. */
    void guardarExterior(Estudiante e, boolean exterior, String pais, String ciudad)
            throws IOException {
        Map<String, Registro> nuevo = copiaRegistros();
        Registro n = nuevo.computeIfAbsent(clave(e), Registro::new);
        n.pais = exterior ? limpiar(pais) : "";
        n.ciudad = exterior ? limpiar(ciudad) : "";
        if (n.vacio()) nuevo.remove(clave(e));

        almacen.guardar(nuevo);
        registros = nuevo;
        refrescarVista();
    }

    // ===================================================== Reiniciar (demo)
    private void reiniciarConConfirmacion() {
        int op = JOptionPane.showConfirmDialog(this,
                "¿Borrar todos los pre-registros y las notificaciones guardadas?\n"
                        + "Los estudiantes volverán a quedar «por pre-registrar».",
                "Reiniciar pre-registros", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (op != JOptionPane.YES_OPTION) return;
        try {
            reiniciarDatos();
        } catch (IOException ex) {
            error("No se pudieron reiniciar los pre-registros:\n" + ex.getMessage());
        }
    }

    void reiniciarDatos() throws IOException {
        almacen.reiniciar();
        registros = new LinkedHashMap<>();
        seleccionados.clear();
        refrescarVista();
    }

    // ============================================================ Utilidades
    private Map<String, Registro> copiaRegistros() {
        Map<String, Registro> copia = new LinkedHashMap<>();
        for (Map.Entry<String, Registro> en : registros.entrySet()) {
            copia.put(en.getKey(), en.getValue().copia());
        }
        return copia;
    }

    private String periodoActual() {
        String p = campoPeriodo.getText().trim();
        if (!p.matches("\\d{4}-\\d")) {
            info("Escribe el periodo de aplicación con el formato AAAA-N, por ejemplo 2026-2.");
            campoPeriodo.requestFocusInWindow();
            return null;
        }
        return p;
    }

    private static String clave(Estudiante e) {
        return e.getCodigo() == null ? "" : e.getCodigo().trim();
    }

    private static boolean vacio(String s) {
        return s == null || s.isBlank();
    }

    private static String textoExterior(Registro r) {
        if (r == null || !r.enExterior()) return "";
        return r.pais + " · " + r.ciudad;
    }

    private static String fechaCorta(String guardada) {
        try {
            return LocalDateTime.parse(guardada, FMT_GUARDADO).format(FMT_CORTO);
        } catch (RuntimeException ex) {
            return guardada;
        }
    }

    /** Evita que un «;» o un salto de línea desplace las columnas del CSV. */
    private static String limpiar(String s) {
        if (s == null) return "";
        return s.replace(";", ",").replace("\r", " ").replace("\n", " ").trim();
    }

    private void info(String mensaje) {
        JOptionPane.showMessageDialog(this, mensaje, "Pre-inscripción ICFES",
                JOptionPane.INFORMATION_MESSAGE);
    }

    private void error(String mensaje) {
        JOptionPane.showMessageDialog(this, mensaje, "Error", JOptionPane.ERROR_MESSAGE);
    }

    // ======================================================== Datos persistentes
    private static final class Registro {
        final String codigo;
        String pais = "";
        String ciudad = "";
        String estado = "";
        String fecha = "";
        String responsable = "";
        String periodo = "";

        Registro(String codigo) {
            this.codigo = codigo;
        }

        Registro copia() {
            Registro c = new Registro(codigo);
            c.pais = pais;
            c.ciudad = ciudad;
            c.estado = estado;
            c.fecha = fecha;
            c.responsable = responsable;
            c.periodo = periodo;
            return c;
        }

        boolean preRegistrado() {
            return ESTADO_PRE.equals(estado);
        }

        boolean enExterior() {
            return !pais.isBlank() || !ciudad.isBlank();
        }

        boolean vacio() {
            return !enExterior() && !preRegistrado();
        }
    }

    /** Lee y escribe preregistro.csv y notificaciones.csv junto a estudiantes.csv. */
    private static final class Almacen {
        private static final String SEP = ";";
        private static final String CAB_PRE =
                "codigo;exterior_pais;exterior_ciudad;estado;fecha_confirmacion;responsable;periodo";
        private static final String CAB_NOT =
                "codigo;correo;fecha_envio;asunto;mensaje;leida";

        private final Path rutaPre;
        private final Path rutaNot;

        Almacen(String rutaCsv) {
            Path base = Paths.get(rutaCsv).toAbsolutePath();
            rutaPre = base.resolveSibling("preregistro.csv");
            rutaNot = base.resolveSibling("notificaciones.csv");
        }

        /** Lee por nombre de columna, así tolera archivos con columnas de más o de menos. */
        Map<String, Registro> cargar() throws IOException {
            Map<String, Registro> mapa = new LinkedHashMap<>();
            if (!Files.exists(rutaPre)) return mapa;
            List<String> lineas = Files.readAllLines(rutaPre, StandardCharsets.UTF_8);
            if (lineas.isEmpty()) return mapa;

            String[] cab = lineas.get(0).replace("\uFEFF", "").split(SEP, -1);
            int iCodigo = indice(cab, "codigo");
            int iPais = indice(cab, "exterior_pais");
            int iCiudad = indice(cab, "exterior_ciudad");
            int iEstado = indice(cab, "estado");
            int iFecha = indice(cab, "fecha_confirmacion");
            int iResp = indice(cab, "responsable");
            int iPeriodo = indice(cab, "periodo");
            if (iCodigo < 0) return mapa;

            for (int i = 1; i < lineas.size(); i++) {
                String linea = lineas.get(i).trim();
                if (linea.isEmpty()) continue;
                String[] d = linea.split(SEP, -1);
                String codigo = campo(d, iCodigo);
                if (codigo.isEmpty()) continue;
                Registro r = new Registro(codigo);
                r.pais = campo(d, iPais);
                r.ciudad = campo(d, iCiudad);
                r.estado = campo(d, iEstado);
                r.fecha = campo(d, iFecha);
                r.responsable = campo(d, iResp);
                r.periodo = campo(d, iPeriodo);
                mapa.put(r.codigo, r);
            }
            return mapa;
        }

        private static int indice(String[] cab, String nombre) {
            for (int i = 0; i < cab.length; i++) {
                if (cab[i].trim().equalsIgnoreCase(nombre)) return i;
            }
            return -1;
        }

        private static String campo(String[] datos, int i) {
            return i < 0 || i >= datos.length ? "" : datos[i].trim();
        }

        void guardar(Map<String, Registro> mapa) throws IOException {
            List<String> lineas = new ArrayList<>();
            lineas.add(CAB_PRE);
            for (Registro r : mapa.values()) {
                if (r.vacio()) continue;
                lineas.add(String.join(SEP,
                        limpiar(r.codigo), limpiar(r.pais), limpiar(r.ciudad),
                        limpiar(r.estado), limpiar(r.fecha), limpiar(r.responsable),
                        limpiar(r.periodo)));
            }
            Files.createDirectories(rutaPre.getParent());
            Files.write(rutaPre, lineas, StandardCharsets.UTF_8);
        }

        void agregarNotificaciones(List<String[]> filas) throws IOException {
            Files.createDirectories(rutaNot.getParent());
            List<String> lineas = new ArrayList<>();
            if (!Files.exists(rutaNot)) lineas.add(CAB_NOT);
            for (String[] f : filas) lineas.add(String.join(SEP, f));
            Files.write(rutaNot, lineas, StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        }

        void reiniciar() throws IOException {
            Files.createDirectories(rutaPre.getParent());
            Files.write(rutaPre, List.of(CAB_PRE), StandardCharsets.UTF_8);
            Files.write(rutaNot, List.of(CAB_NOT), StandardCharsets.UTF_8);
        }
    }

    // =================================================== Diálogo «Exterior»
    private static final class DialogoExterior extends JDialog {
        private final JCheckBox chkExterior =
                new JCheckBox("El estudiante está en el exterior (intercambio)");
        private final JTextField campoPais = new JTextField(28);
        private final JTextField campoCiudad = new JTextField(28);
        private boolean guardado = false;

        DialogoExterior(Window padre, Estudiante e, String paisActual, String ciudadActual) {
            super(padre, "Estudiante en el exterior", ModalityType.APPLICATION_MODAL);

            JPanel c = new JPanel();
            c.setLayout(new BoxLayout(c, BoxLayout.Y_AXIS));
            c.setBackground(Tema.PANEL);
            c.setBorder(BorderFactory.createEmptyBorder(22, 26, 22, 26));

            JLabel nombre = new JLabel(e.getNombre());
            nombre.setFont(Tema.fuente(Font.BOLD, 17f));
            nombre.setForeground(Tema.TEXTO);
            nombre.setAlignmentX(Component.LEFT_ALIGNMENT);
            JLabel codigo = new JLabel(e.getCodigo());
            codigo.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
            codigo.setForeground(Tema.TEXTO_SUAVE);
            codigo.setAlignmentX(Component.LEFT_ALIGNMENT);

            Tema.estilizarCampo(campoPais);
            Tema.estilizarCampo(campoCiudad);
            campoPais.setText(paisActual);
            campoCiudad.setText(ciudadActual);

            chkExterior.setOpaque(false);
            chkExterior.setFont(Tema.fuente(Font.PLAIN, 13f));
            chkExterior.setForeground(Tema.TEXTO);
            chkExterior.setFocusPainted(false);
            chkExterior.setAlignmentX(Component.LEFT_ALIGNMENT);
            chkExterior.setSelected(!paisActual.isBlank() || !ciudadActual.isBlank());
            chkExterior.addActionListener(ev -> alternarExterior());

            c.add(nombre);
            c.add(Box.createVerticalStrut(2));
            c.add(codigo);
            c.add(Box.createVerticalStrut(18));
            c.add(chkExterior);
            c.add(Box.createVerticalStrut(12));
            c.add(etiqueta("País"));
            c.add(Box.createVerticalStrut(4));
            c.add(campoPais);
            c.add(Box.createVerticalStrut(10));
            c.add(etiqueta("Ciudad"));
            c.add(Box.createVerticalStrut(4));
            c.add(campoCiudad);
            c.add(Box.createVerticalStrut(20));

            JButton cancelar = Tema.boton("Cancelar", Tema.PANEL_CLARO, Tema.TEXTO);
            cancelar.addActionListener(ev -> dispose());
            JButton guardar = Tema.boton("Guardar", Tema.ACENTO, Tema.SOBRE_ACENTO);
            guardar.addActionListener(ev -> intentarGuardar());
            JPanel botones = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
            botones.setOpaque(false);
            botones.setAlignmentX(Component.LEFT_ALIGNMENT);
            botones.add(cancelar);
            botones.add(guardar);
            c.add(botones);

            setContentPane(c);
            getRootPane().setDefaultButton(guardar);
            alternarExterior();
            pack();
            setResizable(false);
            setLocationRelativeTo(padre);
        }

        private JLabel etiqueta(String texto) {
            JLabel l = new JLabel(texto);
            l.setFont(Tema.fuente(Font.BOLD, 12f));
            l.setForeground(Tema.TEXTO_SUAVE);
            l.setAlignmentX(Component.LEFT_ALIGNMENT);
            return l;
        }

        private void alternarExterior() {
            boolean on = chkExterior.isSelected();
            campoPais.setEnabled(on);
            campoCiudad.setEnabled(on);
        }

        private void intentarGuardar() {
            if (chkExterior.isSelected()
                    && (campoPais.getText().isBlank() || campoCiudad.getText().isBlank())) {
                JOptionPane.showMessageDialog(this,
                        "Si el estudiante está en el exterior, indica el país y la ciudad.",
                        "Datos incompletos", JOptionPane.WARNING_MESSAGE);
                return;
            }
            guardado = true;
            dispose();
        }

        boolean guardado() { return guardado; }
        boolean exterior() { return chkExterior.isSelected(); }
        String pais() { return campoPais.getText().trim(); }
        String ciudad() { return campoCiudad.getText().trim(); }
    }

    // ================================================================ Renderers
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

        /** Dibuja una pastilla con texto y devuelve la coordenada x donde termina. */
        protected int pastilla(Graphics2D g2, String texto, Color letra, Color fondo) {
            g2.setFont(Tema.fuente(Font.BOLD, 13f));
            FontMetrics fm = g2.getFontMetrics();
            int w = Math.min(fm.stringWidth(texto) + 28, getWidth() - 24);
            int h = 30;
            int y = (getHeight() - h) / 2;
            g2.setColor(fondo);
            g2.fillRoundRect(16, y, w, h, h, h);
            g2.setColor(letra);
            Shape clip = g2.getClip();
            g2.clipRect(16, y, w, h);
            g2.drawString(texto, 30, y + (h + fm.getAscent() - fm.getDescent()) / 2);
            g2.setClip(clip);
            return 16 + w;
        }
    }

    private static class CeldaCasilla extends Celda {
        @Override
        protected void dibujar(Graphics2D g2) {
            Casilla c = (Casilla) valor;
            int d = 20;
            int x = 18;
            int y = (getHeight() - d) / 2;
            if (c.marcada()) {
                g2.setColor(Tema.ACENTO);
                g2.fillRoundRect(x, y, d, d, 6, 6);
                g2.setColor(Color.WHITE);
                g2.setStroke(new BasicStroke(2.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g2.drawPolyline(new int[]{x + 5, x + 9, x + 15},
                        new int[]{y + 10, y + 14, y + 6}, 3);
            } else if (c.habilitada()) {
                g2.setColor(Tema.PANEL);
                g2.fillRoundRect(x, y, d, d, 6, 6);
                g2.setColor(Tema.TEXTO_SUAVE);
                g2.drawRoundRect(x, y, d - 1, d - 1, 6, 6);
            } else {
                g2.setColor(Tema.PANEL_CLARO);
                g2.fillRoundRect(x, y, d, d, 6, 6);
                g2.setColor(Tema.BORDE);
                g2.drawRoundRect(x, y, d - 1, d - 1, 6, 6);
            }
        }
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

    private static class CeldaEstado extends Celda {
        @Override
        protected void dibujar(Graphics2D g2) {
            EstadoFila e = (EstadoFila) valor;
            if (e.preRegistrado()) {
                int fin = pastilla(g2, "✔  Pre-registrado",
                        Tema.AZUL, new Color(47, 109, 181, 40));
                // La fecha va aparte, en gris, para que la pastilla no se recorte.
                g2.setFont(Tema.fuente(Font.PLAIN, 12f));
                g2.setColor(Tema.TEXTO_SUAVE);
                FontMetrics fm = g2.getFontMetrics();
                g2.drawString(fechaCorta(e.fecha()), fin + 10,
                        (getHeight() + fm.getAscent() - fm.getDescent()) / 2);
            } else {
                pastilla(g2, "○  Por pre-registrar", Tema.AMBAR, new Color(245, 158, 11, 40));
            }
        }
    }

    /** Qué pasó (o qué pasará) con la notificación de cada estudiante. */
    private static class CeldaNotificacion extends Celda {
        @Override
        protected void dibujar(Graphics2D g2) {
            NotifFila n = (NotifFila) valor;
            if (!n.tieneCorreo()) {
                pastilla(g2, "✘  Sin correo", Tema.ROJO, new Color(239, 68, 68, 45));
            } else if (n.preRegistrado()) {
                pastilla(g2, "✔  Enviada", Tema.VERDE, new Color(34, 197, 94, 45));
            } else {
                g2.setFont(Tema.fuente(Font.PLAIN, 13f));
                g2.setColor(Tema.TEXTO_SUAVE);
                FontMetrics fm = g2.getFontMetrics();
                g2.drawString("Se enviará al confirmar", 16,
                        (getHeight() + fm.getAscent() - fm.getDescent()) / 2);
            }
        }
    }
}