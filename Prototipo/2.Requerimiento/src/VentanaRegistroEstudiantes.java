
import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.awt.Component;
import javax.swing.Box;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.filechooser.FileNameExtensionFilter;

public class VentanaRegistroEstudiantes extends JDialog {

    private final JComboBox<String> comboPrograma;
    private final JTextField campoPeriodo;
    private final JLabel etiquetaArchivo;
    private File archivoSeleccionado;

    private final RepositorioEstudiantes repositorio;

    public VentanaRegistroEstudiantes(
            java.awt.Frame padre,
            RepositorioEstudiantes repositorio) {

        super(padre, "Registro de estudiantes", true);

        this.repositorio = repositorio;

        comboPrograma = new JComboBox<>(new String[]{
            "Ingeniería de Sistemas",
            "Administración de Empresas",
            "Contaduría Pública"
        });

        campoPeriodo = new JTextField("2026-2", 15);
        etiquetaArchivo = new JLabel("Ningún archivo seleccionado");

        campoPeriodo.setText("2026-2");
        Tema.estilizarCampo(campoPeriodo);

        comboPrograma.setFont(Tema.fuente(
            java.awt.Font.PLAIN, 14f
        ));
        comboPrograma.setBackground(Tema.PANEL_CLARO);
        comboPrograma.setForeground(Tema.TEXTO);
        comboPrograma.setBorder(
            BorderFactory.createLineBorder(Tema.BORDE)
        );

        etiquetaArchivo.setFont(
            Tema.fuente(java.awt.Font.PLAIN, 13f)
        );
        etiquetaArchivo.setForeground(Tema.TEXTO_SUAVE);

        JButton botonArchivo = Tema.boton("Seleccionar CSV",Tema.PANEL_CLARO,Tema.TEXTO);
        JButton botonRegistrar = Tema.boton("Registrar estudiantes",Tema.ACENTO,Tema.SOBRE_ACENTO);
        JButton botonCancelar = Tema.boton("Cancelar",Tema.PANEL_CLARO,Tema.TEXTO);
        JPanel encabezado = new JPanel();
        encabezado.setLayout(new BorderLayout(0, 5));
        encabezado.setBackground(Tema.PANEL);
        encabezado.setBorder(
            BorderFactory.createEmptyBorder(18, 20, 14, 20)
        );

        JLabel titulo = new JLabel("Registrar estudiantes");
        titulo.setFont(Tema.fuente(java.awt.Font.BOLD, 22f));
        titulo.setForeground(Tema.TEXTO);

        JLabel subtitulo = new JLabel(
            "Incorpora estudiantes al sistema Saber Pro"
        );
        subtitulo.setFont(Tema.fuente(java.awt.Font.PLAIN, 13f));
        subtitulo.setForeground(Tema.TEXTO_SUAVE);

        JPanel textos = new JPanel();
        textos.setLayout(new javax.swing.BoxLayout(
            textos, javax.swing.BoxLayout.Y_AXIS
        ));
        textos.setBackground(Tema.PANEL);
        textos.add(titulo);
        textos.add(Box.createVerticalStrut(5));
        textos.add(subtitulo);

        encabezado.add(textos, BorderLayout.CENTER);

        JPanel formulario = new JPanel(new GridBagLayout());
        formulario.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;

        agregarFila(formulario, gbc, 0,
                "Programa académico:", comboPrograma);

        agregarFila(formulario, gbc, 1,
                "Periodo académico:", campoPeriodo);

        agregarFila(formulario, gbc, 2,
                "Archivo CSV:", botonArchivo);

        agregarFila(formulario, gbc, 3,
                "Archivo elegido:", etiquetaArchivo);

        JPanel botones = new JPanel();
        botones.add(botonRegistrar);
        botones.add(botonCancelar);

        formulario.setBackground(Tema.PANEL);
        formulario.setBorder(
            BorderFactory.createEmptyBorder(8, 20, 16, 20)
        );

        botones.setBackground(Tema.PANEL);
        botones.setBorder(
            BorderFactory.createEmptyBorder(0, 20, 18, 20)
        );

        JPanel contenido = new JPanel(new BorderLayout());
        contenido.setBackground(Tema.FONDO);
        contenido.setBorder(
            BorderFactory.createEmptyBorder(14, 14, 14, 14)
        );

        JPanel tarjeta = new JPanel(new BorderLayout());
        tarjeta.setBackground(Tema.PANEL);
        tarjeta.setBorder(BorderFactory.createLineBorder(Tema.BORDE));

        tarjeta.add(encabezado, BorderLayout.NORTH);
        tarjeta.add(formulario, BorderLayout.CENTER);
        tarjeta.add(botones, BorderLayout.SOUTH);

        contenido.add(tarjeta, BorderLayout.CENTER);
        setContentPane(contenido);

        botonArchivo.addActionListener(e -> seleccionarArchivo());

        botonRegistrar.addActionListener(e -> registrarEstudiantes());

        botonCancelar.addActionListener(e -> dispose());

        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        pack();
        setSize(getWidth() + 40,getHeight() + 20);
        setMinimumSize(getSize());
        setLocationRelativeTo(padre);
    }

    private void agregarFila(
            JPanel panel,
            GridBagConstraints gbc,
            int fila,
            String texto,
            java.awt.Component componente) {

        gbc.gridy = fila;

        gbc.gridx = 0;
        gbc.weightx = 0;
        panel.add(new JLabel(texto), gbc);

        gbc.gridx = 1;
        gbc.weightx = 1;
        panel.add(componente, gbc);
    }

    private void seleccionarArchivo() {
        JFileChooser selector = new JFileChooser();

        selector.setDialogTitle("Seleccionar archivo de estudiantes");
        selector.setFileFilter(
                new FileNameExtensionFilter("Archivos CSV (*.csv)", "csv"));

        int resultado = selector.showOpenDialog(this);

        if (resultado == JFileChooser.APPROVE_OPTION) {
            archivoSeleccionado = selector.getSelectedFile();
            etiquetaArchivo.setText(archivoSeleccionado.getName());
        }
    }

    private void registrarEstudiantes() {
        if (archivoSeleccionado == null) {
            JOptionPane.showMessageDialog(
                    this,
                    "Primero debes seleccionar un archivo CSV.",
                    "Archivo requerido",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        String programa = (String) comboPrograma.getSelectedItem();
        String periodo = campoPeriodo.getText().trim();

        if (periodo.isBlank()) {
            JOptionPane.showMessageDialog(
                    this,
                    "Debes ingresar el periodo académico.",
                    "Dato requerido",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        try {
            int cantidad = repositorio.registrarNuevos(
                    Path.of(archivoSeleccionado.getAbsolutePath()),
                    programa,
                    periodo);

            JOptionPane.showMessageDialog(
                    this,
                    "Registro completado correctamente.\n"
                    + "Estudiantes registrados: " + cantidad
                    + "\nEstado inicial: PENDIENTE",
                    "Importación exitosa",
                    JOptionPane.INFORMATION_MESSAGE);

            dispose();

        } catch (IOException | RuntimeException ex) {
            JOptionPane.showMessageDialog(
                    this,
                    "No fue posible importar los estudiantes.\n"
                    + ex.getMessage(),
                    "Error de importación",
                    JOptionPane.ERROR_MESSAGE);
        }
    }
}
