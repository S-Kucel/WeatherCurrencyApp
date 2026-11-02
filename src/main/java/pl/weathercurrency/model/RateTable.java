package pl.weathercurrency.model;

import java.time.LocalDate;
import java.util.List;

public record RateTable(LocalDate date, String number, List<CurrencyRate> rates) {
    public RateTable { rates = List.copyOf(rates); }
    public CurrencyRate currency(String code) {
        return rates.stream().filter(rate -> rate.code().equals(code)).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Nieznana waluta: " + code));
    }
}
