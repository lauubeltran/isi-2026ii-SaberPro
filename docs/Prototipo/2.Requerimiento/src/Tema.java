import java.awt.*;
import javax.swing.*;

public final class Tema {
    private Tema() { }

    // ---- Paleta (cambia estos valores para probar otros colores)
    public static final Color FONDO = new Color(0xF3F5F8);
    public static final Color PANEL = new Color(0xFFFFFF);
    public static final Color PANEL_CLARO = new Color(0xEEF1F5);
    public static final Color BORDE = new Color(0xD8DEE6);
    public static final Color TEXTO = new Color(0x1F2933);
    public static final Color TEXTO_SUAVE = new Color(0x6B7785);

    public static final Color ACENTO = new Color(0x0F6B5C);        // verde institucional
    public static final Color ACENTO_SUAVE = new Color(0xDDEFEA);  // versión clara del acento
    public static final Color SOBRE_ACENTO = Color.WHITE;          // texto sobre el acento
    public static final Color SELECCION = new Color(0xE6F2EF);     // fila seleccionada

    public static final Color AZUL = new Color(0x2F6DB5);
    public static final Color VERDE = new Color(0x15803D);
    public static final Color ROJO = new Color(0xC62828);
    public static final Color AMBAR = new Color(0xB45309);

    public static final Color AVATAR_FONDO = new Color(0xDDEFEA);
    public static final Color AVATAR_TEXTO = new Color(0x0F6B5C);

    public static Font fuente(int estilo, float tam) {
        return new Font("Segoe UI", estilo, 12).deriveFont(tam);
    }

    public static JButton boton(String texto, Color fondo, Color letra) {
        JButton b = new JButton(texto) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                Color c = !isEnabled() ? PANEL_CLARO
                        : getModel().isRollover() ? getBackground().brighter() : getBackground();
                g2.setColor(c);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
                g2.dispose();
                super.paintComponent(g);
            }

            @Override
            public void setEnabled(boolean e) {
                super.setEnabled(e);
                setForeground(e ? letra : TEXTO_SUAVE);
            }
        };
        b.setContentAreaFilled(false);
        b.setBorderPainted(false);
        b.setFocusPainted(false);
        b.setOpaque(false);
        b.setRolloverEnabled(true);
        b.setBackground(fondo);
        b.setForeground(letra);
        b.setFont(fuente(Font.BOLD, 13f));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.setBorder(BorderFactory.createEmptyBorder(9, 18, 9, 18));
        return b;
    }

    public static void estilizarCampo(JTextField f) {
        f.setBackground(PANEL_CLARO);
        f.setForeground(TEXTO);
        f.setCaretColor(TEXTO);
        f.setFont(fuente(Font.PLAIN, 14f));
        f.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDE),
                BorderFactory.createEmptyBorder(10, 12, 10, 12)));
        f.setAlignmentX(Component.LEFT_ALIGNMENT);
        f.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
    }

    /** Panel con fondo redondeado (las tarjetas blancas llevan borde fino). */
    public static class Redondeado extends JPanel {
        private Color color;
        private final int radio;

        public Redondeado(Color color, int radio) {
            this.color = color;
            this.radio = radio;
            setOpaque(false);
        }

        public void setColor(Color c) {
            this.color = c;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(color);
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), radio, radio);
            if (color.equals(PANEL)) {
                g2.setColor(BORDE);
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, radio, radio);
            }
            g2.dispose();
            super.paintComponent(g);
        }
    }

    /** Etiqueta tipo "pastilla". */
    public static class Pildora extends JLabel {
        private final Color fondo;

        public Pildora(String texto, Color fondo, Color letra) {
            super(texto, SwingConstants.CENTER);
            this.fondo = fondo;
            setForeground(letra);
            setFont(fuente(Font.BOLD, 11f));
            setOpaque(false);
            setBorder(BorderFactory.createEmptyBorder(4, 12, 4, 12));
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(fondo);
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), getHeight(), getHeight());
            g2.dispose();
            super.paintComponent(g);
        }
    }
}