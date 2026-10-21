import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Student {

    static class StudentRecord {
        int rollNo;
        String name;
        double marks;

        StudentRecord(int rollNo, String name, double marks) {
            this.rollNo = rollNo;
            this.name = name;
            this.marks = marks;
        }
    }

    static final List<StudentRecord> students = new ArrayList<>();
    static final Stack<StudentRecord> deletedStudents = new Stack<>();

    static final Path ROOT = Paths.get(".")
            .toAbsolutePath().normalize();

    public static void main(String[] args) throws Exception {

        int port = Integer.parseInt(
                System.getenv().getOrDefault("PORT", "8080")
        );

        HttpServer server = HttpServer.create(
                new InetSocketAddress("0.0.0.0", port), 0
        );

        server.createContext("/api/students", Student::handleApi);
        server.createContext("/", Student::handleFiles);

        server.start();

        System.out.println("Student Record Management System started!");
        System.out.println("Listening on port: " + port);
    }

    // LINEAR SEARCH
    static StudentRecord searchStudent(int rollNo) {
        for (StudentRecord s : students) {
            if (s.rollNo == rollNo) {
                return s;
            }
        }
        return null;
    }

    // JSON ESCAPING
    static String escapeJson(String text) {
        return text.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    static String studentJson(StudentRecord s) {
        return "{\"rollNo\":" + s.rollNo
                + ",\"name\":\"" + escapeJson(s.name) + "\""
                + ",\"marks\":" + s.marks + "}";
    }

    static String listJson(List<StudentRecord> list) {
        StringBuilder json = new StringBuilder("[");

        for (int i = 0; i < list.size(); i++) {
            if (i > 0) {
                json.append(",");
            }
            json.append(studentJson(list.get(i)));
        }

        return json.append("]").toString();
    }

    static String allDataJson() {
        return "{\"students\":" + listJson(students)
                + ",\"deletedStudents\":" + listJson(deletedStudents)
                + "}";
    }

    // READ REQUEST FIELDS
    static String field(String json, String key) {
        Pattern pattern = Pattern.compile(
                "\"" + Pattern.quote(key)
                + "\"\\s*:\\s*(?:\"((?:\\\\.|[^\"\\\\])*)\"|([^,}\\s]+))"
        );

        Matcher matcher = pattern.matcher(json);

        if (!matcher.find()) {
            return null;
        }

        if (matcher.group(1) != null) {
            return matcher.group(1)
                    .replace("\\\"", "\"")
                    .replace("\\\\", "\\")
                    .replace("\\n", "\n")
                    .replace("\\r", "\r")
                    .replace("\\t", "\t");
        }

        return matcher.group(2);
    }

    static void sendJson(
            HttpExchange exchange, int status, String body
    ) throws IOException {

        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);

        exchange.getResponseHeaders().set(
                "Content-Type", "application/json; charset=UTF-8"
        );

        exchange.getResponseHeaders().set(
                "Cache-Control", "no-store"
        );

        exchange.sendResponseHeaders(status, bytes.length);

        try (var output = exchange.getResponseBody()) {
            output.write(bytes);
        }
    }

    // API HANDLER
    static void handleApi(HttpExchange exchange) throws IOException {

        try {
            if ("GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendJson(exchange, 200, allDataJson());
                return;
            }

            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendJson(exchange, 405,
                        "{\"error\":\"Method not allowed.\"}");
                return;
            }

            String body = new String(
                    exchange.getRequestBody().readAllBytes(),
                    StandardCharsets.UTF_8
            );

            String action = field(body, "action");

            if (action == null) {
                sendJson(exchange, 400,
                        "{\"error\":\"Missing action.\"}");
                return;
            }

            String message;

            switch (action) {

                case "add": {
                    String rollText = field(body, "rollNo");
                    String name = field(body, "name");
                    String marksText = field(body, "marks");

                    if (rollText == null || name == null
                            || marksText == null || name.isBlank()) {
                        sendJson(exchange, 400,
                                "{\"error\":\"Fill in all fields.\"}");
                        return;
                    }

                    int rollNo;
                    double marks;

                    try {
                        rollNo = Integer.parseInt(rollText);
                        marks = Double.parseDouble(marksText);
                    } catch (NumberFormatException e) {
                        sendJson(exchange, 400,
                                "{\"error\":\"Invalid number.\"}");
                        return;
                    }

                    if (rollNo < 1 || !Double.isFinite(marks)
                            || marks < 0 || marks > 100) {
                        sendJson(exchange, 400,
                                "{\"error\":\"Invalid roll number or marks.\"}");
                        return;
                    }

                    if (searchStudent(rollNo) != null) {
                        sendJson(exchange, 400,
                                "{\"error\":\"Roll number already exists.\"}");
                        return;
                    }

                    students.add(new StudentRecord(
                            rollNo, name.trim(), marks
                    ));

                    message = "Student added successfully.";
                    break;
                }

                case "delete": {
                    String rollText = field(body, "rollNo");

                    if (rollText == null) {
                        sendJson(exchange, 400,
                                "{\"error\":\"Missing roll number.\"}");
                        return;
                    }

                    int rollNo = Integer.parseInt(rollText);
                    StudentRecord student = searchStudent(rollNo);

                    if (student == null) {
                        sendJson(exchange, 404,
                                "{\"error\":\"Student not found.\"}");
                        return;
                    }

                    students.remove(student);
                    deletedStudents.push(student);

                    message = "Student deleted successfully.";
                    break;
                }

                case "restore": {
                    String rollText = field(body, "rollNo");

                    if (rollText == null) {
                        sendJson(exchange, 400,
                                "{\"error\":\"Missing roll number.\"}");
                        return;
                    }

                    int rollNo = Integer.parseInt(rollText);

                    if (deletedStudents.empty()) {
                        sendJson(exchange, 400,
                                "{\"error\":\"No deleted records.\"}");
                        return;
                    }

                    StudentRecord latest = deletedStudents.peek();

                    if (latest.rollNo != rollNo) {
                        sendJson(exchange, 400,
                                "{\"error\":\"Restore the most recently deleted student first.\"}");
                        return;
                    }

                    if (searchStudent(rollNo) != null) {
                        sendJson(exchange, 400,
                                "{\"error\":\"Roll number already exists.\"}");
                        return;
                    }

                    students.add(deletedStudents.pop());

                    message = "Student restored successfully.";
                    break;
                }

                case "sort": {
                    // BUBBLE SORT: descending order of marks
                    for (int i = 0; i < students.size() - 1; i++) {
                        for (int j = 0;
                             j < students.size() - i - 1; j++) {

                            if (students.get(j).marks
                                    < students.get(j + 1).marks) {

                                StudentRecord temp = students.get(j);

                                students.set(
                                        j, students.get(j + 1)
                                );

                                students.set(j + 1, temp);
                            }
                        }
                    }

                    message = "Students sorted by marks.";
                    break;
                }

                default:
                    sendJson(exchange, 400,
                            "{\"error\":\"Unknown action.\"}");
                    return;
            }

            sendJson(exchange, 200,
                    "{\"message\":\"" + escapeJson(message) + "\"}");

        } catch (NumberFormatException e) {
            sendJson(exchange, 400,
                    "{\"error\":\"Invalid number format.\"}");
        } catch (Exception e) {
            e.printStackTrace();

            sendJson(exchange, 500,
                    "{\"error\":\"Internal server error.\"}");
        }
    }

    // SERVE FRONTEND FILES
    static void handleFiles(HttpExchange exchange) throws IOException {

        if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(405, -1);
            exchange.close();
            return;
        }

        String urlPath = exchange.getRequestURI().getPath();

        if (urlPath.equals("/")) {
            urlPath = "/index.html";
        }

        Path file = ROOT.resolve(urlPath.substring(1)).normalize();

        if (!file.startsWith(ROOT) || !Files.isRegularFile(file)) {
            byte[] bytes = "404 - File not found."
                    .getBytes(StandardCharsets.UTF_8);

            exchange.sendResponseHeaders(404, bytes.length);

            try (var output = exchange.getResponseBody()) {
                output.write(bytes);
            }

            return;
        }

        String contentType;

        if (urlPath.endsWith(".html")) {
            contentType = "text/html; charset=UTF-8";
        } else if (urlPath.endsWith(".css")) {
            contentType = "text/css; charset=UTF-8";
        } else if (urlPath.endsWith(".js")) {
            contentType = "text/javascript; charset=UTF-8";
        } else {
            contentType = "application/octet-stream";
        }

        byte[] bytes = Files.readAllBytes(file);

        exchange.getResponseHeaders().set(
                "Content-Type", contentType
        );

        exchange.getResponseHeaders().set(
                "Cache-Control", "no-store"
        );

        exchange.sendResponseHeaders(200, bytes.length);

        try (var output = exchange.getResponseBody()) {
            output.write(bytes);
        }
    }
}
