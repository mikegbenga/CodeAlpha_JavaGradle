package com.codealpha.tasks;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Small JSON REST API using the HTTP server built into the JDK.
 *
 * <pre>
 * GET    /health              -> {"status":"UP", ...}
 * GET    /api/tasks           -> list tasks
 * POST   /api/tasks           -> {"title":"..."} creates a task (201)
 * GET    /api/tasks/{id}      -> one task (404 if missing)
 * PUT    /api/tasks/{id}/done -> mark done
 * DELETE /api/tasks/{id}      -> delete (204)
 * </pre>
 */
public final class TaskApi implements AutoCloseable {

    private static final Pattern TASK_PATH = Pattern.compile("^/api/tasks/(\\d+)(/done)?/?$");

    record NewTask(String title) {
    }

    record ErrorBody(String error) {
    }

    record Health(String status, String version, long tasks, long done, Instant startedAt) {
    }

    private final HttpServer server;
    private final TaskService service;
    private final String version;
    private final Instant startedAt = Instant.now();

    public TaskApi(int port, TaskService service, String version) throws IOException {
        this.service = service;
        this.version = version;
        this.server = HttpServer.create(new InetSocketAddress(port), 0);
        this.server.createContext("/health", this::health);
        this.server.createContext("/api/tasks", this::tasks);
        this.server.setExecutor(Executors.newFixedThreadPool(8));
    }

    public TaskApi start() {
        server.start();
        return this;
    }

    public int port() {
        return server.getAddress().getPort();
    }

    @Override
    public void close() {
        server.stop(1);
        ((java.util.concurrent.ExecutorService) server.getExecutor()).shutdown();
    }

    private void health(HttpExchange ex) throws IOException {
        if (!"GET".equals(ex.getRequestMethod())) {
            send(ex, 405, new ErrorBody("method not allowed"));
            return;
        }
        send(ex, 200, new Health("UP", version, service.list().size(), service.countDone(), startedAt));
    }

    private void tasks(HttpExchange ex) throws IOException {
        String method = ex.getRequestMethod();
        String path = ex.getRequestURI().getPath();
        try {
            if (path.equals("/api/tasks") || path.equals("/api/tasks/")) {
                switch (method) {
                    case "GET" -> send(ex, 200, service.list());
                    case "POST" -> {
                        NewTask body = Json.read(readBody(ex), NewTask.class);
                        send(ex, 201, service.create(body.title()));
                    }
                    default -> send(ex, 405, new ErrorBody("method not allowed"));
                }
                return;
            }
            Matcher m = TASK_PATH.matcher(path);
            if (!m.matches()) {
                send(ex, 404, new ErrorBody("not found"));
                return;
            }
            long id = Long.parseLong(m.group(1));
            boolean donePath = m.group(2) != null;
            if (donePath && "PUT".equals(method)) {
                sendOrNotFound(ex, service.complete(id).orElse(null));
            } else if (!donePath && "GET".equals(method)) {
                sendOrNotFound(ex, service.find(id).orElse(null));
            } else if (!donePath && "DELETE".equals(method)) {
                if (service.delete(id)) {
                    send(ex, 204, null);
                } else {
                    send(ex, 404, new ErrorBody("task " + id + " not found"));
                }
            } else {
                send(ex, 405, new ErrorBody("method not allowed"));
            }
        } catch (IllegalArgumentException e) {
            send(ex, 400, new ErrorBody(e.getMessage()));
        }
    }

    private void sendOrNotFound(HttpExchange ex, Task task) throws IOException {
        if (task == null) {
            send(ex, 404, new ErrorBody("task not found"));
        } else {
            send(ex, 200, task);
        }
    }

    private static String readBody(HttpExchange ex) throws IOException {
        try (InputStream in = ex.getRequestBody()) {
            return new String(in.readNBytes(10_000), StandardCharsets.UTF_8);
        }
    }

    private static void send(HttpExchange ex, int status, Object body) throws IOException {
        Map.of("Content-Type", "application/json; charset=utf-8",
                "X-Content-Type-Options", "nosniff")
                .forEach((k, v) -> ex.getResponseHeaders().set(k, v));
        if (body == null) {
            ex.sendResponseHeaders(status, -1);
            ex.close();
            return;
        }
        byte[] bytes = Json.write(body).getBytes(StandardCharsets.UTF_8);
        ex.sendResponseHeaders(status, bytes.length);
        try (OutputStream out = ex.getResponseBody()) {
            out.write(bytes);
        }
    }
}
