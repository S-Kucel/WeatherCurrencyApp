package pl.weathercurrency;

import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.time.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

final class FixtureServer implements AutoCloseable {
    static final String RATES = """
        [{"table":"A","no":"213/A/NBP/2026","effectiveDate":"2026-11-02","rates":[
          {"currency":"euro","code":"EUR","mid":4.30},
          {"currency":"dolar amerykański","code":"USD","mid":4.00},
          {"currency":"jen (Japonia)","code":"JPY","mid":0.026}] }]
        """;
    static final String CITIES = """
        {"results":[{"name":"Łódź","latitude":51.75,"longitude":19.46,"country":"Polska"}]}
        """;
    final AtomicReference<String> rates = new AtomicReference<>(RATES);
    final AtomicReference<String> cities = new AtomicReference<>(CITIES);
    final AtomicReference<String> forecast = new AtomicReference<>(weather());
    final AtomicReference<String> lastGeoQuery = new AtomicReference<>("");
    final AtomicInteger rateStatus = new AtomicInteger(200);
    private final HttpServer server;
    private final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
    FixtureServer() throws IOException {
        server = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
        server.setExecutor(executor);
        server.createContext("/geo", exchange -> {
            lastGeoQuery.set(exchange.getRequestURI().getRawQuery());
            respond(exchange, 200, cities.get());
        });
        server.createContext("/forecast", exchange -> respond(exchange, 200, forecast.get()));
        server.createContext("/rates", exchange -> respond(exchange, rateStatus.get(), rates.get()));
        server.createContext("/slow", exchange -> {
            try { Thread.sleep(500); respond(exchange, 200, "{}"); }
            catch (InterruptedException ex) { Thread.currentThread().interrupt(); }
        });
        server.start();
    }
    URI endpoint(String path) { return URI.create("http://localhost:" + server.getAddress().getPort() + path); }
    private static void respond(com.sun.net.httpserver.HttpExchange exchange, int status, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        exchange.sendResponseHeaders(status, bytes.length);
        try (var output = exchange.getResponseBody()) { output.write(bytes); }
    }
    private static String weather() {
        LocalDate first = LocalDate.of(2026, 11, 2);
        String dates = IntStream.range(0, 7).mapToObj(i -> "\"" + first.plusDays(i) + "\"").collect(Collectors.joining(","));
        String times = IntStream.range(0, 48).mapToObj(i -> "\"" + first.atStartOfDay().plusHours(i) + "\"").collect(Collectors.joining(","));
        String temperatures = IntStream.range(0, 48).mapToObj(i -> "" + (15 + 5 * Math.sin(i * .25))).collect(Collectors.joining(","));
        return """
            {"timezone":"Europe/Warsaw","current":{"time":"2026-11-02T12:00","temperature_2m":18,
            "apparent_temperature":17,"relative_humidity_2m":68,"wind_speed_10m":12,"weather_code":2,"is_day":1},
            "daily":{"time":[%s],"weather_code":[2,0,3,61,80,71,95],"temperature_2m_min":[9,10,8,8,7,6,5],
            "temperature_2m_max":[19,20,17,16,15,13,12],"precipitation_probability_max":[10,0,20,70,60,40,90]},
            "hourly":{"time":[%s],"temperature_2m":[%s]}}
            """.formatted(dates, times, temperatures);
    }
    @Override public void close() { server.stop(0); executor.shutdownNow(); }
}
