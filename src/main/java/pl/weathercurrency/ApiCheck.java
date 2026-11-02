package pl.weathercurrency;

import pl.weathercurrency.service.*;

/** Optional live check; the regular test suite works without internet access. */
public final class ApiCheck {
    public static void main(String[] args) throws Exception {
        JsonHttpClient http = new JsonHttpClient();
        WeatherService weather = new WeatherService(http);
        var cities = weather.search("Warszawa");
        if (cities.isEmpty()) throw new IllegalStateException("API nie zwróciło miasta.");
        var report = weather.weather(cities.get(0));
        var rates = new CurrencyService(http).latest();
        if (report.days().size() != 7 || rates.rates().size() < 5) throw new IllegalStateException("Niekompletne dane API.");
        System.out.println("OK Open-Meteo: " + report.city().name() + ", " + report.temperature() + " °C, prognoza " + report.days().size() + " dni");
        System.out.println("OK NBP: " + rates.number() + ", " + rates.date() + ", " + rates.rates().size() + " walut, EUR/PLN=" + rates.currency("EUR").plnRate());
    }
}
