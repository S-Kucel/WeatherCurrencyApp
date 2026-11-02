package pl.weathercurrency.service;

import com.google.gson.*;
import java.io.IOException;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.time.*;
import java.util.*;
import pl.weathercurrency.model.*;

public final class WeatherService {
    private final JsonHttpClient http;
    private final URI geocoding;
    private final URI forecast;
    public WeatherService(JsonHttpClient http) {
        this(http, URI.create("https://geocoding-api.open-meteo.com/v1/search"), URI.create("https://api.open-meteo.com/v1/forecast"));
    }
    public WeatherService(JsonHttpClient http, URI geocoding, URI forecast) {
        this.http = http; this.geocoding = geocoding; this.forecast = forecast;
    }
    public List<City> search(String query) throws IOException, InterruptedException {
        if (query == null || query.strip().length() < 2) throw new IllegalArgumentException("Wpisz co najmniej 2 znaki nazwy miasta.");
        JsonElement response = http.get(URI.create(geocoding + "?name=" + URLEncoder.encode(query.strip(), StandardCharsets.UTF_8) + "&count=10&language=pl&format=json"));
        try {
            JsonObject object = response.getAsJsonObject();
            if (!object.has("results")) return List.of();
            List<City> cities = new ArrayList<>();
            for (JsonElement entry : object.getAsJsonArray("results")) {
                JsonObject city = entry.getAsJsonObject();
                cities.add(new City(city.get("name").getAsString(), optional(city, "admin1"), optional(city, "country"),
                        number(city.get("latitude")), number(city.get("longitude"))));
            }
            return List.copyOf(cities);
        } catch (RuntimeException ex) { throw new IOException("Nie udało się odczytać listy miast.", ex); }
    }
    public WeatherReport weather(City city) throws IOException, InterruptedException {
        URI uri = URI.create(forecast + "?latitude=" + city.latitude() + "&longitude=" + city.longitude()
                + "&current=temperature_2m,relative_humidity_2m,apparent_temperature,is_day,weather_code,wind_speed_10m"
                + "&daily=weather_code,temperature_2m_max,temperature_2m_min,precipitation_probability_max"
                + "&hourly=temperature_2m&timezone=auto&forecast_days=7&wind_speed_unit=kmh");
        JsonElement response = http.get(uri);
        try {
            JsonObject root = response.getAsJsonObject(), current = root.getAsJsonObject("current"), daily = root.getAsJsonObject("daily"), hourly = root.getAsJsonObject("hourly");
            ZoneId timezone = ZoneId.of(root.get("timezone").getAsString());
            LocalDateTime observed = LocalDateTime.parse(current.get("time").getAsString());
            List<WeatherReport.Day> days = new ArrayList<>();
            for (int i = 0; i < daily.getAsJsonArray("time").size(); i++) {
                days.add(new WeatherReport.Day(LocalDate.parse(daily.getAsJsonArray("time").get(i).getAsString()),
                        (int)at(daily, "weather_code", i), at(daily, "temperature_2m_min", i), at(daily, "temperature_2m_max", i),
                        (int)at(daily, "precipitation_probability_max", i)));
            }
            List<WeatherReport.Hour> hours = new ArrayList<>();
            for (int i = 0; i < hourly.getAsJsonArray("time").size(); i++) {
                LocalDateTime time = LocalDateTime.parse(hourly.getAsJsonArray("time").get(i).getAsString());
                if (!time.isBefore(observed.withMinute(0).withSecond(0)) && hours.size() < 24) {
                    hours.add(new WeatherReport.Hour(time, at(hourly, "temperature_2m", i)));
                }
            }
            if (days.isEmpty() || hours.isEmpty()) throw new IllegalStateException("Brak prognozy");
            return new WeatherReport(city, timezone, observed, number(current.get("temperature_2m")), number(current.get("apparent_temperature")),
                    (int)number(current.get("relative_humidity_2m")), number(current.get("wind_speed_10m")),
                    (int)number(current.get("weather_code")), number(current.get("is_day")) == 1, days, hours);
        } catch (RuntimeException ex) { throw new IOException("Dane pogodowe są niekompletne lub mają nieprawidłowy format.", ex); }
    }
    private static String optional(JsonObject object, String key) { return object.has(key) && !object.get(key).isJsonNull() ? object.get(key).getAsString() : ""; }
    private static double number(JsonElement value) {
        double result = value.getAsDouble();
        if (!Double.isFinite(result)) throw new IllegalArgumentException("Nieprawidłowa liczba");
        return result;
    }
    private static double at(JsonObject object, String key, int index) { return number(object.getAsJsonArray(key).get(index)); }
}
