package pl.weathercurrency.model;

import java.util.stream.Collectors;
import java.util.stream.Stream;

public record City(String name, String region, String country, double latitude, double longitude) {
    public static City warsaw() { return new City("Warszawa", "Mazowieckie", "Polska", 52.22977, 21.01178); }
    public String details() {
        return Stream.of(region, country).filter(s -> s != null && !s.isBlank()).collect(Collectors.joining(", "));
    }
    @Override public String toString() { return name + (details().isEmpty() ? "" : " · " + details()); }
}
