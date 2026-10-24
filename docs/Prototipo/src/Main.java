import javax.swing.SwingUtilities;
import javax.swing.UIManager;

public class Main {
    public static final String RUTA_CSV = "src/datos/estudiantes.csv";

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignorada) { }
        SwingUtilities.invokeLater(() -> new LoginDirector(RUTA_CSV).setVisible(true));
    }
}