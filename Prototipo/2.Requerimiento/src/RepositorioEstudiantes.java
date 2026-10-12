
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class RepositorioEstudiantes {
    private static final String SEP = ";";

    private static final List<String> COLUMNAS_NUEVAS = Arrays.asList(
            "correo", "telefono", "cohorte", "pensum", "periodo"
    );

    private static final Set<String> COLUMNAS_FIJAS = Set.of(
            "codigo", "nombre", "programa",
            "correo", "telefono", "cohorte", "pensum", "periodo",
            "creditos_aprobados", "creditos_totales", "promedio",
            "condicion", "observaciones"
    );

    private final Path ruta;
    private String[] encabezado;

    public RepositorioEstudiantes(String ruta) {
        this.ruta = Paths.get(ruta);
    }

    private String normalizar(String texto) {
        return texto.trim()
                .toLowerCase(Locale.ROOT)
                .replace("á", "a")
                .replace("é", "e")
                .replace("í", "i")
                .replace("ó", "o")
                .replace("ú", "u")
                .replace(" ", "_");
    }

    private int indice(String[] cabecera, String nombre) {
        for (int i = 0; i < cabecera.length; i++) {
            if (normalizar(cabecera[i]).equals(normalizar(nombre))) {
                return i;
            }
        }
        return -1;
    }

    private String valor(String[] datos, String[] cabecera,
                         String columna, String defecto) {
        int i = indice(cabecera, columna);

        if (i < 0 || i >= datos.length) {
            return defecto;
        }

        return datos[i].trim();
    }

    private String valorAlternativo(String[] datos, String[] cabecera,
                                    String defecto, String... nombres) {
        for (String nombre : nombres) {
            int i = indice(cabecera, nombre);

            if (i >= 0 && i < datos.length) {
                return datos[i].trim();
            }
        }

        return defecto;
    }

    private String[] ampliarEncabezado(String[] original) {
        List<String> columnas = new ArrayList<>(Arrays.asList(original));

        for (String nueva : COLUMNAS_NUEVAS) {
            boolean existe = false;

            for (String columna : columnas) {
                if (normalizar(columna).equals(normalizar(nueva))) {
                    existe = true;
                    break;
                }
            }

            if (!existe) {
                columnas.add(nueva);
            }
        }

        return columnas.toArray(new String[0]);
    }

    public List<Estudiante> cargar() throws IOException {
        List<String> lineas = Files.readAllLines(
                ruta, StandardCharsets.UTF_8);

        List<Estudiante> lista = new ArrayList<>();

        if (lineas.isEmpty()) {
            return lista;
        }

        String primera = lineas.get(0).replace("\uFEFF", "");
        String[] cabeceraOriginal = primera.split(SEP, -1);

        for (int i = 0; i < cabeceraOriginal.length; i++) {
            cabeceraOriginal[i] = cabeceraOriginal[i].trim();
        }

        encabezado = ampliarEncabezado(cabeceraOriginal);

        int idxCodigo = indice(encabezado, "codigo");
        int idxNombre = indice(encabezado, "nombre");
        int idxPrograma = indice(encabezado, "programa");
        int idxAprobados = indice(encabezado, "creditos_aprobados");
        int idxTotales = indice(encabezado, "creditos_totales");
        int idxPromedio = indice(encabezado, "promedio");
        int idxCondicion = indice(encabezado, "condicion");
        int idxObservaciones = indice(encabezado, "observaciones");

        if (idxCodigo < 0 || idxNombre < 0 || idxPrograma < 0
                || idxAprobados < 0 || idxTotales < 0
                || idxPromedio < 0 || idxCondicion < 0) {
            throw new IOException(
                    "El CSV actual no contiene todas las columnas "
                    + "necesarias para la aplicación.");
        }

        for (int i = 1; i < lineas.size(); i++) {
            String linea = lineas.get(i).trim();

            if (linea.isEmpty()) {
                continue;
            }

            String[] datos = linea.split(SEP, -1);

            Map<String, Boolean> requisitos = new LinkedHashMap<>();

            for (int j = 0; j < cabeceraOriginal.length; j++) {
                String columna = cabeceraOriginal[j];
                String nombre = normalizar(columna);

                if (!COLUMNAS_FIJAS.contains(nombre)) {
                    requisitos.put(
                            columna,
                            Boolean.parseBoolean(
                                    j < datos.length
                                            ? datos[j].trim()
                                            : "false")
                    );
                }
            }

            String condicion = valor(
                    datos, encabezado, "condicion",
                    Estudiante.PENDIENTE);

            if (condicion.isBlank()) {
                condicion = Estudiante.PENDIENTE;
            }

            String observaciones = idxObservaciones >= 0
                    ? valor(datos, encabezado, "observaciones", "")
                    : "";

            lista.add(new Estudiante(
                    valor(datos, encabezado, "codigo", ""),
                    valor(datos, encabezado, "nombre", ""),
                    valor(datos, encabezado, "programa", ""),
                    valor(datos, encabezado, "correo", ""),
                    valor(datos, encabezado, "telefono", ""),
                    valor(datos, encabezado, "cohorte", ""),
                    valor(datos, encabezado, "pensum", ""),
                    valor(datos, encabezado, "periodo", ""),
                    Integer.parseInt(datos[idxAprobados].trim()),
                    Integer.parseInt(datos[idxTotales].trim()),
                    Double.parseDouble(datos[idxPromedio].trim()),
                    requisitos,
                    condicion,
                    observaciones
            ));
        }

        return lista;
    }

    public void guardar(List<Estudiante> estudiantes) throws IOException {
        if (encabezado == null) {
            List<String> existentes = Files.readAllLines(
                    ruta, StandardCharsets.UTF_8);

            if (existentes.isEmpty()) {
                throw new IOException(
                        "El CSV no tiene encabezados.");
            }

            encabezado = ampliarEncabezado(
                    existentes.get(0).replace("\uFEFF", "").split(SEP, -1));
        }

        List<String> lineas = new ArrayList<>();
        lineas.add(String.join(SEP, encabezado));

        for (Estudiante e : estudiantes) {
            Map<String, String> valores = new LinkedHashMap<>();

            valores.put("codigo", e.getCodigo());
            valores.put("nombre", e.getNombre());
            valores.put("programa", e.getPrograma());
            valores.put("correo", e.getCorreo());
            valores.put("telefono", e.getTelefono());
            valores.put("cohorte", e.getCohorte());
            valores.put("pensum", e.getPensum());
            valores.put("periodo", e.getPeriodo());
            valores.put("creditos_aprobados",
                    String.valueOf(e.getCreditosAprobados()));
            valores.put("creditos_totales",
                    String.valueOf(e.getCreditosTotales()));
            valores.put("promedio", String.valueOf(e.getPromedio()));
            valores.put("condicion", e.getCondicion());
            valores.put("observaciones", e.getObservaciones());

            for (Map.Entry<String, Boolean> req
                    : e.getRequisitos().entrySet()) {
                valores.put(normalizar(req.getKey()),
                        String.valueOf(req.getValue()));
            }

            List<String> celdas = new ArrayList<>();

            for (String columna : encabezado) {
                String dato = valores.getOrDefault(
                        normalizar(columna), "");

                // Evitar que un punto y coma desplace otras columnas.
                dato = dato.replace(";", ",")
                        .replace("\r", " ")
                        .replace("\n", " ");

                celdas.add(dato);
            }

            lineas.add(String.join(SEP, celdas));
        }

        Files.write(ruta, lineas, StandardCharsets.UTF_8);
    }

    public int registrarNuevos(Path archivo,
                               String programa,
                               String periodo) throws IOException {

        if (programa == null || programa.isBlank()
                || periodo == null || periodo.isBlank()) {
            throw new IOException(
                    "Debe indicar el programa y el periodo académico.");
        }

        List<String> lineasArchivo = Files.readAllLines(
                archivo, StandardCharsets.UTF_8);

        if (lineasArchivo.isEmpty()) {
            throw new IOException("El archivo seleccionado está vacío.");
        }

        String[] cabeceraArchivo = lineasArchivo.get(0)
                .replace("\uFEFF", "").split(SEP, -1);

        for (int i = 0; i < cabeceraArchivo.length; i++) {
            cabeceraArchivo[i] = cabeceraArchivo[i].trim();
        }

        // Verificar que el archivo tenga todas las columnas requeridas.
        String[][] alias = {
            {"codigo", "codigo_estudiante"},
            {"nombre", "nombre_completo"},
            {"correo", "email"},
            {"telefono", "celular"},
            {"cohorte"},
            {"pensum", "curriculum", "plan_de_estudios"},
            {"creditos_aprobados"}
        };

        for (String[] opciones : alias) {
            boolean encontrada = false;

            for (String opcion : opciones) {
                if (indice(cabeceraArchivo, opcion) >= 0) {
                    encontrada = true;
                    break;
                }
            }

            if (!encontrada) {
                throw new IOException(
                        "Falta una columna obligatoria: "
                        + opciones[0]);
            }
        }

        List<Estudiante> actuales = cargar();

        Set<String> codigos = new HashSet<>();

        for (Estudiante e : actuales) {
            codigos.add(e.getCodigo().trim());
        }

        List<Estudiante> nuevos = new ArrayList<>();

        for (int i = 1; i < lineasArchivo.size(); i++) {
            String linea = lineasArchivo.get(i).trim();

            if (linea.isEmpty()) {
                continue;
            }

            String[] datos = linea.split(SEP, -1);

            String codigo = valorAlternativo(
                    datos, cabeceraArchivo, "",
                    "codigo", "codigo_estudiante");

            String nombre = valorAlternativo(
                    datos, cabeceraArchivo, "",
                    "nombre", "nombre_completo");

            String correo = valorAlternativo(
                    datos, cabeceraArchivo, "",
                    "correo", "email");

            String telefono = valorAlternativo(
                    datos, cabeceraArchivo, "",
                    "telefono", "celular");

            String cohorte = valorAlternativo(
                    datos, cabeceraArchivo, "", "cohorte");

            String pensum = valorAlternativo(
                    datos, cabeceraArchivo, "",
                    "pensum", "curriculum", "plan_de_estudios");

            String aprobadosTexto = valorAlternativo(
                    datos, cabeceraArchivo, "",
                    "creditos_aprobados");

            if (codigo.isBlank() || nombre.isBlank()
                    || correo.isBlank() || telefono.isBlank()
                    || cohorte.isBlank() || pensum.isBlank()
                    || aprobadosTexto.isBlank()) {
                throw new IOException(
                        "Hay datos obligatorios vacíos en la fila "
                        + (i + 1) + ".");
            }

            if (!codigos.add(codigo)) {
                throw new IOException(
                        "El código " + codigo
                        + " está duplicado en el archivo o ya existe "
                        + "en el sistema. No se importó ningún registro.");
            }

            int creditosAprobados;

            try {
                creditosAprobados = Integer.parseInt(aprobadosTexto);
            } catch (NumberFormatException ex) {
                throw new IOException(
                        "Los créditos aprobados deben ser un entero "
                        + "en la fila " + (i + 1) + ".");
            }

            if (creditosAprobados < 0) {
                throw new IOException(
                        "Los créditos aprobados no pueden ser negativos. "
                        + "Fila: " + (i + 1) + ".");
            }

            Map<String, Boolean> requisitos = new LinkedHashMap<>();

            for (String columna : encabezado) {
                String normalizada = normalizar(columna);

                if (!COLUMNAS_FIJAS.contains(normalizada)) {
                    requisitos.put(columna, false);
                }
            }

            nuevos.add(new Estudiante(
                    codigo, nombre, programa,
                    correo, telefono, cohorte, pensum, periodo,
                    creditosAprobados, 0, 0.0,
                    requisitos,
                    Estudiante.PENDIENTE,
                    "Registro importado; faltan datos de evaluación."
            ));
        }

        if (nuevos.isEmpty()) {
            throw new IOException(
                    "No hay registros de estudiantes para importar.");
        }

        actuales.addAll(nuevos);
        guardar(actuales);

        return nuevos.size();
    }
}
