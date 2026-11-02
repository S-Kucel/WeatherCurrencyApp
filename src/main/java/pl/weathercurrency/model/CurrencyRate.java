package pl.weathercurrency.model;

import java.math.BigDecimal;

public record CurrencyRate(String code, String name, BigDecimal plnRate) {
    public CurrencyRate {
        if (code == null || !code.matches("[A-Z]{3}") || name == null || plnRate == null || plnRate.signum() <= 0) {
            throw new IllegalArgumentException("Nieprawidłowy kurs waluty.");
        }
    }
    @Override public String toString() { return code + " · " + name; }
}
