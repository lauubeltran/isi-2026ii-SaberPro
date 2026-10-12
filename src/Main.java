import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.regex.Pattern;

public class Main {
    private static final Path WEB_ROOT = Path.of("web").toAbsolutePath().normalize();
    private static final Path DATA_FILE = Path.of("data", "estudiantes.tsv");
    private static final String EXPECTED_HEADER = "codigo\tnombre\tcorreo\ttelefono\tcohorte\tpensum\tcreditos_aprobados";
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");
    private static final Object DATA_LOCK = new Object();

    public static void main(String[] args) throws IOException {
        int port = args.length > 0 ? Integer.parseInt(args[0]) : 8080;
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", port), 0);
        server.createContext("/api/records", Main::handleRecords);
        server.createContext("/api/register", Main::handleRegistration);
        server.createContext("/", Main::handleStatic);
        server.setExecutor(Executors.newCachedThreadPool());
        server.start();
        System.out.println("Prototipo Saber Pro disponible en http://localhost:" + port);
    }

    private static void handleRecords(HttpExchange exchange) throws IOException {
        if (!"GET".equals(exchange.getRequestMethod())) {
            sendJson(exchange, 405, "{\"message\":\"Método no permitido.\"}");
            return;
        }
        try {
            List<Student> students;
            synchronized (DATA_LOCK) {
                students = readStudents();
            }
            StringBuilder json = new StringBuilder("{\"count\":").append(students.size()).append(",\"records\":[");
            for (int i = 0; i < students.size(); i++) {
                if (i > 0) json.append(',');
                json.append(students.get(i).toJson());
            }
            sendJson(exchange, 200, json.append("]}").toString());
        } catch (IOException exception) {
            sendJson(exchange, 500, "{\"message\":\"No se pudieron leer los registros locales.\"}");
        }
    }

    private static void handleRegistration(HttpExchange exchange) throws IOException {
        if (!"POST".equals(exchange.getRequestMethod())) {
            sendJson(exchange, 405, "{\"message\":\"Método no permitido.\"}");
            return;
        }
        String program = exchange.getRequestHeaders().getFirst("X-Program");
        String period = exchange.getRequestHeaders().getFirst("X-Period");
        String content = new String(exchange.getRequestBody().readNBytes(2_000_001), StandardCharsets.UTF_8);
        if (content.length() > 2_000_000) {
            sendJson(exchange, 413, "{\"message\":\"El archivo supera el límite de 2 MB.\"}");
            return;
        }
        List<String> errors = new ArrayList<>();
        if (!"Ingeniería de Sistemas".equals(program)) errors.add("Selecciona el programa Ingeniería de Sistemas.");
        if (period == null || !period.matches("20\\d{2}-(1|2)")) errors.add("Selecciona un periodo válido, por ejemplo 2026-2.");

        String[] lines = content.replace("\r", "").split("\n", -1);
        int headerLine = firstNonEmptyLine(lines);
        if (headerLine < 0 || !normalizeHeader(lines[headerLine]).equals(EXPECTED_HEADER)) {
            errors.add("La primera línea debe tener las columnas: " + EXPECTED_HEADER.replace('\t', ' '));
            sendJson(exchange, 400, validationJson(errors));
            return;
        }

        List<Student> incoming = new ArrayList<>();
        Set<String> seenCodes = new HashSet<>();
        for (int index = headerLine + 1; index < lines.length; index++) {
            String line = lines[index].trim();
            if (line.isEmpty()) continue;
            String[] fields = line.split("\t", -1);
            int lineNumber = index + 1;
            if (fields.length != 7) {
                errors.add("Línea " + lineNumber + ": se esperaban 7 columnas y se encontraron " + fields.length + ".");
                continue;
            }
            for (int fieldIndex = 0; fieldIndex < fields.length; fieldIndex++) fields[fieldIndex] = fields[fieldIndex].trim();
            if (fields[0].isEmpty() || fields[1].isEmpty() || fields[2].isEmpty() || fields[3].isEmpty()
                    || fields[4].isEmpty() || fields[5].isEmpty() || fields[6].isEmpty()) {
                errors.add("Línea " + lineNumber + ": todos los campos son obligatorios.");
                continue;
            }
            if (!EMAIL_PATTERN.matcher(fields[2]).matches()) {
                errors.add("Línea " + lineNumber + ": el correo no tiene un formato válido.");
                continue;
            }
            try {
                if (Integer.parseInt(fields[6]) < 0) throw new NumberFormatException();
            } catch (NumberFormatException exception) {
                errors.add("Línea " + lineNumber + ": créditos aprobados debe ser un entero igual o mayor que cero.");
                continue;
            }
            String codeKey = fields[0].toLowerCase(Locale.ROOT);
            if (!seenCodes.add(codeKey)) {
                errors.add("Línea " + lineNumber + ": el código " + fields[0] + " está repetido en el archivo.");
                continue;
            }
            incoming.add(new Student(fields, program, period, Instant.now().toString()));
        }
        if (incoming.isEmpty() && errors.isEmpty()) errors.add("El archivo no contiene estudiantes para registrar.");
        if (!errors.isEmpty()) {
            sendJson(exchange, 400, validationJson(errors));
            return;
        }

        synchronized (DATA_LOCK) {
            List<Student> existing = readStudents();
            Set<String> existingCodes = new HashSet<>();
            for (Student student : existing) existingCodes.add(student.code.toLowerCase(Locale.ROOT));
            for (Student student : incoming) {
                if (existingCodes.contains(student.code.toLowerCase(Locale.ROOT))) {
                    errors.add("El código " + student.code + " ya está registrado.");
                }
            }
            if (errors.isEmpty()) {
                existing.addAll(incoming);
                writeStudents(existing);
            }
        }
        if (!errors.isEmpty()) {
            sendJson(exchange, 409, validationJson(errors));
            return;
        }
        sendJson(exchange, 201, "{\"message\":\"Registros guardados como pendientes de evaluación.\",\"registered\":" + incoming.size() + "}");
    }

