import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;

/**
 * Panel del Coordinador de pruebas (requerimientos 3.4 y 3.5).
 *
 * Flujo:
 *  1. El sistema muestra el listado de habilitados que envió el Director (listado_enviado.csv)
 *     y el enlace a la plataforma oficial del ICFES.
 *  2. El Coordinador carga el listado en la plataforma del ICFES (actividad externa al sistema).
 *  3. El Coordinador vuelve al sistema y confirma que el pre-registro fue realizado, indicando la fecha.
 *  4. El sistema registra la confirmación (fecha y responsable) y cambia el estado de los
 *     estudiantes a «pre-registrado» (preregistro_confirmado.csv).
 *  5. El sistema ejecuta «Notificar a los estudiantes»: guarda una notificación por estudiante en
 *     notificaciones.csv (codigo;correo;fecha_envio;asunto;mensaje;leida), de donde la leerá la
 *     página del estudiante.
 *
 * La campanita (arriba a la derecha) le recuerda al Coordinador lo que tiene pendiente.
 */
public class VentanaCoordinador extends JFrame {

    /** Enlace a la plataforma del ICFES. Cámbialo por la dirección directa de carga si la tienes. */
    static final String URL_ICFES =
            "https://www.icfes.gov.co/evaluaciones-icfes/acerca-del-examen-saber-pro/etapas-previas-examen-saber-pro/";
    static final String ASUNTO = "Pre-registro Saber Pro";
    static final String ESTADO_PRE = "PRE_REGISTRADO";
    static final String RESPONSABLE = "Coordinador de pruebas";

    private static final String CABECERA_NOTIFICACIONES = "codigo;correo;fecha_envio;asunto;mensaje;leida";
    private static final String CABECERA_PREREGISTRO =
            "codigo;estado;fecha_preregistro;responsable;fecha_confirmacion";
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final DateTimeFormatter FMT_DIA = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter FMT_VISTA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    /** Confirmación de pre-registro de un estudiante (una fila de preregistro_confirmado.csv). */
    private record Confirmacion(String fechaPreRegistro, String responsable, String fechaConfirmacion) { }

    private final String rutaCsv;
    private final Path rutaNotificaciones;
    private final Path rutaPreRegistro;

    private ListadoEnviado.Listado listado;                 // null = todavía no llega ninguno
    private Set<String> notificados = new HashSet<>();      // códigos que ya tienen su notificación
    private Map<String, Confirmacion> preRegistrados = new LinkedHashMap<>();

    private final JLabel lblRecibidos = new JLabel("0");
    private final JLabel lblPreRegistrados = new JLabel("0");
    private final JLabel lblPorPreRegistrar = new JLabel("0");
    private final JLabel lblFechaListado = new JLabel(" ");
    private final JLabel lblTotalPie = new JLabel(" ");
    private final JButton btnPlataforma = Tema.boton("Abrir plataforma del ICFES", Tema.PANEL_CLARO, Tema.TEXTO);
    private final JButton btnConfirmar = Tema.boton("Confirmar pre-registro", Tema.ACENTO, Tema.SOBRE_ACENTO);
    private final JButton btnNotificar = Tema.boton("Notificar a los estudiantes", Tema.PANEL_CLARO, Tema.TEXTO);
    private final Campana campana = new Campana();

    private final DefaultTableModel modelo = new DefaultTableModel(
            new String[]{"ESTUDIANTE", "CÓDIGO", "PROGRAMA", "PRE-REGISTRO", "NOTIFICACIÓN"}, 0) {
        @Override
        public boolean isCellEditable(int fila, int columna) {
            return false;
        }
    };
    private final CardLayout cartas = new CardLayout();
    private final JPanel centro = new JPanel(cartas);

