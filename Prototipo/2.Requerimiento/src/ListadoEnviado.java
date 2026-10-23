import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Listado de habilitados que el Director envía al Coordinador.
 *
 * Se guarda como «listado_enviado.csv» junto a estudiantes.csv. Cada envío reemplaza el
 * anterior (es una foto del listado en ese momento).
 *
 * Formato: codigo;nombre;programa;condicion;correo;fecha_envio
 */
public final class ListadoEnviado {
    private static final String CABECERA = "codigo;nombre;programa;condicion;correo;fecha_envio";
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private ListadoEnviado() { }

    /** Un estudiante del listado enviado. */
    public record Fila(String codigo, String nombre, String programa, String correo) { }

    /** El listado completo y la fecha en que se envió (texto «yyyy-MM-dd HH:mm»). */
    public record Listado(String fechaEnvio, List<Fila> filas) { }

    public static Path ruta(String rutaCsv) {
        return Paths.get(rutaCsv).toAbsolutePath().resolveSibling("listado_enviado.csv");
    }

    /** Guarda a los estudiantes dados como el listado enviado al Coordinador. */
    public static void guardar(String rutaCsv, List<Estudiante> habilitados) throws IOException {
        String fecha = LocalDateTime.now().format(FMT);
        List<String> lineas = new ArrayList<>();
        lineas.add(CABECERA);
        for (Estudiante e : habilitados) {
            lineas.add(String.join(";",
                    limpiar(e.getCodigo()), limpiar(e.getNombre()), limpiar(e.getPrograma()),
                    Estudiante.HABILITADO, limpiar(e.getCorreo()), fecha));
        }
        Path destino = ruta(rutaCsv);
        Files.createDirectories(destino.getParent());
        // Se escribe en un archivo temporal y luego se reemplaza, para no dejar el listado a medias.
        Path temporal = destino.resolveSibling("listado_enviado.csv.tmp");
        Files.write(temporal, lineas, StandardCharsets.UTF_8);
        Files.move(temporal, destino, StandardCopyOption.REPLACE_EXISTING);
    }

    /** Lee el último listado enviado; devuelve null si todavía no se ha enviado ninguno. */
    public static Listado leer(String rutaCsv) throws IOException {
        Path archivo = ruta(rutaCsv);
        if (!Files.exists(archivo)) return null;
        List<String> lineas = Files.readAllLines(archivo, StandardCharsets.UTF_8);
        if (lineas.size() < 2) return null;

        String[] cab = lineas.get(0).replace("﻿", "").split(";", -1);
        int iCodigo = indice(cab, "codigo");
        int iNombre = indice(cab, "nombre");
        int iPrograma = indice(cab, "programa");
        int iCorreo = indice(cab, "correo");
        int iFecha = indice(cab, "fecha_envio");
        if (iCodigo < 0) return null;

        List<Fila> filas = new ArrayList<>();
        String fecha = "";
        for (int i = 1; i < lineas.size(); i++) {
            String linea = lineas.get(i).trim();
            if (linea.isEmpty()) continue;
            String[] d = linea.split(";", -1);
            String codigo = campo(d, iCodigo);
            if (codigo.isEmpty()) continue;
            filas.add(new Fila(codigo, campo(d, iNombre), campo(d, iPrograma), campo(d, iCorreo)));
            if (fecha.isEmpty()) fecha = campo(d, iFecha);
        }
        return filas.isEmpty() ? null : new Listado(fecha, filas);
    }

    /** «yyyy-MM-dd HH:mm» → «dd/MM/yyyy HH:mm» (si no se puede convertir, lo devuelve igual). */
    public static String fechaLegible(String guardada) {
        try {
            return LocalDateTime.parse(guardada, FMT).format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
        } catch (RuntimeException ex) {
            return guardada;
        }
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

    /** El separador es «;», así que se quitan los «;» y los saltos de línea de los textos. */
    private static String limpiar(String s) {
        return s == null ? "" : s.replace(';', ',').replace('\r', ' ').replace('\n', ' ').trim();
    }
}