    private static int firstNonEmptyLine(String[] lines) {
        for (int i = 0; i < lines.length; i++) if (!lines[i].isBlank()) return i;
        return -1;
    }

    private static String normalizeHeader(String header) {
        return header.replace("\uFEFF", "").trim().toLowerCase(Locale.ROOT);
    }

    private static String validationJson(List<String> errors) {
        StringBuilder json = new StringBuilder("{\"message\":\"No se guardó ningún registro.\",\"errors\":[");
        for (int i = 0; i < errors.size(); i++) {
            if (i > 0) json.append(',');
            json.append(quote(errors.get(i)));
        }
        return json.append("]}").toString();
    }

    private static List<Student> readStudents() throws IOException {
        List<Student> students = new ArrayList<>();
        if (!Files.exists(DATA_FILE)) return students;
        List<String> lines = Files.readAllLines(DATA_FILE, StandardCharsets.UTF_8);
        for (int i = 1; i < lines.size(); i++) {
            String[] fields = lines.get(i).split("\t", -1);
            if (fields.length == 11) students.add(Student.fromStored(fields));
        }
        return students;
    }

    private static void writeStudents(List<Student> students) throws IOException {
        Files.createDirectories(DATA_FILE.getParent());
        List<String> lines = new ArrayList<>();
        lines.add("fecha\tcodigo\tnombre\tcorreo\ttelefono\tcohorte\tpensum\tcreditos_aprobados\tprograma\tperiodo\testado");
        for (Student student : students) lines.add(student.toStoredLine());
        Path temporaryFile = DATA_FILE.resolveSibling(DATA_FILE.getFileName() + ".tmp");
        Files.write(temporaryFile, lines, StandardCharsets.UTF_8);
        try {
            Files.move(temporaryFile, DATA_FILE, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException exception) {
            Files.move(temporaryFile, DATA_FILE, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private static void handleStatic(HttpExchange exchange) throws IOException {
        if (!"GET".equals(exchange.getRequestMethod())) {
            sendText(exchange, 405, "Método no permitido.", "text/plain; charset=utf-8");
            return;
        }
        String requestedPath = exchange.getRequestURI().getPath();
        if ("/".equals(requestedPath)) requestedPath = "/index.html";
        Path file = WEB_ROOT.resolve(requestedPath.substring(1)).normalize();
        if (!file.startsWith(WEB_ROOT) || !Files.isRegularFile(file)) {
            sendText(exchange, 404, "No encontrado.", "text/plain; charset=utf-8");
            return;
        }
        String name = file.getFileName().toString();
        String type = name.endsWith(".html") ? "text/html; charset=utf-8"
                : name.endsWith(".css") ? "text/css; charset=utf-8"
                : name.endsWith(".js") ? "text/javascript; charset=utf-8"
                : "text/plain; charset=utf-8";
        byte[] body = Files.readAllBytes(file);
        exchange.getResponseHeaders().set("Content-Type", type);
        exchange.getResponseHeaders().set("X-Content-Type-Options", "nosniff");
        exchange.sendResponseHeaders(200, body.length);
        exchange.getResponseBody().write(body);
        exchange.close();
    }

    private static void sendJson(HttpExchange exchange, int status, String body) throws IOException {
        sendText(exchange, status, body, "application/json; charset=utf-8");
    }

    private static void sendText(HttpExchange exchange, int status, String body, String contentType) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", contentType);
        exchange.getResponseHeaders().set("Cache-Control", "no-store");
        exchange.sendResponseHeaders(status, bytes.length);
        exchange.getResponseBody().write(bytes);
        exchange.close();
    }

    private static String quote(String value) {
        StringBuilder result = new StringBuilder("\"");
        for (char character : value.toCharArray()) {
            switch (character) {
                case '"' -> result.append("\\\"");
                case '\\' -> result.append("\\\\");
                case '\n' -> result.append("\\n");
                case '\r' -> result.append("\\r");
                case '\t' -> result.append("\\t");
                default -> {
                    if (character < 0x20) result.append(String.format("\\u%04x", (int) character));
                    else result.append(character);
                }
            }
        }
        return result.append('"').toString();
    }

    private static class Student {
        private final String code;
        private final String name;
        private final String email;
        private final String phone;
        private final String cohort;
        private final String curriculum;
        private final String credits;
        private final String program;
        private final String period;
        private final String registeredAt;

        private Student(String[] fields, String program, String period, String registeredAt) {
            this(fields[0], fields[1], fields[2], fields[3], fields[4], fields[5], fields[6], program, period, registeredAt);
        }

        private Student(String code, String name, String email, String phone, String cohort, String curriculum,
                        String credits, String program, String period, String registeredAt) {
            this.code = code;
            this.name = name;
            this.email = email;
            this.phone = phone;
            this.cohort = cohort;
            this.curriculum = curriculum;
            this.credits = credits;
            this.program = program;
            this.period = period;
            this.registeredAt = registeredAt;
        }

        private String toJson() {
            return "{\"code\":" + quote(code) + ",\"name\":" + quote(name) + ",\"email\":" + quote(email)
                    + ",\"phone\":" + quote(phone) + ",\"cohort\":" + quote(cohort) + ",\"curriculum\":" + quote(curriculum)
                    + ",\"credits\":" + quote(credits) + ",\"program\":" + quote(program) + ",\"period\":" + quote(period)
                    + ",\"registeredAt\":" + quote(registeredAt) + ",\"status\":\"Pendiente de evaluación\"}";
        }

        private String toStoredLine() {
            return String.join("\t", registeredAt, code, name, email, phone, cohort, curriculum, credits,
                    program, period, "Pendiente de evaluación");
        }

        private static Student fromStored(String[] fields) {
            return new Student(fields[1], fields[2], fields[3], fields[4], fields[5], fields[6], fields[7],
                    fields[8], fields[9], fields[0]);
        }
    }
}