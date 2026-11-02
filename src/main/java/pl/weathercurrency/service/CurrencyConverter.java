package pl.weathercurrency.service;

import java.math.*;
import pl.weathercurrency.model.RateTable;

public final class CurrencyConverter {
    private CurrencyConverter() {}
    public static BigDecimal parseAmount(String input) {
        String value = input == null ? "" : input.strip().replace(" ", "").replace("\u00a0", "");
        if (!value.matches("\\d{1,12}([.,]\\d{1,6})?")) {
            throw new IllegalArgumentException("Podaj kwotę od 0, maks. 12 cyfr i 6 miejsc po przecinku.");
        }
        return new BigDecimal(value.replace(',', '.'));
    }
    public static BigDecimal convert(BigDecimal amount, String from, String to, RateTable table) {
        if (amount == null || amount.signum() < 0) throw new IllegalArgumentException("Kwota nie może być ujemna.");
        return amount.multiply(table.currency(from).plnRate(), MathContext.DECIMAL128)
                .divide(table.currency(to).plnRate(), MathContext.DECIMAL128);
    }
}
