import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class RepositorioEstudiantes {
    private static final String SEP = ";";
    private final Path ruta;
    private String[] encabezado;
    private int idxCond;
    private int idxObs;

    public RepositorioEstudiantes(String ruta) {
        this.ruta = Paths.get(ruta);
    }

    private int indice(String nombre) {
        for (int i = 0; i < encabezado.length; i++) {
            if (encabezado[i].equalsIgnoreCase(nombre)) return i;
        }
        return -1;
    }

    public List<Estudiante> cargar() throws IOException {
        List<String> lineas = Files.readAllLines(ruta, StandardCharsets.UTF_8);
        List<Estudiante> lista = new ArrayList<>();
        if (lineas.isEmpty()) return lista;

        encabezado = lineas.get(0).replace("\uFEFF", "").split(SEP, -1);
        for (int i = 0; i < encabezado.length; i++) encabezado[i] = encabezado[i].trim();

        idxCond = indice("condicion");
        idxObs = indice("observaciones");
        if (idxCond < 0) idxCond = idxObs >= 0 ? idxObs - 1 : encabezado.length - 1;

        for (int i = 1; i < lineas.size(); i++) {
            String linea = lineas.get(i).trim();
            if (linea.isEmpty()) continue;
            String[] c = linea.split(SEP, -1);

            Map<String, Boolean> req = new LinkedHashMap<>();
            for (int j = 6; j < idxCond && j < c.length; j++) {
                req.put(encabezado[j], Boolean.parseBoolean(c[j].trim()));
            }
            String cond = idxCond < c.length ? c[idxCond].trim() : Estudiante.PENDIENTE;
            if (cond.isEmpty()) cond = Estudiante.PENDIENTE;
            String obs = (idxObs >= 0 && idxObs < c.length) ? c[idxObs].trim() : "";

            lista.add(new Estudiante(
                    c[0].trim(), c[1].trim(), c[2].trim(),
                    Integer.parseInt(c[3].trim()),
                    Integer.parseInt(c[4].trim()),
                    Double.parseDouble(c[5].trim()),
                    req, cond, obs));
        }
        return lista;
    }

    public void guardar(List<Estudiante> estudiantes) throws IOException {
        List<String> cab = new ArrayList<>(Arrays.asList(encabezado));
        if (idxObs < 0) cab.add("observaciones");

        List<String> lineas = new ArrayList<>();
        lineas.add(String.join(SEP, cab));
        for (Estudiante e : estudiantes) {
            StringBuilder sb = new StringBuilder();
            sb.append(e.getCodigo()).append(SEP)
              .append(e.getNombre()).append(SEP)
              .append(e.getPrograma()).append(SEP)
              .append(e.getCreditosAprobados()).append(SEP)
              .append(e.getCreditosTotales()).append(SEP)
              .append(e.getPromedio()).append(SEP);
            for (boolean v : e.getRequisitos().values()) sb.append(v).append(SEP);
            sb.append(e.getCondicion()).append(SEP);
            sb.append(e.getObservaciones().replace(";", ",").replace("\r", " ").replace("\n", " "));
            lineas.add(sb.toString());
        }
        Files.write(ruta, lineas, StandardCharsets.UTF_8);
    }
}