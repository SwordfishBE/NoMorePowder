package net.nomorepowder.util;

import com.sun.net.httpserver.HttpServer;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.Logger;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.Property;

import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpRequest;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

public final class ModrinthUpdateCheckerRuntimeTest {
    private static int passed;

    public static void main(String[] args) throws Exception {
        AtomicReference<Response> response = new AtomicReference<>(new Response(200, "[]"));
        CountDownLatch releaseSlowResponse = new CountDownLatch(1);
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/versions", exchange -> {
            try (exchange) {
                Response current = response.get();
                if (current.status() == 0) {
                    try {
                        releaseSlowResponse.await();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                    return;
                }
                byte[] body = current.body().getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().set("Content-Type", "application/json");
                exchange.sendResponseHeaders(current.status(), body.length);
                exchange.getResponseBody().write(body);
            }
        });
        Logger logger = (Logger) LogManager.getLogger("nomorepowder");
        Level previousLevel = logger.getLevel();
        CapturingAppender appender = new CapturingAppender();
        appender.start();
        logger.addAppender(appender);
        logger.setLevel(Level.DEBUG);
        server.start();
        URI endpoint = URI.create("http://127.0.0.1:" + server.getAddress().getPort() + "/versions");
        try {
            for (int status : new int[]{400, 403, 404, 429, 500, 503}) {
                run(response, appender, endpoint, status, "<html>error</html>", Level.DEBUG, "HTTP " + status);
            }
            for (String body : List.of("{broken", "[{", "<html>not JSON</html>")) {
                run(response, appender, endpoint, 200, body, Level.DEBUG, "invalid JSON");
            }
            for (String body : List.of("", "null", "{}", "42", "[]", "[null,42,[],{}]")) {
                run(response, appender, endpoint, 200, body, Level.DEBUG, "no usable versions");
            }
            String valid = release("1.1.6", "26.3", "fabric");
            for (String field : List.of("version_number", "version_type", "date_published", "loaders", "game_versions")) {
                for (String invalid : List.of("null", "{}", "[]", "42", "true")) {
                    var object = com.google.gson.JsonParser.parseString(valid).getAsJsonObject();
                    object.add(field, com.google.gson.JsonParser.parseString(invalid));
                    run(response, appender, endpoint, 200, "[" + object + "]", Level.DEBUG, "no usable versions");
                }
                var object = com.google.gson.JsonParser.parseString(valid).getAsJsonObject();
                object.remove(field);
                run(response, appender, endpoint, 200, "[" + object + "]", Level.DEBUG, "no usable versions");
            }
            run(response, appender, endpoint, 200, "[" + valid.replace("2026-10-22T00:00:00Z", "not-a-date") + "]",
                    Level.DEBUG, "no usable versions");
            run(response, appender, endpoint, 200, "[" + release("9.0.0", "26.4", "fabric") + "]",
                    Level.DEBUG, "no usable versions");
            run(response, appender, endpoint, 200, "[" + release("9.0.0", "26.3", "neoforge") + "]",
                    Level.DEBUG, "no usable versions");
            run(response, appender, endpoint, 200, "[" + valid.replace("\"release\"", "\"beta\"") + "]",
                    Level.DEBUG, "no usable versions");
            run(response, appender, endpoint, 200, "[" + valid + "]", Level.INFO, "New version available: 1.1.6");
            run(response, appender, endpoint, 200, "[" + release("1.1.5", "26.3", "fabric") + "]", null, null);
            run(response, appender, endpoint, 200, "[" + release("1.1.4", "26.3", "fabric") + "]", null, null);
            run(response, appender, endpoint, 200,
                    "[" + valid.replace("[\"fabric\"]", "[null,{},42,\"fabric\"]") + ","
                            + release("9.0.0", "26.4", "fabric") + "]",
                    Level.INFO, "New version available: 1.1.6");
            run(response, appender, endpoint, 0, "", Level.DEBUG, "Update check failed");
        } finally {
            releaseSlowResponse.countDown();
            server.stop(0);
            logger.removeAppender(appender);
            logger.setLevel(previousLevel);
            appender.stop();
        }
        System.out.println("Update checker runtime tests passed: " + passed);
    }

    private static void run(AtomicReference<Response> response, CapturingAppender appender, URI endpoint,
                            int status, String body, Level level, String message) {
        response.set(new Response(status, body));
        appender.events.clear();
        HttpRequest request = HttpRequest.newBuilder(endpoint)
                .timeout(Duration.ofMillis(status == 0 ? 200 : 5000)).GET().build();
        ModrinthUpdateChecker.checkForUpdate(request, "26.3", "1.1.5");
        if (level == null) {
            if (!appender.events.isEmpty()) {
                throw new AssertionError("Unexpected log for up-to-date version: " + body);
            }
        } else if (appender.events.size() != 1 || appender.events.getFirst().getLevel() != level
                || !appender.events.getFirst().getMessage().getFormattedMessage().contains(message)) {
            throw new AssertionError("Unexpected log for HTTP " + status + ": " + body + " -> " + appender.events);
        }
        passed++;
    }

    private static String release(String version, String minecraft, String loader) {
        return """
                {"version_number":"%s","version_type":"release","date_published":"2026-10-22T00:00:00Z",
                 "loaders":["%s"],"game_versions":["%s"]}
                """.formatted(version, loader, minecraft);
    }

    private record Response(int status, String body) {
    }

    private static final class CapturingAppender extends AbstractAppender {
        private final List<LogEvent> events = new ArrayList<>();

        private CapturingAppender() {
            super("update-checker-test", null, null, false, Property.EMPTY_ARRAY);
        }

        @Override
        public void append(LogEvent event) {
            events.add(event.toImmutable());
        }
    }
}
