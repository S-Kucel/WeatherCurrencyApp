package pl.weathercurrency;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import pl.weathercurrency.model.*;
import pl.weathercurrency.service.CurrencyConverter;
import static org.junit.jupiter.api.Assertions.*;

class CurrencyConverterTest {
    private final RateTable table = new RateTable(LocalDate.of(2026, 11, 2), "test", List.of(
            new CurrencyRate("PLN", "złoty", BigDecimal.ONE), new CurrencyRate("EUR", "euro", new BigDecimal("4.30")),
            new CurrencyRate("USD", "dolar", new BigDecimal("4.00")), new CurrencyRate("JPY", "jen", new BigDecimal("0.026"))));
    @ParameterizedTest @CsvSource({"100,100", "'100,50',100.50", "1 000.50,1000.50", "0,0", "'1 000,25',1000.25"})
    void parsesPolishAndEnglishAmounts(String input, String expected) { assertEquals(new BigDecimal(expected), CurrencyConverter.parseAmount(input)); }
    @ParameterizedTest @ValueSource(strings = {"", "-1", "1e5", "1,2.3", "abc", "1000000000000", "1.1234567"})
    void rejectsInvalidAmounts(String input) { assertThrows(IllegalArgumentException.class, () -> CurrencyConverter.parseAmount(input)); }
    @Test void convertsForeignCurrencyThroughPln() {
        assertEquals(0, new BigDecimal("107.5").compareTo(CurrencyConverter.convert(new BigDecimal("100"), "EUR", "USD", table)));
    }
    @Test void handlesSmallPerUnitRatesWithoutHundredUnitMultiplier() {
        assertEquals(0, new BigDecimal("2.6").compareTo(CurrencyConverter.convert(new BigDecimal("100"), "JPY", "PLN", table)));
    }
    @Test void preservesAmountWhenCurrencyIsTheSame() {
        assertEquals(0, new BigDecimal("123.456789").compareTo(CurrencyConverter.convert(new BigDecimal("123.456789"), "EUR", "EUR", table)));
    }
    @Test void rejectsUnknownCurrencyAndNegativeAmount() {
        assertThrows(IllegalArgumentException.class, () -> CurrencyConverter.convert(BigDecimal.ONE, "XXX", "PLN", table));
        assertThrows(IllegalArgumentException.class, () -> CurrencyConverter.convert(BigDecimal.ONE.negate(), "USD", "PLN", table));
    }
}
