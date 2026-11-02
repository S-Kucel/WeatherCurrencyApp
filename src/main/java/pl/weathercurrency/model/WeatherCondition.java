package pl.weathercurrency.model;

public final class WeatherCondition {
    private WeatherCondition() {}
    public static String description(int code) {
        return switch (code) {
            case 0 -> "Bezchmurnie";
            case 1 -> "Przeważnie słonecznie";
            case 2 -> "Częściowe zachmurzenie";
            case 3 -> "Pochmurno";
            case 45, 48 -> "Mgła";
            case 51, 53, 55 -> "Mżawka";
            case 56, 57 -> "Marznąca mżawka";
            case 61, 63, 65 -> "Deszcz";
            case 66, 67 -> "Marznący deszcz";
            case 71, 73, 75, 77 -> "Śnieg";
            case 80, 81, 82 -> "Przelotny deszcz";
            case 85, 86 -> "Przelotny śnieg";
            case 95, 96, 99 -> "Burza";
            default -> "Brak opisu pogody";
        };
    }
}