    public VentanaCoordinador(String rutaCsv) {
        super("Saber Pro · Panel del Coordinador");
        this.rutaCsv = rutaCsv;
        Path base = Paths.get(rutaCsv).toAbsolutePath();
        this.rutaNotificaciones = base.resolveSibling("notificaciones.csv");
        this.rutaPreRegistro = base.resolveSibling("preregistro_confirmado.csv");

        setDefaultCloseOperation(EXIT_ON_CLOSE);
        getContentPane().setBackground(Tema.FONDO);
        setLayout(new BorderLayout());
        add(crearSidebar(), BorderLayout.WEST);

        JPanel derecha = new JPanel(new BorderLayout());
        derecha.setBackground(Tema.FONDO);
        derecha.add(crearBarraSuperior(), BorderLayout.NORTH);
        derecha.add(crearContenido(), BorderLayout.CENTER);
        add(derecha, BorderLayout.CENTER);

        // Al volver a la ventana se revisa si llegó un listado nuevo.
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowActivated(WindowEvent e) {
                recargar();
            }
        });

        recargar();
        setSize(1280, 820);
        setMinimumSize(new Dimension(1050, 650));
        setLocationRelativeTo(null);
    }

    // ================================================================== Datos

    /** Vuelve a leer el listado recibido, los pre-registros y las notificaciones, y refresca la pantalla. */
    void recargar() {
        try {
            listado = ListadoEnviado.leer(rutaCsv);
        } catch (IOException | RuntimeException ex) {
            System.err.println("No se pudo leer listado_enviado.csv: " + ex.getMessage());
            listado = null;
        }
        try {
            preRegistrados = leerPreRegistros();
        } catch (IOException | RuntimeException ex) {
            System.err.println("No se pudo leer preregistro_confirmado.csv: " + ex.getMessage());
            preRegistrados = new LinkedHashMap<>();
        }
        try {
            notificados = leerNotificados();
        } catch (IOException | RuntimeException ex) {
            System.err.println("No se pudo leer notificaciones.csv: " + ex.getMessage());
            notificados = new HashSet<>();
        }
        refrescarVista();
    }

    private Set<String> leerNotificados() throws IOException {
        Set<String> codigos = new HashSet<>();
        if (!Files.exists(rutaNotificaciones)) return codigos;
        List<String> lineas = Files.readAllLines(rutaNotificaciones, StandardCharsets.UTF_8);
        for (int i = 1; i < lineas.size(); i++) {
            String[] d = lineas.get(i).split(";", -1);
            if (d.length >= 4 && ASUNTO.equals(d[3].trim())) codigos.add(d[0].trim());
        }
        return codigos;
    }

    private Map<String, Confirmacion> leerPreRegistros() throws IOException {
        Map<String, Confirmacion> mapa = new LinkedHashMap<>();
        if (!Files.exists(rutaPreRegistro)) return mapa;
        List<String> lineas = Files.readAllLines(rutaPreRegistro, StandardCharsets.UTF_8);
        if (lineas.isEmpty()) return mapa;
        String[] cab = lineas.get(0).replace("﻿", "").split(";", -1);
        int iCodigo = indice(cab, "codigo");
        int iEstado = indice(cab, "estado");
        int iFecha = indice(cab, "fecha_preregistro");
        int iResp = indice(cab, "responsable");
        int iConf = indice(cab, "fecha_confirmacion");
        if (iCodigo < 0) return mapa;
        for (int i = 1; i < lineas.size(); i++) {
            String linea = lineas.get(i).trim();
            if (linea.isEmpty()) continue;
            String[] d = linea.split(";", -1);
            String codigo = campo(d, iCodigo);
            if (codigo.isEmpty() || !ESTADO_PRE.equals(campo(d, iEstado))) continue;
            mapa.put(codigo, new Confirmacion(campo(d, iFecha), campo(d, iResp), campo(d, iConf)));
        }
        return mapa;
    }

    private void guardarPreRegistros(Map<String, Confirmacion> mapa) throws IOException {
        List<String> lineas = new ArrayList<>();
        lineas.add(CABECERA_PREREGISTRO);
        for (Map.Entry<String, Confirmacion> en : mapa.entrySet()) {
            Confirmacion c = en.getValue();
            lineas.add(String.join(";", limpiar(en.getKey()), ESTADO_PRE, limpiar(c.fechaPreRegistro()),
                    limpiar(c.responsable()), limpiar(c.fechaConfirmacion())));
        }
        Files.createDirectories(rutaPreRegistro.getParent());
        Files.write(rutaPreRegistro, lineas, StandardCharsets.UTF_8);
    }

    private static int indice(String[] cab, String nombre) {
        for (int i = 0; i < cab.length; i++) {
            if (cab[i].trim().equalsIgnoreCase(nombre)) return i;
        }
        return -1;
    }

    private static String campo(String[] d, int i) {
        return i < 0 || i >= d.length ? "" : d[i].trim();
    }

    /** Estudiantes del listado recibido que todavía no están pre-registrados. */
    List<ListadoEnviado.Fila> porPreRegistrar() {
        List<ListadoEnviado.Fila> lista = new ArrayList<>();
        if (listado == null) return lista;
        for (ListadoEnviado.Fila f : listado.filas()) {
            if (!preRegistrados.containsKey(f.codigo())) lista.add(f);
        }
        return lista;
    }

    /** Estudiantes pre-registrados (del listado recibido) que todavía no tienen su notificación. */
    List<ListadoEnviado.Fila> porNotificar() {
        List<ListadoEnviado.Fila> lista = new ArrayList<>();
        if (listado == null) return lista;
        for (ListadoEnviado.Fila f : listado.filas()) {
            if (preRegistrados.containsKey(f.codigo()) && !notificados.contains(f.codigo())) lista.add(f);
        }
        return lista;
    }

    /**
     * Pasos 4 y 5 del caso de uso: registra la confirmación (fecha del pre-registro y responsable),
     * cambia el estado de los estudiantes a «pre-registrado» y ejecuta «Notificar a los estudiantes».
     * Devuelve cuántos estudiantes quedaron pre-registrados.
     */
    int confirmarPreRegistro(LocalDate fechaPreRegistro) throws IOException {
        List<ListadoEnviado.Fila> lista = porPreRegistrar();
        if (lista.isEmpty()) return 0;

        String ahora = LocalDateTime.now().format(FMT);
        Map<String, Confirmacion> nuevo = new LinkedHashMap<>(preRegistrados);
        for (ListadoEnviado.Fila f : lista) {
            nuevo.put(f.codigo(), new Confirmacion(fechaPreRegistro.format(FMT_DIA), RESPONSABLE, ahora));
        }
        guardarPreRegistros(nuevo);   // si falla, no se cambia nada
        preRegistrados = nuevo;

        try {
            notificarPendientes();
        } catch (IOException ex) {
            throw new IOException("El pre-registro quedó guardado, pero no se pudieron registrar las "
                    + "notificaciones. Usa «Notificar a los estudiantes» para reintentarlo.\n"
                    + ex.getMessage(), ex);
        }
        return lista.size();
    }

    /**
     * Caso de uso «Notificar a los estudiantes»: registra la notificación de cada estudiante
     * pre-registrado que todavía no la tiene. Devuelve cuántas se registraron.
     */
    int notificarPendientes() throws IOException {
        List<ListadoEnviado.Fila> pendientes = porNotificar();
        if (pendientes.isEmpty()) return 0;

        String ahora = LocalDateTime.now().format(FMT);
        List<String> lineas = new ArrayList<>();
        if (Files.exists(rutaNotificaciones)) {
            lineas.addAll(Files.readAllLines(rutaNotificaciones, StandardCharsets.UTF_8));
        }
        if (lineas.isEmpty()) lineas.add(CABECERA_NOTIFICACIONES);
        for (ListadoEnviado.Fila f : pendientes) {
            lineas.add(String.join(";", limpiar(f.codigo()), limpiar(f.correo()), ahora, ASUNTO,
                    limpiar(mensaje(f, preRegistrados.get(f.codigo()))), "false"));
        }
        Files.createDirectories(rutaNotificaciones.getParent());
        Files.write(rutaNotificaciones, lineas, StandardCharsets.UTF_8);
        notificados.addAll(pendientes.stream().map(ListadoEnviado.Fila::codigo).toList());
        return pendientes.size();
    }

    private static String mensaje(ListadoEnviado.Fila f, Confirmacion c) {
        String fecha = c == null ? "" : " el " + fechaVista(c.fechaPreRegistro());
        return "Hola " + f.nombre() + ". Fuiste habilitado(a) para presentar las pruebas Saber Pro "
                + "y la Universidad de los Llanos realizó tu pre-registro ante el ICFES" + fecha + ". "
                + "El ICFES te enviará un correo con tu usuario y contraseña provisional "
                + "para que continúes con el registro y el pago. "
                + "Revisa tu correo y mantén tus datos actualizados.";
    }

    /** Avisos de la campanita: título y detalle. */
    List<String[]> avisos() {
        List<String[]> lista = new ArrayList<>();
        List<ListadoEnviado.Fila> por = porPreRegistrar();
        if (!por.isEmpty()) {
            lista.add(new String[]{
                    "Tienes un listado por pre-registrar",
                    "Recibiste el listado de " + listado.filas().size() + " habilitados el "
                            + ListadoEnviado.fechaLegible(listado.fechaEnvio()) + ". "
                            + (por.size() == 1 ? "Falta 1 estudiante" : "Faltan " + por.size() + " estudiantes")
                            + " por pre-registrar: cárgalos en el ICFES y confirma el pre-registro; "
                            + "después se notificará a los estudiantes."
            });
        }
        List<ListadoEnviado.Fila> sinNotificar = porNotificar();
        if (!sinNotificar.isEmpty()) {
            lista.add(new String[]{
                    "Recuerda notificar a los estudiantes",
                    (sinNotificar.size() == 1 ? "1 estudiante pre-registrado todavía no fue notificado."
                                              : sinNotificar.size() + " estudiantes pre-registrados todavía no fueron notificados.")
            });
        }
        return lista;
    }

    private static String limpiar(String s) {
        return s == null ? "" : s.replace(';', ',').replace('\r', ' ').replace('\n', ' ').trim();
    }

    /** «yyyy-MM-dd» → «dd/MM/yyyy» (si no se puede convertir, lo devuelve igual). */
    private static String fechaVista(String guardada) {
        try {
            return LocalDate.parse(guardada, FMT_DIA).format(FMT_VISTA);
        } catch (RuntimeException ex) {
            return guardada;
        }
    }

    // ================================================================ Sidebar
    private JPanel crearSidebar() {
        JPanel lado = new JPanel(new BorderLayout());
        lado.setBackground(Tema.PANEL);
        lado.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 0, 1, Tema.BORDE),
                BorderFactory.createEmptyBorder(26, 18, 18, 18)));
        lado.setPreferredSize(new Dimension(280, 0));

        JLabel logo = new JLabel("Saber Pro");
        logo.setFont(new Font("Segoe UI", Font.BOLD, 26));
        logo.setForeground(Tema.ACENTO);
        logo.setAlignmentX(Component.LEFT_ALIGNMENT);
        JLabel sub = new JLabel("Gestión de pruebas");
        sub.setFont(Tema.fuente(Font.PLAIN, 12f));
        sub.setForeground(Tema.TEXTO_SUAVE);
        sub.setAlignmentX(Component.LEFT_ALIGNMENT);
        JLabel seccion = new JLabel("COORDINACIÓN");
        seccion.setFont(Tema.fuente(Font.BOLD, 11f));
        seccion.setForeground(Tema.TEXTO_SUAVE);
        seccion.setAlignmentX(Component.LEFT_ALIGNMENT);

        Tema.Redondeado item = new Tema.Redondeado(Tema.PANEL_CLARO, 12);
        item.setLayout(new BorderLayout());
        item.setBorder(BorderFactory.createEmptyBorder(10, 12, 10, 12));
        item.setAlignmentX(Component.LEFT_ALIGNMENT);
        item.setMaximumSize(new Dimension(Integer.MAX_VALUE, 48));
        JLabel textoItem = new JLabel("Pre-registro ICFES");
        textoItem.setFont(Tema.fuente(Font.BOLD, 13f));
        textoItem.setForeground(Tema.TEXTO);
        item.add(textoItem, BorderLayout.WEST);

        JPanel menu = new JPanel();
        menu.setLayout(new BoxLayout(menu, BoxLayout.Y_AXIS));
        menu.setOpaque(false);
        menu.add(logo);
        menu.add(sub);
        menu.add(Box.createVerticalStrut(30));
        menu.add(seccion);
        menu.add(Box.createVerticalStrut(10));
        menu.add(item);

        // Usuario y cerrar sesión: siempre abajo a la izquierda
        JLabel quien = new JLabel("Coordinador de pruebas");
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
        JLabel ini = new JLabel("CP", SwingConstants.CENTER);
        ini.setFont(Tema.fuente(Font.BOLD, 13f));
        ini.setForeground(Tema.TEXTO);
        avatar.add(ini);

        JPanel usuario = new JPanel(new BorderLayout(12, 0));
        usuario.setOpaque(false);
        usuario.add(avatar, BorderLayout.WEST);
        usuario.add(textos, BorderLayout.CENTER);
        usuario.setAlignmentX(Component.LEFT_ALIGNMENT);
        usuario.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));

        JButton salir = Tema.boton("Cerrar sesión", Tema.PANEL_CLARO, Tema.TEXTO_SUAVE);
        salir.setAlignmentX(Component.LEFT_ALIGNMENT);
        salir.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        salir.addActionListener(ev -> cerrarSesion());

        JPanel pie = new JPanel();
        pie.setLayout(new BoxLayout(pie, BoxLayout.Y_AXIS));
        pie.setOpaque(false);
        pie.add(usuario);
        pie.add(Box.createVerticalStrut(14));
        pie.add(salir);

        lado.add(menu, BorderLayout.CENTER);
        lado.add(pie, BorderLayout.SOUTH);
        return lado;
    }

    private void cerrarSesion() {
        new LoginDirector(rutaCsv).setVisible(true);
        dispose();
    }

    // ============================================================ Barra superior
    private JPanel crearBarraSuperior() {
        JPanel barra = new JPanel(new BorderLayout());
        barra.setOpaque(false);
        barra.setBorder(BorderFactory.createEmptyBorder(16, 32, 0, 32));
        campana.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent ev) {
                mostrarAvisos();
            }
        });
        barra.add(campana, BorderLayout.EAST);
        return barra;
    }

    private void mostrarAvisos() {
        recargar();
        List<String[]> avisos = avisos();

        JPanel c = new JPanel();
        c.setLayout(new BoxLayout(c, BoxLayout.Y_AXIS));
        c.setBackground(Tema.PANEL);
        c.setBorder(BorderFactory.createEmptyBorder(14, 16, 14, 16));

        JLabel titulo = new JLabel("Notificaciones");
        titulo.setFont(Tema.fuente(Font.BOLD, 15f));
        titulo.setForeground(Tema.TEXTO);
        titulo.setAlignmentX(Component.LEFT_ALIGNMENT);
        c.add(titulo);
        c.add(Box.createVerticalStrut(10));

        if (avisos.isEmpty()) {
            c.add(texto("No tienes notificaciones.", Font.PLAIN, 13f, Tema.TEXTO_SUAVE));
        }
        for (String[] a : avisos) {
            JPanel tarjeta = new Tema.Redondeado(Tema.PANEL_CLARO, 12);
            tarjeta.setLayout(new BoxLayout(tarjeta, BoxLayout.Y_AXIS));
            tarjeta.setBorder(BorderFactory.createEmptyBorder(10, 12, 10, 12));
            tarjeta.setAlignmentX(Component.LEFT_ALIGNMENT);
            tarjeta.add(texto(a[0], Font.BOLD, 13f, Tema.TEXTO));
            tarjeta.add(Box.createVerticalStrut(4));
            JLabel detalle = new JLabel("<html><div style='width:240px'>" + a[1] + "</div></html>");
            detalle.setFont(Tema.fuente(Font.PLAIN, 12f));
            detalle.setForeground(Tema.TEXTO_SUAVE);
            detalle.setAlignmentX(Component.LEFT_ALIGNMENT);
            tarjeta.add(detalle);
            c.add(tarjeta);
            c.add(Box.createVerticalStrut(8));
        }

        JPopupMenu menu = new JPopupMenu();
        menu.setBorder(BorderFactory.createLineBorder(Tema.BORDE));
        menu.add(c);
        c.setPreferredSize(new Dimension(340, c.getPreferredSize().height));
        menu.show(campana, campana.getWidth() - 340, campana.getHeight() + 4);
    }

    private JLabel texto(String t, int estilo, float tam, Color color) {
        JLabel l = new JLabel(t);
        l.setFont(Tema.fuente(estilo, tam));
        l.setForeground(color);
        l.setAlignmentX(Component.LEFT_ALIGNMENT);
        return l;
    }

    // ================================================================ Contenido
    private JPanel crearContenido() {
        JPanel c = new JPanel(new BorderLayout(0, 16));
        c.setBackground(Tema.FONDO);
        c.setBorder(BorderFactory.createEmptyBorder(0, 32, 22, 32));

        JLabel kicker = texto("PANEL DEL COORDINADOR · INGENIERÍA DE SISTEMAS", Font.BOLD, 11f, Tema.ACENTO);
        JLabel titulo = new JLabel("Pre-registro ICFES");
        titulo.setFont(new Font("Segoe UI", Font.BOLD, 34));
        titulo.setForeground(Tema.TEXTO);
        titulo.setAlignmentX(Component.LEFT_ALIGNMENT);
        JLabel sub = texto("Carga el listado del Director en la plataforma del ICFES y confirma aquí el "
                + "pre-registro; luego se notifica a los estudiantes.", Font.PLAIN, 13f, Tema.TEXTO_SUAVE);
        lblFechaListado.setFont(Tema.fuente(Font.BOLD, 12f));
        lblFechaListado.setForeground(Tema.TEXTO);
        lblFechaListado.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel textos = new JPanel();
        textos.setLayout(new BoxLayout(textos, BoxLayout.Y_AXIS));
        textos.setOpaque(false);
        textos.add(kicker);
        textos.add(Box.createVerticalStrut(6));
        textos.add(titulo);
        textos.add(Box.createVerticalStrut(4));
        textos.add(sub);
        textos.add(Box.createVerticalStrut(10));
        textos.add(lblFechaListado);

        JPanel stats = new JPanel(new GridLayout(1, 3, 14, 0));
        stats.setOpaque(false);
        stats.add(tarjetaStat("Habilitados recibidos", lblRecibidos, Tema.AZUL));
        stats.add(tarjetaStat("Pre-registrados", lblPreRegistrados, Tema.VERDE));
        stats.add(tarjetaStat("Por pre-registrar", lblPorPreRegistrar, Tema.AMBAR));
        stats.setPreferredSize(new Dimension(0, 78));

        JPanel cabecera = new JPanel(new BorderLayout(0, 14));
        cabecera.setOpaque(false);
        cabecera.add(textos, BorderLayout.NORTH);
        cabecera.add(stats, BorderLayout.CENTER);
        c.add(cabecera, BorderLayout.NORTH);

        c.add(crearCentro(), BorderLayout.CENTER);
        c.add(crearPie(), BorderLayout.SOUTH);
        return c;
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

    private JPanel crearCentro() {
        centro.setOpaque(false);

        JTable tabla = new JTable(modelo);
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
        tabla.getColumnModel().getColumn(1).setCellRenderer(new CeldaCodigo());
        tabla.getColumnModel().getColumn(2).setCellRenderer(new CeldaTexto());
        tabla.getColumnModel().getColumn(3).setCellRenderer(new CeldaPreRegistro());
        tabla.getColumnModel().getColumn(4).setCellRenderer(new CeldaNotificacion());
        int[] anchos = {290, 120, 230, 280, 190};
        for (int i = 0; i < anchos.length; i++) {
            tabla.getColumnModel().getColumn(i).setPreferredWidth(anchos[i]);
        }

        JScrollPane scroll = new JScrollPane(tabla);
        scroll.setBorder(BorderFactory.createLineBorder(Tema.BORDE));
        scroll.getViewport().setBackground(Tema.PANEL);
        scroll.getVerticalScrollBar().setUnitIncrement(18);

        Tema.Redondeado vacio = new Tema.Redondeado(Tema.PANEL, 18);
        vacio.setLayout(new GridBagLayout());
        JPanel msg = new JPanel();
        msg.setLayout(new BoxLayout(msg, BoxLayout.Y_AXIS));
        msg.setOpaque(false);
        JLabel m1 = new JLabel("Aún no has recibido ningún listado");
        m1.setFont(Tema.fuente(Font.BOLD, 18f));
        m1.setForeground(Tema.TEXTO);
        m1.setAlignmentX(Component.CENTER_ALIGNMENT);
        JLabel m2 = new JLabel("El Director del programa lo envía desde «Listado de habilitados».");
        m2.setFont(Tema.fuente(Font.PLAIN, 13f));
        m2.setForeground(Tema.TEXTO_SUAVE);
        m2.setAlignmentX(Component.CENTER_ALIGNMENT);
        msg.add(m1);
        msg.add(Box.createVerticalStrut(6));
        msg.add(m2);
        vacio.add(msg);

        centro.add(scroll, "tabla");
        centro.add(vacio, "vacio");
        return centro;
    }

    private JPanel crearPie() {
        lblTotalPie.setFont(Tema.fuente(Font.PLAIN, 12f));
        lblTotalPie.setForeground(Tema.TEXTO_SUAVE);

        btnPlataforma.addActionListener(ev -> abrirPlataforma());
        btnConfirmar.addActionListener(ev -> confirmar());
        btnNotificar.addActionListener(ev -> notificar());

        JPanel botones = new JPanel();
        botones.setLayout(new BoxLayout(botones, BoxLayout.X_AXIS));
        botones.setOpaque(false);
        botones.add(btnNotificar);   // solo se ve si quedó alguien pre-registrado sin notificar
        botones.add(Box.createHorizontalStrut(8));
        botones.add(btnPlataforma);
        botones.add(Box.createHorizontalStrut(8));
        botones.add(btnConfirmar);

        JPanel pie = new JPanel(new BorderLayout());
        pie.setOpaque(false);
        pie.add(lblTotalPie, BorderLayout.WEST);
        pie.add(botones, BorderLayout.EAST);
        return pie;
    }

    // ============================================================== Refrescar
    private void refrescarVista() {
        modelo.setRowCount(0);
        int total = 0;
        int pre = 0;
        if (listado != null) {
            for (ListadoEnviado.Fila f : listado.filas()) {
                Confirmacion c = preRegistrados.get(f.codigo());
                boolean esta = c != null;
                total++;
                if (esta) pre++;
                modelo.addRow(new Object[]{
                        f, f.codigo(), f.programa(),
                        new EstadoFila(esta, esta ? fechaVista(c.fechaPreRegistro()) : ""),
                        new NotifFila(esta, notificados.contains(f.codigo()))
                });
            }
        }
        lblRecibidos.setText(String.valueOf(total));
        lblPreRegistrados.setText(String.valueOf(pre));
        lblPorPreRegistrar.setText(String.valueOf(total - pre));

        lblFechaListado.setText(listado == null
                ? "Sin listado recibido"
                : "Listado recibido el " + ListadoEnviado.fechaLegible(listado.fechaEnvio())
                  + " del Director del programa");
        lblTotalPie.setText(listado == null ? " " : "Total de estudiantes habilitados: " + total);

        btnConfirmar.setEnabled(total - pre > 0);
        btnNotificar.setVisible(!porNotificar().isEmpty());
        campana.setCantidad(avisos().size());
        cartas.show(centro, listado == null ? "vacio" : "tabla");
    }

    // ================================================================ Acciones

    /** Paso 1: enlace a la plataforma oficial del ICFES. */
    private void abrirPlataforma() {
        try {
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                Desktop.getDesktop().browse(new URI(URL_ICFES));
                return;
            }
        } catch (IOException | URISyntaxException | RuntimeException ex) {
            // Se cae al plan B: copiar el enlace.
        }
        try {
            Toolkit.getDefaultToolkit().getSystemClipboard()
                    .setContents(new StringSelection(URL_ICFES), null);
        } catch (RuntimeException ex) {
            // Si tampoco se puede copiar, el enlace igual se muestra abajo.
        }
        JOptionPane.showMessageDialog(this,
                "No se pudo abrir el navegador.\nEl enlace quedó copiado; pégalo en tu navegador:\n\n" + URL_ICFES,
                "Plataforma del ICFES", JOptionPane.INFORMATION_MESSAGE);
    }

    /** Pasos 3 a 5: el Coordinador confirma el pre-registro indicando la fecha. */
    private void confirmar() {
        int n = porPreRegistrar().size();
        if (n == 0) return;
        DialogoFecha dlg = new DialogoFecha(this, n);
        dlg.setVisible(true);
        LocalDate fecha = dlg.fecha();
        if (fecha == null) return;

        try {
            int hechos = confirmarPreRegistro(fecha);
            recargar();
            JOptionPane.showMessageDialog(this,
                    "Pre-registro confirmado para " + hechos + (hechos == 1 ? " estudiante" : " estudiantes")
                            + ".\nSe notificó a cada estudiante.\n\n"
                            + "El ICFES enviará a cada uno un correo con sus credenciales para continuar con "
                            + "el registro y el pago.");
        } catch (IOException ex) {
            recargar();
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    /** Reintento de «Notificar a los estudiantes» (solo aparece si una notificación no se pudo guardar). */
    private void notificar() {
        try {
            int hechas = notificarPendientes();
            recargar();
            JOptionPane.showMessageDialog(this, hechas == 1
                    ? "Se notificó a 1 estudiante."
                    : "Se notificó a " + hechas + " estudiantes.");
        } catch (IOException ex) {
            recargar();
            JOptionPane.showMessageDialog(this,
                    "No se pudieron guardar las notificaciones:\n" + ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    // ================================================== Diálogo «Confirmar pre-registro»
    private static final class DialogoFecha extends JDialog {
        private final JTextField campoFecha =
                new JTextField(LocalDate.now().format(FMT_VISTA), 12);
        private final JLabel lblError = new JLabel(" ");
        private LocalDate fecha = null;

        DialogoFecha(Window padre, int cantidad) {
            super(padre, "Confirmar pre-registro", ModalityType.APPLICATION_MODAL);

            JPanel c = new JPanel();
            c.setLayout(new BoxLayout(c, BoxLayout.Y_AXIS));
            c.setBackground(Tema.PANEL);
            c.setBorder(BorderFactory.createEmptyBorder(22, 26, 22, 26));

            JLabel t1 = etiqueta("Confirmar pre-registro", Font.BOLD, 17f, Tema.TEXTO);
            JLabel t2 = new JLabel("<html><div style='width:330px'>Confirma que cargaste el listado en el ICFES y que el "
                    + "pre-registro de <b>" + cantidad + (cantidad == 1 ? " estudiante" : " estudiantes")
                    + "</b> fue realizado. Después se notificará a cada estudiante.</div></html>");
            t2.setFont(Tema.fuente(Font.PLAIN, 13f));
            t2.setForeground(Tema.TEXTO_SUAVE);
            t2.setAlignmentX(Component.LEFT_ALIGNMENT);

            Tema.estilizarCampo(campoFecha);
            campoFecha.setMaximumSize(new Dimension(200, 44));

            lblError.setFont(Tema.fuente(Font.BOLD, 12f));
            lblError.setForeground(Tema.ROJO);
            lblError.setAlignmentX(Component.LEFT_ALIGNMENT);

            JButton cancelar = Tema.boton("Cancelar", Tema.PANEL_CLARO, Tema.TEXTO);
            cancelar.addActionListener(ev -> dispose());
            JButton confirmar = Tema.boton("Confirmar", Tema.ACENTO, Tema.SOBRE_ACENTO);
            confirmar.addActionListener(ev -> intentarConfirmar());
            JPanel botones = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
            botones.setOpaque(false);
            botones.setAlignmentX(Component.LEFT_ALIGNMENT);
            botones.add(cancelar);
            botones.add(confirmar);

            c.add(t1);
            c.add(Box.createVerticalStrut(8));
            c.add(t2);
            c.add(Box.createVerticalStrut(18));
            c.add(etiqueta("Fecha del pre-registro (dd/mm/aaaa)", Font.BOLD, 12f, Tema.TEXTO_SUAVE));
            c.add(Box.createVerticalStrut(4));
            c.add(campoFecha);
            c.add(Box.createVerticalStrut(6));
            c.add(lblError);
            c.add(Box.createVerticalStrut(12));
            c.add(botones);

            setContentPane(c);
            getRootPane().setDefaultButton(confirmar);
            pack();
            setResizable(false);
            setLocationRelativeTo(padre);
        }

        private JLabel etiqueta(String t, int estilo, float tam, Color color) {
            JLabel l = new JLabel(t);
            l.setFont(Tema.fuente(estilo, tam));
            l.setForeground(color);
            l.setAlignmentX(Component.LEFT_ALIGNMENT);
            return l;
        }

        private void intentarConfirmar() {
            try {
                LocalDate f = LocalDate.parse(campoFecha.getText().trim(), FMT_VISTA);
                if (f.isAfter(LocalDate.now())) {
                    lblError.setText("La fecha no puede ser futura.");
                    return;
                }
                fecha = f;
                dispose();
            } catch (DateTimeParseException ex) {
                lblError.setText("Escribe la fecha como dd/mm/aaaa, por ejemplo 22/10/2026.");
            }
        }

        LocalDate fecha() {
            return fecha;
        }
    }

    // ================================================================ Campanita
    /** Campana dibujada con formas (no usa símbolos de fuente) con un globo rojo para el conteo. */
    private static final class Campana extends JComponent {
        private int cantidad = 0;
        private boolean encima = false;

        Campana() {
            setPreferredSize(new Dimension(44, 44));
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent e) {
                    encima = true;
                    repaint();
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    encima = false;
                    repaint();
                }
            });
        }

        void setCantidad(int c) {
            cantidad = c;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int w = getWidth();
            int h = getHeight();

            g2.setColor(encima ? Tema.PANEL_CLARO : new Color(0, 0, 0, 0));
            g2.fillOval(0, 0, w, h);

            // Campana en un espacio de 24 x 24, centrada y escalada
            double escala = Math.min(w, h) / 44.0 * 1.25;
            g2.translate((w - 24 * escala) / 2.0, (h - 24 * escala) / 2.0);
            g2.scale(escala, escala);
            g2.setColor(cantidad > 0 ? Tema.TEXTO : Tema.TEXTO_SUAVE);

            Path2D cuerpo = new Path2D.Double();
            cuerpo.moveTo(4, 17.5);
            cuerpo.curveTo(6.2, 15.5, 6.2, 12.5, 6.2, 9.5);
            cuerpo.curveTo(6.2, 6, 8.6, 3.8, 12, 3.8);
            cuerpo.curveTo(15.4, 3.8, 17.8, 6, 17.8, 9.5);
            cuerpo.curveTo(17.8, 12.5, 17.8, 15.5, 20, 17.5);
            cuerpo.closePath();
            g2.fill(cuerpo);
            g2.fill(new Ellipse2D.Double(11, 1.8, 2, 2.4));
            g2.fill(new Ellipse2D.Double(9.4, 18.6, 5.2, 3.2));
            g2.dispose();

            if (cantidad > 0) {
                Graphics2D g3 = (Graphics2D) g.create();
                g3.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g3.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                int d = 18;
                int x = w - d - 2;
                int y = 2;
                g3.setColor(Tema.ROJO);
                g3.fillOval(x, y, d, d);
                g3.setColor(Color.WHITE);
                g3.setFont(Tema.fuente(Font.BOLD, 11f));
                String s = cantidad > 9 ? "9+" : String.valueOf(cantidad);
                FontMetrics fm = g3.getFontMetrics();
                g3.drawString(s, x + (d - fm.stringWidth(s)) / 2, y + (d + fm.getAscent() - fm.getDescent()) / 2);
                g3.dispose();
            }
        }
    }

    // ============================================================== Renderers
    private record EstadoFila(boolean preRegistrado, String fecha) { }

    private record NotifFila(boolean preRegistrado, boolean notificado) { }

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

        /** Pastilla con un punto dibujado; devuelve la coordenada x donde termina. */
        protected int pastilla(Graphics2D g2, String texto, Color fondo, Color letra) {
            g2.setFont(Tema.fuente(Font.BOLD, 13f));
            FontMetrics fm = g2.getFontMetrics();
            int w = fm.stringWidth(texto) + 46;
            int h = 30;
            int y = (getHeight() - h) / 2;
            g2.setColor(fondo);
            g2.fillRoundRect(16, y, w, h, h, h);
            g2.setColor(letra);
            g2.fillOval(30, y + (h - 8) / 2, 8, 8);
            g2.drawString(texto, 46, y + (h + fm.getAscent() - fm.getDescent()) / 2);
            return 16 + w;
        }

        protected void textoSuave(Graphics2D g2, String s, int x) {
            g2.setFont(Tema.fuente(Font.PLAIN, 13f));
            g2.setColor(Tema.TEXTO_SUAVE);
            FontMetrics fm = g2.getFontMetrics();
            g2.drawString(s, x, (getHeight() + fm.getAscent() - fm.getDescent()) / 2);
        }
    }

    private static class CeldaEstudiante extends Celda {
        @Override
        protected void dibujar(Graphics2D g2) {
            ListadoEnviado.Fila e = (ListadoEnviado.Fila) valor;
            int h = getHeight();
            int d = 38;
            int y = (h - d) / 2;
            g2.setColor(Tema.AVATAR_FONDO);
            g2.fillOval(16, y, d, d);

            String[] p = e.nombre().trim().split("\\s+");
            String ini = p[0].isEmpty() ? "?" : "" + p[0].charAt(0) + (p.length > 1 ? p[1].charAt(0) : "");
            g2.setFont(Tema.fuente(Font.BOLD, 13f));
            g2.setColor(Tema.AVATAR_TEXTO);
            FontMetrics fm = g2.getFontMetrics();
            g2.drawString(ini, 16 + (d - fm.stringWidth(ini)) / 2,
                    y + (d + fm.getAscent() - fm.getDescent()) / 2);

            g2.setFont(Tema.fuente(Font.BOLD, 14f));
            g2.setColor(Tema.TEXTO);
            fm = g2.getFontMetrics();
            g2.drawString(e.nombre(), 64, (h + fm.getAscent() - fm.getDescent()) / 2);
        }
    }

    private static class CeldaCodigo extends Celda {
        @Override
        protected void dibujar(Graphics2D g2) {
            g2.setFont(new Font(Font.MONOSPACED, Font.BOLD, 13));
            g2.setColor(Tema.TEXTO);
            FontMetrics fm = g2.getFontMetrics();
            g2.drawString(String.valueOf(valor), 16,
                    (getHeight() + fm.getAscent() - fm.getDescent()) / 2);
        }
    }

    private static class CeldaTexto extends Celda {
        @Override
        protected void dibujar(Graphics2D g2) {
            String s = valor == null || valor.toString().isEmpty() ? "—" : valor.toString();
            g2.setFont(Tema.fuente(Font.PLAIN, 13f));
            g2.setColor(Tema.TEXTO_SUAVE);
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

    /** «Por pre-registrar» (ámbar) o «Pre-registrado» (azul) con la fecha al lado. */
    private static class CeldaPreRegistro extends Celda {
        @Override
        protected void dibujar(Graphics2D g2) {
            EstadoFila e = (EstadoFila) valor;
            if (e.preRegistrado()) {
                int fin = pastilla(g2, "Pre-registrado", new Color(0xDCE8F7), Tema.AZUL);
                textoSuave(g2, e.fecha(), fin + 10);
            } else {
                pastilla(g2, "Por pre-registrar", new Color(0xFDEBD3), Tema.AMBAR);
            }
        }
    }

    /** «Notificado» (verde), «Sin notificar» (gris) o un aviso de que se enviará al confirmar. */
    private static class CeldaNotificacion extends Celda {
        @Override
        protected void dibujar(Graphics2D g2) {
            NotifFila n = (NotifFila) valor;
            if (n.notificado()) {
                pastilla(g2, "Notificado", new Color(34, 197, 94, 45), Tema.VERDE);
            } else if (n.preRegistrado()) {
                pastilla(g2, "Sin notificar", Tema.PANEL_CLARO, Tema.TEXTO_SUAVE);
            } else {
                textoSuave(g2, "Se enviará al confirmar", 16);
            }
        }
    }
}