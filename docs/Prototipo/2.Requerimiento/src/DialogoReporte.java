import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.*;

public class DialogoReporte extends JDialog {
    public enum Accion { CERRAR, GUARDAR, GUARDAR_Y_SIGUIENTE }

    private Accion accion = Accion.CERRAR;
    private final boolean veredictoHabilitado;
    private boolean eligioHabilitado;
    private final Opcion opHab;
    private final Opcion opNo;
    private final JTextArea txtObs = new JTextArea(4, 34);

    public DialogoReporte(JFrame owner, Estudiante e, VerificadorRequisitos.Resultado r,
                          boolean haySiguiente) {
        super(owner, "Reporte de verificación", true);
        veredictoHabilitado = r.habilitado();
        eligioHabilitado = e.estaEvaluado()
                ? Estudiante.HABILITADO.equals(e.getCondicion())
                : veredictoHabilitado;

        JPanel c = new JPanel();
        c.setLayout(new BoxLayout(c, BoxLayout.Y_AXIS));
        c.setBackground(Tema.PANEL);
        c.setBorder(BorderFactory.createEmptyBorder(26, 30, 24, 30));

        // ---- Encabezado con avatar
        JPanel cab = new JPanel(new BorderLayout(14, 0));
        cab.setOpaque(false);
        cab.add(new Avatar(e.getNombre()), BorderLayout.WEST);
        JPanel datos = new JPanel(new GridLayout(2, 1));
        datos.setOpaque(false);
        datos.add(etiqueta(e.getNombre(), Font.BOLD, 20f, Tema.TEXTO));
        datos.add(etiqueta(e.getCodigo() + " · " + e.getPrograma(), Font.PLAIN, 13f, Tema.TEXTO_SUAVE));
        cab.add(datos, BorderLayout.CENTER);
        JPanel prom = new JPanel(new GridBagLayout());
        prom.setOpaque(false);
        prom.add(new Tema.Pildora(String.format("Promedio %.1f", e.getPromedio()),
                Tema.AVATAR_FONDO, Tema.AVATAR_TEXTO));
        cab.add(prom, BorderLayout.EAST);
        cab.setAlignmentX(Component.LEFT_ALIGNMENT);
        cab.setMaximumSize(new Dimension(Integer.MAX_VALUE, 60));
        c.add(cab);
        c.add(Box.createVerticalStrut(22));

        // ---- Requisitos
        c.add(etiqueta("REQUISITOS", Font.BOLD, 11f, Tema.TEXTO_SUAVE));
        c.add(Box.createVerticalStrut(4));
        for (VerificadorRequisitos.Item it : r.items) {
            c.add(fila(it));
        }
        c.add(Box.createVerticalStrut(16));

        // ---- Propuesta del sistema
        boolean ok = r.habilitado();
        Tema.Redondeado banner = new Tema.Redondeado(
                ok ? new Color(34, 197, 94, 45) : new Color(239, 68, 68, 45), 14);
        banner.setLayout(new BorderLayout());
        JLabel txt = new JLabel(ok ? "El sistema propone:  HABILITADO"
                : "El sistema propone:  NO HABILITADO  (" + r.pendientes() + " pendiente(s))");
        txt.setFont(Tema.fuente(Font.BOLD, 14f));
        txt.setForeground(ok ? Tema.VERDE : Tema.ROJO);
        txt.setBorder(BorderFactory.createEmptyBorder(12, 16, 12, 16));
        banner.add(txt, BorderLayout.CENTER);
        banner.setAlignmentX(Component.LEFT_ALIGNMENT);
        banner.setMaximumSize(new Dimension(Integer.MAX_VALUE, 48));
        c.add(banner);
        c.add(Box.createVerticalStrut(22));

        // ---- Decisión del director
        c.add(etiqueta("DECISIÓN DEL DIRECTOR", Font.BOLD, 11f, Tema.TEXTO_SUAVE));
        c.add(Box.createVerticalStrut(8));

        opHab = new Opcion("✔  Habilitado", Tema.VERDE, () -> elegir(true));
        opNo = new Opcion("✘  No habilitado", Tema.ROJO, () -> elegir(false));
        elegir(eligioHabilitado);

        JPanel opciones = new JPanel(new GridLayout(1, 2, 12, 0));
        opciones.setOpaque(false);
        opciones.add(opHab);
        opciones.add(opNo);
        opciones.setAlignmentX(Component.LEFT_ALIGNMENT);
        opciones.setMaximumSize(new Dimension(Integer.MAX_VALUE, 52));
        c.add(opciones);
        c.add(Box.createVerticalStrut(8));
        c.add(etiqueta("Si cambias la condición propuesta por el sistema, debes escribir el motivo.",
                Font.PLAIN, 12f, Tema.TEXTO_SUAVE));
        c.add(Box.createVerticalStrut(14));

        // ---- Observaciones
        c.add(etiqueta("Observaciones", Font.BOLD, 12f, Tema.TEXTO));
        c.add(Box.createVerticalStrut(4));
        txtObs.setLineWrap(true);
        txtObs.setWrapStyleWord(true);
        txtObs.setBackground(Tema.PANEL_CLARO);
        txtObs.setForeground(Tema.TEXTO);
        txtObs.setCaretColor(Tema.TEXTO);
        txtObs.setFont(Tema.fuente(Font.PLAIN, 13f));
        txtObs.setBorder(BorderFactory.createEmptyBorder(8, 10, 8, 10));
        txtObs.setText(e.getObservaciones());
        JScrollPane sp = new JScrollPane(txtObs);
        sp.setBorder(BorderFactory.createLineBorder(Tema.BORDE));
        sp.setAlignmentX(Component.LEFT_ALIGNMENT);
        c.add(sp);
        c.add(Box.createVerticalStrut(22));

        // ---- Botones
        JButton btnCancelar = Tema.boton("Cancelar", Tema.PANEL_CLARO, Tema.TEXTO);
        JButton btnGuardar = Tema.boton("Guardar condición", Tema.AZUL, Color.WHITE);
        JButton btnSiguiente = Tema.boton("Guardar y siguiente", Tema.ACENTO, Tema.SOBRE_ACENTO);
        btnSiguiente.setEnabled(haySiguiente);

        btnCancelar.addActionListener(ev -> { accion = Accion.CERRAR; dispose(); });
        btnGuardar.addActionListener(ev -> confirmar(Accion.GUARDAR));
        btnSiguiente.addActionListener(ev -> confirmar(Accion.GUARDAR_Y_SIGUIENTE));

        JPanel botones = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        botones.setOpaque(false);
        botones.add(btnCancelar);
        botones.add(btnGuardar);
        botones.add(btnSiguiente);
        botones.setAlignmentX(Component.LEFT_ALIGNMENT);
        c.add(botones);

        getRootPane().registerKeyboardAction(ev -> { accion = Accion.CERRAR; dispose(); },
                KeyStroke.getKeyStroke("ESCAPE"), JComponent.WHEN_IN_FOCUSED_WINDOW);

        add(c);
        pack();
        setMinimumSize(new Dimension(600, getHeight()));
        setResizable(false);
        setLocationRelativeTo(owner);
    }

