import java.awt.*;
import javax.swing.*;

public class LoginDirector extends JFrame {
    // Credenciales de demostración
    private static final String USUARIO = "director";
    private static final String CLAVE = "saberpro";

    private final String rutaCsv;
    private final JTextField txtUsuario = new JTextField("director");
    private final JPasswordField txtClave = new JPasswordField();
    private final JLabel lblError = new JLabel(" ");

    public LoginDirector(String rutaCsv) {
        super("Saber Pro · Acceso del Director");
        this.rutaCsv = rutaCsv;
        setDefaultCloseOperation(EXIT_ON_CLOSE);

        JPanel fondo = new JPanel(new GridBagLayout());
        fondo.setBackground(Tema.FONDO);

        Tema.Redondeado tarjeta = new Tema.Redondeado(Tema.PANEL, 26);
        tarjeta.setLayout(new BoxLayout(tarjeta, BoxLayout.Y_AXIS));
        tarjeta.setBorder(BorderFactory.createEmptyBorder(38, 42, 34, 42));
        tarjeta.setPreferredSize(new Dimension(440, 540));

        JLabel logo = texto("Saber Pro", Font.BOLD, 34f, Tema.ACENTO);
        logo.setFont(new Font("Segoe UI", Font.BOLD, 34));
        JLabel sub = texto("Sistema de información · Gestión de pruebas", Font.PLAIN, 13f, Tema.TEXTO_SUAVE);

        Tema.Pildora rol = new Tema.Pildora("ACCESO DEL DIRECTOR DEL PROGRAMA",
                Tema.ACENTO_SUAVE, Tema.ACENTO);
        rol.setAlignmentX(Component.LEFT_ALIGNMENT);

        Tema.estilizarCampo(txtUsuario);
        Tema.estilizarCampo(txtClave);

        lblError.setFont(Tema.fuente(Font.BOLD, 12f));
        lblError.setForeground(Tema.ROJO);
        lblError.setAlignmentX(Component.LEFT_ALIGNMENT);

        JButton btn = Tema.boton("Ingresar como Director", Tema.ACENTO, Tema.SOBRE_ACENTO);
        btn.setAlignmentX(Component.LEFT_ALIGNMENT);
        btn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 46));
        btn.addActionListener(ev -> ingresar());
        getRootPane().setDefaultButton(btn);

        JLabel pista = texto("Demo · usuario: director · clave: saberpro", Font.PLAIN, 12f, Tema.TEXTO_SUAVE);

        tarjeta.add(logo);
        tarjeta.add(Box.createVerticalStrut(4));
        tarjeta.add(sub);
        tarjeta.add(Box.createVerticalStrut(22));
        tarjeta.add(rol);
        tarjeta.add(Box.createVerticalStrut(28));
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
        tarjeta.add(btn);
        tarjeta.add(Box.createVerticalGlue());
        tarjeta.add(pista);

        fondo.add(tarjeta);
        setContentPane(fondo);
        setSize(900, 680);
        setLocationRelativeTo(null);
    }

    private JLabel texto(String t, int estilo, float tam, Color color) {
        JLabel l = new JLabel(t);
        l.setFont(Tema.fuente(estilo, tam));
        l.setForeground(color);
        l.setAlignmentX(Component.LEFT_ALIGNMENT);
        return l;
    }

    private void ingresar() {
        String u = txtUsuario.getText().trim();
        String c = new String(txtClave.getPassword());
        if (USUARIO.equalsIgnoreCase(u) && CLAVE.equals(c)) {
            new VentanaPrincipal(rutaCsv).setVisible(true);
            dispose();
        } else {
            lblError.setText("Usuario o contraseña incorrectos.");
            txtClave.setText("");
            txtClave.requestFocus();
        }
    }
}