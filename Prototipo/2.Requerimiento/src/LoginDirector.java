import java.awt.*;
import javax.swing.*;

/** Pantalla de acceso. Se elige el rol (Director o Coordinador) y se ingresa con usuario y clave. */
public class LoginDirector extends JFrame {
    // Credenciales de demostración
    private static final String USUARIO_DIRECTOR = "director";
    private static final String USUARIO_COORDINADOR = "coordinador";
    private static final String CLAVE = "saberpro";

    private final String rutaCsv;
    private final JTextField txtUsuario = new JTextField(USUARIO_DIRECTOR);
    private final JPasswordField txtClave = new JPasswordField();
    private final JLabel lblError = new JLabel(" ");

    private final JButton btnDirector = Tema.boton("Director", Tema.ACENTO, Tema.SOBRE_ACENTO);
    private final JButton btnCoordinador = Tema.boton("Coordinador", Tema.PANEL_CLARO, Tema.TEXTO);
    private final JLabel textoRol = new JLabel();
    private final JButton btnIngresar = Tema.boton("Ingresar como Director", Tema.ACENTO, Tema.SOBRE_ACENTO);
    private Tema.Pildora rol;
    private final JPanel tarjetaRol = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));

    public LoginDirector(String rutaCsv) {
        super("Saber Pro · Acceso");
        this.rutaCsv = rutaCsv;
        setDefaultCloseOperation(EXIT_ON_CLOSE);

        JPanel fondo = new JPanel(new GridBagLayout());
        fondo.setBackground(Tema.FONDO);

        Tema.Redondeado tarjeta = new Tema.Redondeado(Tema.PANEL, 26);
        tarjeta.setLayout(new BoxLayout(tarjeta, BoxLayout.Y_AXIS));
        tarjeta.setBorder(BorderFactory.createEmptyBorder(38, 42, 34, 42));
        tarjeta.setPreferredSize(new Dimension(440, 600));

        JLabel logo = texto("Saber Pro", Font.BOLD, 34f, Tema.ACENTO);
        logo.setFont(new Font("Segoe UI", Font.BOLD, 34));
        JLabel sub = texto("Sistema de información · Gestión de pruebas", Font.PLAIN, 13f, Tema.TEXTO_SUAVE);

        // Selector de rol
        btnDirector.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        btnCoordinador.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        btnDirector.addActionListener(ev -> elegirRol(true));
        btnCoordinador.addActionListener(ev -> elegirRol(false));
        JPanel selector = new JPanel(new GridLayout(1, 2, 8, 0));
        selector.setOpaque(false);
        selector.setAlignmentX(Component.LEFT_ALIGNMENT);
        selector.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));
        selector.add(btnDirector);
        selector.add(btnCoordinador);

        rol = new Tema.Pildora("ACCESO DEL DIRECTOR DEL PROGRAMA", Tema.ACENTO_SUAVE, Tema.ACENTO);
        tarjetaRol.setOpaque(false);
        tarjetaRol.setAlignmentX(Component.LEFT_ALIGNMENT);
        tarjetaRol.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
        tarjetaRol.add(rol);

        Tema.estilizarCampo(txtUsuario);
        Tema.estilizarCampo(txtClave);

        lblError.setFont(Tema.fuente(Font.BOLD, 12f));
        lblError.setForeground(Tema.ROJO);
        lblError.setAlignmentX(Component.LEFT_ALIGNMENT);

        btnIngresar.setAlignmentX(Component.LEFT_ALIGNMENT);
        btnIngresar.setMaximumSize(new Dimension(Integer.MAX_VALUE, 46));
        btnIngresar.addActionListener(ev -> ingresar());
        getRootPane().setDefaultButton(btnIngresar);

        JLabel pista = texto("Demo · usuario: director o coordinador · clave: saberpro",
                Font.PLAIN, 12f, Tema.TEXTO_SUAVE);

        tarjeta.add(logo);
        tarjeta.add(Box.createVerticalStrut(4));
        tarjeta.add(sub);
        tarjeta.add(Box.createVerticalStrut(22));
        tarjeta.add(texto("Ingresar como", Font.BOLD, 12f, Tema.TEXTO));
        tarjeta.add(Box.createVerticalStrut(6));
        tarjeta.add(selector);
        tarjeta.add(Box.createVerticalStrut(18));
        tarjeta.add(tarjetaRol);
        tarjeta.add(Box.createVerticalStrut(22));
        tarjeta.add(texto("Usuario", Font.BOLD, 12f, Tema.TEXTO));
        tarjeta.add(Box.createVerticalStrut(6));
        tarjeta.add(txtUsuario);
        tarjeta.add(Box.createVerticalStrut(16));
        tarjeta.add(texto("Contraseña", Font.BOLD, 12f, Tema.TEXTO));
        tarjeta.add(Box.createVerticalStrut(6));
        tarjeta.add(txtClave);
        tarjeta.add(Box.createVerticalStrut(10));
        tarjeta.add(lblError);
        tarjeta.add(Box.createVerticalStrut(14));
        tarjeta.add(btnIngresar);
        tarjeta.add(Box.createVerticalGlue());
        tarjeta.add(pista);

        fondo.add(tarjeta);
        setContentPane(fondo);
        setSize(900, 720);
        setLocationRelativeTo(null);
    }

    private JLabel texto(String t, int estilo, float tam, Color color) {
        JLabel l = new JLabel(t);
        l.setFont(Tema.fuente(estilo, tam));
        l.setForeground(color);
        l.setAlignmentX(Component.LEFT_ALIGNMENT);
        return l;
    }

    /** Cambia el rol elegido: resalta el botón, el rótulo, el usuario y el texto del botón de ingreso. */
    private void elegirRol(boolean director) {
        btnDirector.setBackground(director ? Tema.ACENTO : Tema.PANEL_CLARO);
        btnDirector.setForeground(director ? Tema.SOBRE_ACENTO : Tema.TEXTO);
        btnCoordinador.setBackground(director ? Tema.PANEL_CLARO : Tema.ACENTO);
        btnCoordinador.setForeground(director ? Tema.TEXTO : Tema.SOBRE_ACENTO);
        btnDirector.repaint();
        btnCoordinador.repaint();

        tarjetaRol.removeAll();
        rol = new Tema.Pildora(director ? "ACCESO DEL DIRECTOR DEL PROGRAMA" : "ACCESO DEL COORDINADOR DE PRUEBAS",
                Tema.ACENTO_SUAVE, Tema.ACENTO);
        tarjetaRol.add(rol);
        tarjetaRol.revalidate();
        tarjetaRol.repaint();

        txtUsuario.setText(director ? USUARIO_DIRECTOR : USUARIO_COORDINADOR);
        btnIngresar.setText(director ? "Ingresar como Director" : "Ingresar como Coordinador");
        lblError.setText(" ");
        txtClave.setText("");
        txtClave.requestFocus();
    }

    private void ingresar() {
        String u = txtUsuario.getText().trim();
        String c = new String(txtClave.getPassword());
        if (CLAVE.equals(c) && USUARIO_DIRECTOR.equalsIgnoreCase(u)) {
            new VentanaPrincipal(rutaCsv).setVisible(true);
            dispose();
        } else if (CLAVE.equals(c) && USUARIO_COORDINADOR.equalsIgnoreCase(u)) {
            new VentanaCoordinador(rutaCsv).setVisible(true);
            dispose();
        } else {
            lblError.setText("Usuario o contraseña incorrectos.");
            txtClave.setText("");
            txtClave.requestFocus();
        }
    }
}