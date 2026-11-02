package pl.weathercurrency.service;

import com.google.gson.*;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.URI;
import java.time.LocalDate;
import java.util.*;
import pl.weathercurrency.model.*;

public final class CurrencyService {
    private final JsonHttpClient http;
    private final URI endpoint;
    public CurrencyService(JsonHttpClient http) { this(http, URI.create("https://api.nbp.pl/api/exchangerates/tables/a/?format=json")); }
    public CurrencyService(JsonHttpClient http, URI endpoint) { this.http = http; this.endpoint = endpoint; }
    public RateTable latest() throws IOException, InterruptedException {
        JsonElement response = http.get(endpoint);
        try {
            JsonObject table = response.getAsJsonArray().get(0).getAsJsonObject();
            List<CurrencyRate> rates = new ArrayList<>();
            rates.add(new CurrencyRate("PLN", "złoty polski", BigDecimal.ONE));
            Set<String> codes = new HashSet<>(Set.of("PLN"));
            for (JsonElement entry : table.getAsJsonArray("rates")) {
                JsonObject rate = entry.getAsJsonObject();
                String code = rate.get("code").getAsString();
                if (!codes.add(code)) throw new IllegalArgumentException("Powtórzona waluta");
                rates.add(new CurrencyRate(code, rate.get("currency").getAsString(), rate.get("mid").getAsBigDecimal()));
            }
            if (rates.size() == 1) throw new IllegalArgumentException("Pusta tabela");
            rates.sort(Comparator.comparing(CurrencyRate::code));
            return new RateTable(LocalDate.parse(table.get("effectiveDate").getAsString()), table.get("no").getAsString(), rates);
        } catch (RuntimeException ex) { throw new IOException("Nie udało się odczytać tabeli kursów NBP.", ex); }
    }
}
