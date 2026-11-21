package com.codealpha.tasks;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/** Starts the real HTTP server on a random port and calls it like a client would. */
class TaskApiIT {

    private static TaskApi api;
    private static final HttpClient HTTP = HttpClient.newBuilder().proxy(HttpClient.Builder.NO_PROXY).build();

    @BeforeAll
    static void start() throws IOException {
        api = new TaskApi(0, new TaskService(), "test").start();
    }

    @AfterAll
    static void stop() {
        api.close();
    }

    private static HttpResponse<String> call(String method, String path, String body) throws Exception {
        HttpRequest.Builder b = HttpRequest.newBuilder(URI.create("http://localhost:" + api.port() + path));
        b.method(method, body == null ? BodyPublishers.noBody() : BodyPublishers.ofString(body));
        return HTTP.send(b.build(), BodyHandlers.ofString());
    }

    @Test
    void healthIsUp() throws Exception {
        var r = call("GET", "/health", null);
        assertEquals(200, r.statusCode());
        assertTrue(r.body().contains("\"status\":\"UP\""));
        assertTrue(r.body().contains("\"version\":\"test\""));
        assertEquals("nosniff", r.headers().firstValue("X-Content-Type-Options").orElse(""));
    }

    @Test
    void healthRejectsPost() throws Exception {
        assertEquals(405, call("POST", "/health", "{}").statusCode());
    }

    @Test
    void fullTaskLifecycle() throws Exception {
        var created = call("POST", "/api/tasks", "{\"title\":\"Learn Gradle\"}");
        assertEquals(201, created.statusCode());
        assertTrue(created.body().contains("\"title\":\"Learn Gradle\""));
        String id = created.body().replaceAll(".*\"id\":(\\d+).*", "$1");

        assertEquals(200, call("GET", "/api/tasks/" + id, null).statusCode());
        assertTrue(call("GET", "/api/tasks", null).body().contains("Learn Gradle"));

        var done = call("PUT", "/api/tasks/" + id + "/done", null);
        assertEquals(200, done.statusCode());
        assertTrue(done.body().contains("\"done\":true"));

        assertEquals(204, call("DELETE", "/api/tasks/" + id, null).statusCode());
        assertEquals(404, call("GET", "/api/tasks/" + id, null).statusCode());
        assertEquals(404, call("DELETE", "/api/tasks/" + id, null).statusCode());
        assertEquals(404, call("PUT", "/api/tasks/" + id + "/done", null).statusCode());
    }

    @Test
    void badInputGives400() throws Exception {
        assertEquals(400, call("POST", "/api/tasks", "{\"title\":\"\"}").statusCode());
        assertEquals(400, call("POST", "/api/tasks", "not json").statusCode());
        assertEquals(400, call("POST", "/api/tasks", "").statusCode());
    }

    @Test
    void unknownRoutesAndMethods() throws Exception {
        assertEquals(404, call("GET", "/api/tasks/abc", null).statusCode());
        assertEquals(405, call("PATCH", "/api/tasks", "{}").statusCode());
        assertEquals(405, call("POST", "/api/tasks/1", "{}").statusCode());
    }

    @Test
    void createdAtIsIsoTimestamp() throws Exception {
        var r = call("POST", "/api/tasks", "{\"title\":\"Check JSON\"}");
        assertTrue(r.body().matches(".*\"createdAt\":\"\\d{4}-\\d{2}-\\d{2}T.*Z\".*"), r.body());
    }
}
