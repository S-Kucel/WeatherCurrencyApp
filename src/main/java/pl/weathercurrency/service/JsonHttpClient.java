package pl.weathercurrency.service;

import com.google.gson.*;
import java.io.IOException;
import java.net.URI;
import java.net.http.*;
import java.time.Duration;

public final class JsonHttpClient {
    private final HttpClient client;
    private final Duration timeout;
    public JsonHttpClient() {
        this(HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(8)).followRedirects(HttpClient.Redirect.NORMAL).build(), Duration.ofSeconds(18));
    }
    public JsonHttpClient(HttpClient client, Duration timeout) { this.client = client; this.timeout = timeout; }
    public JsonElement get(URI uri) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder(uri).timeout(timeout)
                .header("Accept", "application/json").header("User-Agent", "WeatherCurrencyApp/1.0").GET().build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString(java.nio.charset.StandardCharsets.UTF_8));
        if (response.statusCode() != 200) {
            String message = switch (response.statusCode()) {
                case 429 -> "Przekroczono limit zapytań. Spróbuj ponownie za chwilę.";
                case 404 -> "Nie znaleziono danych w serwisie.";
                default -> "Serwis jest niedostępny (HTTP " + response.statusCode() + "). Spróbuj ponownie.";
            };
            throw new IOException(message);
        }
        try {
            JsonElement result = JsonParser.parseString(response.body());
            if (result.isJsonNull()) throw new JsonParseException("Pusta odpowiedź");
            return result;
        } catch (JsonParseException ex) {
            throw new IOException("Serwis zwrócił nieprawidłowe dane JSON.", ex);
        }
    }
}
