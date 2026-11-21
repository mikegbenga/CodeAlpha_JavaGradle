package com.codealpha.tasks;

import java.io.IOException;

/** Entry point: java -jar codealpha-task-api-all.jar (PORT env var, default 8080). */
public final class App {

    private App() {
    }

    public static void main(String[] args) throws IOException {
        int port = Integer.parseInt(System.getenv().getOrDefault("PORT", "8080"));
        String version = App.class.getPackage().getImplementationVersion();
        TaskApi api = new TaskApi(port, new TaskService(), version == null ? "dev" : version).start();
        System.out.printf("codealpha-task-api %s listening on http://localhost:%d%n",
                version == null ? "dev" : version, api.port());
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("shutting down");
            api.close();
        }));
    }
}
