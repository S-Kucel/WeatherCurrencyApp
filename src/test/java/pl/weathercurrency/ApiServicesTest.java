package pl.weathercurrency;

import org.junit.jupiter.api.*;
import java.io.IOException;
import java.net.http.*;
import java.time.*;
import pl.weathercurrency.model.City;
import pl.weathercurrency.service.*;
import static org.junit.jupiter.api.Assertions.*;

class ApiServicesTest {
    private FixtureServer server;
    private JsonHttpClient http;
    private WeatherService weather;
    private CurrencyService currencies;
    @BeforeEach void setup() throws Exception {
        server = new FixtureServer(); http = new JsonHttpClient();
        weather = new WeatherService(http, server.endpoint("/geo"), server.endpoint("/forecast"));
        currencies = new CurrencyService(http, server.endpoint("/rates"));
    }
    @AfterEach void cleanup() { server.close(); }
    @Test void searchesUnicodeAndAllowsMissingRegion() throws Exception {
        var city = weather.search("Łódź").getFirst();
        assertEquals("Łódź", city.name()); assertEquals("", city.region());
        assertTrue(server.lastGeoQuery.get().contains("%C5%81%C3%B3d%C5%BA"));
    }
    @Test void handlesNoMatchingCityAndRejectsShortQuery() throws Exception {
        server.cities.set("{}"); assertTrue(weather.search("Unknown").isEmpty());
        assertThrows(IllegalArgumentException.class, () -> weather.search("a"));
    }
    @Test void parsesForecastAndSelectsUpcoming24HoursInCityTimezone() throws Exception {
        var report = weather.weather(City.warsaw());
        assertEquals(18, report.temperature()); assertEquals(7, report.days().size()); assertEquals(24, report.hours().size());
        assertEquals(ZoneId.of("Europe/Warsaw"), report.timezone());
        assertEquals(LocalDateTime.of(2026, 11, 2, 12, 0), report.hours().getFirst().time());
    }
    @Test void rejectsNullWeatherMeasurements() {
        server.forecast.set(server.forecast.get().replace("\"temperature_2m\":18", "\"temperature_2m\":null"));
        assertThrows(IOException.class, () -> weather.weather(City.warsaw()));
    }
    @Test void preservesNbpPublicationDateAndAddsPln() throws Exception {
        var table = currencies.latest();
        assertEquals(LocalDate.of(2026, 11, 2), table.date()); assertEquals("213/A/NBP/2026", table.number());
        assertEquals(4, table.rates().size()); assertEquals("1", table.currency("PLN").plnRate().toPlainString());
        assertEquals("0.026", table.currency("JPY").plnRate().toPlainString());
    }
    @Test void rejectsInvalidRates() {
        server.rates.set(FixtureServer.RATES.replace("4.30", "-4.30"));
        assertThrows(IOException.class, currencies::latest);
    }
    @Test void explainsRateLimits() {
        server.rateStatus.set(429);
        assertTrue(assertThrows(IOException.class, currencies::latest).getMessage().contains("limit"));
    }
    @Test void rejectsMalformedJson() {
        server.rates.set("{not valid JSON"); assertThrows(IOException.class, currencies::latest);
    }
    @Test void networkRequestsHaveTimeouts() {
        JsonHttpClient impatient = new JsonHttpClient(HttpClient.newHttpClient(), Duration.ofMillis(50));
        assertThrows(HttpTimeoutException.class, () -> impatient.get(server.endpoint("/slow")));
    }
}