    private void elegir(boolean habilitado) {
        eligioHabilitado = habilitado;
        opHab.setActiva(habilitado);
        opNo.setActiva(!habilitado);
    }

    private void confirmar(Accion a) {
        if (eligioHabilitado != veredictoHabilitado && txtObs.getText().trim().isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "Cambiaste la condición propuesta por el sistema.\n"
                    + "Escribe el motivo en el campo de observaciones.",
                    "Falta la justificación", JOptionPane.WARNING_MESSAGE);
            txtObs.requestFocus();
            return;
        }
        accion = a;
        dispose();
    }

    private JLabel etiqueta(String texto, int estilo, float tam, Color color) {
        JLabel l = new JLabel(texto);
        l.setFont(Tema.fuente(estilo, tam));
        l.setForeground(color);
        l.setAlignmentX(Component.LEFT_ALIGNMENT);
        return l;
    }

    private JPanel fila(VerificadorRequisitos.Item it) {
        JPanel f = new JPanel(new BorderLayout());
        f.setOpaque(false);
        f.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, Tema.BORDE),
                BorderFactory.createEmptyBorder(9, 0, 9, 0)));
        Color col = it.cumple ? Tema.VERDE : Tema.ROJO;
        JLabel izq = new JLabel((it.cumple ? "✔  " : "✘  ") + it.nombre);
        izq.setFont(Tema.fuente(Font.BOLD, 13f));
        izq.setForeground(col);
        JLabel der = new JLabel(it.detalle);
        der.setFont(Tema.fuente(Font.PLAIN, 12f));
        der.setForeground(Tema.TEXTO_SUAVE);
        f.add(izq, BorderLayout.WEST);
        f.add(der, BorderLayout.EAST);
        f.setAlignmentX(Component.LEFT_ALIGNMENT);
        f.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        return f;
    }

    public Accion getAccion() { return accion; }

    public String getCondicionFinal() {
        return eligioHabilitado ? Estudiante.HABILITADO : Estudiante.NO_HABILITADO;
    }

    public String getObservaciones() {
        return txtObs.getText().trim();
    }

    // ---------------------------------------------------------- Componentes
    private static class Avatar extends JComponent {
        private final String iniciales;

        Avatar(String nombre) {
            String[] p = nombre.trim().split("\\s+");
            iniciales = "" + p[0].charAt(0) + (p.length > 1 ? p[1].charAt(0) : "");
            setPreferredSize(new Dimension(56, 56));
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                    RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g2.setColor(Tema.AVATAR_FONDO);
            g2.fillOval(0, 0, 56, 56);
            g2.setFont(Tema.fuente(Font.BOLD, 18f));
            g2.setColor(Tema.AVATAR_TEXTO);
            FontMetrics fm = g2.getFontMetrics();
            g2.drawString(iniciales, (56 - fm.stringWidth(iniciales)) / 2,
                    (56 + fm.getAscent() - fm.getDescent()) / 2);
            g2.dispose();
        }
    }

    /** Tarjeta grande seleccionable (Habilitado / No habilitado). */
    private static class Opcion extends JComponent {
        private final String texto;
        private final Color color;
        private boolean activa;

        Opcion(String texto, Color color, Runnable alElegir) {
            this.texto = texto;
            this.color = color;
            setPreferredSize(new Dimension(200, 50));
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent ev) {
                    alElegir.run();
                }
            });
        }

        void setActiva(boolean a) {
            activa = a;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                    RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            int w = getWidth();
            int h = getHeight();
            g2.setColor(activa ? new Color(color.getRed(), color.getGreen(), color.getBlue(), 55)
                    : Tema.PANEL_CLARO);
            g2.fillRoundRect(0, 0, w - 1, h - 1, 14, 14);
            g2.setColor(activa ? color : Tema.BORDE);
            g2.setStroke(new BasicStroke(activa ? 2f : 1f));
            g2.drawRoundRect(1, 1, w - 3, h - 3, 14, 14);
            g2.setFont(Tema.fuente(Font.BOLD, 14f));
            g2.setColor(activa ? color : Tema.TEXTO_SUAVE);
            FontMetrics fm = g2.getFontMetrics();
            g2.drawString(texto, (w - fm.stringWidth(texto)) / 2,
                    (h + fm.getAscent() - fm.getDescent()) / 2);
            g2.dispose();
        }
    }
}