import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;

public class VentanaListadoHabilitados extends JPanel {

    private final List<Estudiante> habilitados = new ArrayList<>();
    private final DefaultTableModel modelo;
    private final JLabel cantidad = new JLabel();
    private final JLabel lblContador = new JLabel("0");

    public VentanaListadoHabilitados(List<Estudiante> estudiantes) {

        setLayout(new BorderLayout(0, 16));
        setBackground(Tema.FONDO);
        setBorder(BorderFactory.createEmptyBorder(26, 32, 22, 32));

        // ---------- Encabezado
        JLabel kicker = new JLabel("PANEL DEL DIRECTOR DEL PROGRAMA · INGENIERÍA DE SISTEMAS");
        kicker.setFont(Tema.fuente(Font.BOLD, 11f));
        kicker.setForeground(Tema.ACENTO);
        kicker.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel titulo = new JLabel("Listado de estudiantes habilitados");
        titulo.setFont(new Font("Segoe UI", Font.BOLD, 34));
        titulo.setForeground(Tema.TEXTO);
        titulo.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel subtitulo = new JLabel("Listado para revisión y envío al coordinador.");
        subtitulo.setFont(Tema.fuente(Font.PLAIN, 13f));
        subtitulo.setForeground(Tema.TEXTO_SUAVE);
        subtitulo.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel textos = new JPanel();
        textos.setLayout(new BoxLayout(textos, BoxLayout.Y_AXIS));
        textos.setOpaque(false);
        textos.add(kicker);
        textos.add(Box.createVerticalStrut(6));
        textos.add(titulo);
        textos.add(Box.createVerticalStrut(4));
        textos.add(subtitulo);

        // Tarjeta con el contador
        Tema.Redondeado tarjeta = new Tema.Redondeado(Tema.PANEL, 18);
        tarjeta.setLayout(new BorderLayout(0, 2));
        tarjeta.setBorder(BorderFactory.createEmptyBorder(12, 20, 12, 20));
        lblContador.setFont(new Font("Segoe UI", Font.BOLD, 30));
        lblContador.setForeground(Tema.VERDE);
        JLabel lblTit = new JLabel("Habilitados");
        lblTit.setFont(Tema.fuente(Font.PLAIN, 12f));
        lblTit.setForeground(Tema.TEXTO_SUAVE);
        tarjeta.add(lblContador, BorderLayout.CENTER);
        tarjeta.add(lblTit, BorderLayout.SOUTH);
        tarjeta.setPreferredSize(new Dimension(170, 80));

        JPanel encabezado = new JPanel(new BorderLayout());
        encabezado.setOpaque(false);
        encabezado.add(textos, BorderLayout.CENTER);
        encabezado.add(tarjeta, BorderLayout.EAST);
        add(encabezado, BorderLayout.NORTH);

        // ---------- Tabla
        modelo = new DefaultTableModel(
                new String[]{"ESTUDIANTE", "CÓDIGO", "PROGRAMA", "CONDICIÓN"}, 0) {
            @Override
            public boolean isCellEditable(int fila, int columna) {
                return false;
            }
        };

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
        tabla.getColumnModel().getColumn(3).setCellRenderer(new CeldaCondicion());
        int[] anchos = {320, 160, 280, 180};
        for (int i = 0; i < anchos.length; i++) {
            tabla.getColumnModel().getColumn(i).setPreferredWidth(anchos[i]);
        }

        JScrollPane scroll = new JScrollPane(tabla);
        scroll.setBorder(BorderFactory.createLineBorder(Tema.BORDE));
        scroll.getViewport().setBackground(Tema.PANEL);
        scroll.getVerticalScrollBar().setUnitIncrement(18);
        add(scroll, BorderLayout.CENTER);

        // ---------- Pie
        cantidad.setFont(Tema.fuente(Font.PLAIN, 12f));
        cantidad.setForeground(Tema.TEXTO_SUAVE);

        JButton btnGuardar = Tema.boton(
                "Guardar listado para enviar", Tema.ACENTO, Tema.SOBRE_ACENTO);
        btnGuardar.addActionListener(ev -> guardarListado());

        JPanel pie = new JPanel(new BorderLayout());
        pie.setOpaque(false);
        pie.add(cantidad, BorderLayout.WEST);
        pie.add(btnGuardar, BorderLayout.EAST);
        add(pie, BorderLayout.SOUTH);

        actualizar(estudiantes);
    }

    /** Vuelve a calcular los habilitados y refresca la tabla. */
    public void actualizar(List<Estudiante> estudiantes) {
        habilitados.clear();
        for (Estudiante e : estudiantes) {
            if (Estudiante.HABILITADO.equals(e.getCondicion())) {
                habilitados.add(e);
            }
        }

        modelo.setRowCount(0);
        for (Estudiante e : habilitados) {
            modelo.addRow(new Object[]{
                    e,
                    e.getCodigo(),
                    e.getPrograma(),
                    e.getCondicion()
            });
        }

        lblContador.setText(String.valueOf(habilitados.size()));
        cantidad.setText("Total de estudiantes habilitados: " + habilitados.size());
    }

    private void guardarListado() {
        JFileChooser selector = new JFileChooser();
        selector.setSelectedFile(new File("listado_habilitados.csv"));

        if (selector.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }

        File archivo = selector.getSelectedFile();
        List<String> lineas = new ArrayList<>();
        lineas.add("codigo;nombre;programa;condicion");

        for (Estudiante e : habilitados) {
            lineas.add(e.getCodigo() + ";"
                    + e.getNombre() + ";"
                    + e.getPrograma() + ";"
                    + e.getCondicion());
        }

        try {
            Files.write(archivo.toPath(), lineas, StandardCharsets.UTF_8);
            JOptionPane.showMessageDialog(this, "Listado guardado correctamente.");
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    // -------------------------------------------------------------- Renderers
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
            fm = g2.getFontMetrics();
            g2.drawString(e.getNombre(), 64, (h + fm.getAscent() - fm.getDescent()) / 2);
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

    private static class CeldaCondicion extends Celda {
        @Override
        protected void dibujar(Graphics2D g2) {
            String texto = "✔  Habilitado";
            g2.setFont(Tema.fuente(Font.BOLD, 13f));
            FontMetrics fm = g2.getFontMetrics();
            int w = fm.stringWidth(texto) + 28;
            int h = 30;
            int y = (getHeight() - h) / 2;
            g2.setColor(new Color(34, 197, 94, 45));
            g2.fillRoundRect(16, y, w, h, h, h);
            g2.setColor(Tema.VERDE);
            g2.drawString(texto, 30, y + (h + fm.getAscent() - fm.getDescent()) / 2);
        }
    }
}