package com.example.worldclocks;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

public final class WorldClockServer {
    private static final int PORT = 8080;
    private static final int CITY_COUNT = 8;

    private static final List<City> CITIES = List.of(
            new City("Tokyo", "Japan", "Asia/Tokyo"),
            new City("London", "United Kingdom", "Europe/London"),
            new City("Paris", "France", "Europe/Paris"),
            new City("Dubai", "United Arab Emirates", "Asia/Dubai"),
            new City("Singapore", "Singapore", "Asia/Singapore"),
            new City("Sydney", "Australia", "Australia/Sydney"),
            new City("Auckland", "New Zealand", "Pacific/Auckland"),
            new City("Honolulu", "United States", "Pacific/Honolulu"),
            new City("New York", "United States", "America/New_York"),
            new City("Los Angeles", "United States", "America/Los_Angeles"),
            new City("Chicago", "United States", "America/Chicago"),
            new City("Denver", "United States", "America/Denver"),
            new City("Sao Paulo", "Brazil", "America/Sao_Paulo"),
            new City("Mexico City", "Mexico", "America/Mexico_City"),
            new City("Johannesburg", "South Africa", "Africa/Johannesburg"),
            new City("Cairo", "Egypt", "Africa/Cairo"),
            new City("Istanbul", "Turkey", "Europe/Istanbul"),
            new City("Bangkok", "Thailand", "Asia/Bangkok"),
            new City("Seoul", "South Korea", "Asia/Seoul"),
            new City("Vancouver", "Canada", "America/Vancouver"),
            new City("Moscow", "Russia", "Europe/Moscow")
    );

    private WorldClockServer() {
    }

    public static void main(String[] args) throws IOException {
        AtomicReference<List<City>> selectedCities = new AtomicReference<>(chooseCities(null));
        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
        ExecutorService requestExecutor = Executors.newFixedThreadPool(4);
        HttpServer server = HttpServer.create(new InetSocketAddress(PORT), 0);

        server.createContext("/", exchange -> handleRequest(exchange, selectedCities));
        server.setExecutor(requestExecutor);
        scheduler.scheduleAtFixedRate(() -> {
            selectedCities.set(chooseCities(selectedCities.get()));
            System.out.println("Selected a new set of world clock cities.");
        }, 5, 5, TimeUnit.MINUTES);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            server.stop(1);
            scheduler.shutdown();
            requestExecutor.shutdown();
        }, "world-clock-shutdown"));

        server.start();
        System.out.println("World clock API is running at http://localhost:" + PORT + "/api/clocks");
    }

    private static List<City> chooseCities(List<City> previousSelection) {
        List<City> shuffled = new ArrayList<>(CITIES);
        Collections.shuffle(shuffled);
        List<City> selected = new ArrayList<>(shuffled.subList(0, CITY_COUNT));
        if (previousSelection != null
                && previousSelection.containsAll(selected)
                && selected.containsAll(previousSelection)) {
            City replacement = shuffled.stream()
                    .filter(city -> !previousSelection.contains(city))
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException("City catalog has no replacement."));
            selected.set(selected.size() - 1, replacement);
        }
        return List.copyOf(selected);
    }

    private static void handleRequest(HttpExchange exchange, AtomicReference<List<City>> selectedCities)
            throws IOException {
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        exchange.getResponseHeaders().set("Cache-Control", "no-store");

        if (!"/api/clocks".equals(exchange.getRequestURI().getPath())) {
            sendJson(exchange, 404, "{\"error\":\"Not found\"}");
            return;
        }

        if (!"GET".equals(exchange.getRequestMethod())) {
            exchange.getResponseHeaders().set("Allow", "GET");
            sendJson(exchange, 405, "{\"error\":\"Method not allowed\"}");
            return;
        }

        StringBuilder json = new StringBuilder();
        json.append("{\"generatedAt\":")
                .append(quote(Instant.now().toString()))
                .append(",\"clocks\":[");

        List<City> cities = selectedCities.get();
        for (int i = 0; i < cities.size(); i++) {
            if (i > 0) {
                json.append(',');
            }

            City city = cities.get(i);
            String currentTime = ZonedDateTime.now(ZoneId.of(city.timeZone))
                    .format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
            json.append("{\"city\":")
                    .append(quote(city.name))
                    .append(",\"country\":")
                    .append(quote(city.country))
                    .append(",\"timeZone\":")
                    .append(quote(city.timeZone))
                    .append(",\"currentTime\":")
                    .append(quote(currentTime))
                    .append('}');
        }
        json.append("]}");
        sendJson(exchange, 200, json.toString());
    }

    private static String quote(String value) {
        StringBuilder escaped = new StringBuilder("\"");
        for (char character : value.toCharArray()) {
            switch (character) {
                case '"':
                    escaped.append("\\\"");
                    break;
                case '\\':
                    escaped.append("\\\\");
                    break;
                case '\b':
                    escaped.append("\\b");
                    break;
                case '\f':
                    escaped.append("\\f");
                    break;
                case '\n':
                    escaped.append("\\n");
                    break;
                case '\r':
                    escaped.append("\\r");
                    break;
                case '\t':
                    escaped.append("\\t");
                    break;
                default:
                    if (character < 0x20) {
                        escaped.append(String.format("\\u%04x", (int) character));
                    } else {
                        escaped.append(character);
                    }
            }
        }
        return escaped.append('"').toString();
    }

    private static void sendJson(HttpExchange exchange, int status, String body) throws IOException {
        byte[] response = body.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(status, response.length);
        try (var output = exchange.getResponseBody()) {
            output.write(response);
        }
    }

    private static final class City {
        private final String name;
        private final String country;
        private final String timeZone;

        private City(String name, String country, String timeZone) {
            this.name = name;
            this.country = country;
            this.timeZone = timeZone;
        }
    }
}
