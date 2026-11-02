package pl.weathercurrency.model;

import java.time.*;
import java.util.List;

public record WeatherReport(City city, ZoneId timezone, LocalDateTime observedAt,
        double temperature, double apparentTemperature, int humidity, double windSpeed,
        int code, boolean day, List<Day> days, List<Hour> hours) {
    public WeatherReport { days = List.copyOf(days); hours = List.copyOf(hours); }
    public record Day(LocalDate date, int code, double minimum, double maximum, int rainProbability) {}
    public record Hour(LocalDateTime time, double temperature) {}
}
