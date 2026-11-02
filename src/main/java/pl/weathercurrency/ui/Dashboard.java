package pl.weathercurrency.ui;

import java.math.*;
import java.net.ConnectException;
import java.net.http.HttpTimeoutException;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Consumer;
import javafx.application.Platform;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.*;
import javafx.collections.transformation.FilteredList;
import javafx.geometry.*;
import javafx.scene.Node;
import javafx.scene.chart.*;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import pl.weathercurrency.model.*;
import pl.weathercurrency.service.*;

public final class Dashboard extends BorderPane implements AutoCloseable {
    private static final Locale POLISH = Locale.forLanguageTag("pl-PL");
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("d MMMM yyyy", POLISH);
    private final WeatherService weatherService;
    private final CurrencyService currencyService;
    private final ExecutorService worker = Executors.newFixedThreadPool(3, runnable -> {
        Thread thread = new Thread(runnable, "weather-currency-api"); thread.setDaemon(true); return thread;
    });
    private volatile boolean closed;
    private long weatherRequest;
    private boolean updatingRates;
    private final VBox weatherPage = new VBox(18), currencyPage = new VBox(18);
    private final Label weatherStatus = label("Wybierz miasto, aby pobrać prognozę.", "status");
    private final Label currencyStatus = label("Pobierz najnowszą tabelę kursów.", "status");
    private final ProgressIndicator weatherProgress = progress(), currencyProgress = progress();
    private final TextField cityQuery = new TextField("Warszawa");
    private final ComboBox<City> cities = new ComboBox<>();
    private final Button search = button("Szukaj miasta", "primary-button");
    private final Button weatherRefresh = button("Odśwież prognozę", "secondary-button");
    private final Button rateRefresh = button("Odśwież kursy", "secondary-button");
    private final VBox hero = new VBox();
    private final HBox metrics = new HBox(14);
    private final GridPane forecast = new GridPane();
    private final LineChart<String, Number> temperatureChart = new LineChart<>(new CategoryAxis(), new NumberAxis());
    private final TextField amount = new TextField("100");
    private final ComboBox<CurrencyRate> from = new ComboBox<>(), to = new ComboBox<>();
    private final Label result = label("—", "conversion-result"), unitRate = label("Pobierz kursy, aby przeliczyć kwotę.", "result-detail");
    private final Label amountError = label("", "error");
    private final Label rateDate = label("Źródło: Narodowy Bank Polski · tabela A", "muted");
    private final ObservableList<CurrencyRate> allRates = FXCollections.observableArrayList();
    private final FilteredList<CurrencyRate> filteredRates = new FilteredList<>(allRates);
    private final TableView<CurrencyRate> rateTable = new TableView<>(filteredRates);
    private RateTable rates;
    private WeatherReport lastWeather;

    public Dashboard(WeatherService weatherService, CurrencyService currencyService, Consumer<String> openLink) {
        this.weatherService = weatherService; this.currencyService = currencyService;
        weatherStatus.setId("weatherStatus"); currencyStatus.setId("currencyStatus"); amountError.setId("amountError");
        getStyleClass().add("app-root");
        buildWeatherPage(); buildCurrencyPage();
        ToggleButton weatherNav = new ToggleButton("Pogoda"), currencyNav = new ToggleButton("Waluty");
        weatherNav.setId("weatherNav"); currencyNav.setId("currencyNav");
        ToggleGroup navigation = new ToggleGroup();
        for (ToggleButton item : List.of(weatherNav, currencyNav)) {
            item.setToggleGroup(navigation); item.getStyleClass().add("nav-button"); item.setMaxWidth(Double.MAX_VALUE);
        }
        weatherNav.setSelected(true);
        weatherNav.setOnAction(event -> { weatherNav.setSelected(true); showPage(weatherPage); });
        currencyNav.setOnAction(event -> { currencyNav.setSelected(true); showPage(currencyPage); });
        VBox brand = new VBox(12, label("W / C", "brand-mark"), label("WEATHER\n& CURRENCY", "brand"), label("Twój codzienny przegląd", "sidebar-muted"));
        Region spacer = new Region(); VBox.setVgrow(spacer, Priority.ALWAYS);
        Hyperlink weatherLink = new Hyperlink("Open-Meteo ↗"), nbpLink = new Hyperlink("Narodowy Bank Polski ↗");
        weatherLink.setOnAction(event -> openLink.accept("https://open-meteo.com/"));
        nbpLink.setOnAction(event -> openLink.accept("https://api.nbp.pl/"));
        VBox sources = new VBox(7, label("ŹRÓDŁA DANYCH", "sidebar-caption"), weatherLink, nbpLink);
        VBox sidebar = new VBox(12, brand, new Separator(), weatherNav, currencyNav, spacer, sources);
        sidebar.setPadding(new Insets(30, 22, 26, 22)); sidebar.setPrefWidth(218); sidebar.setMinWidth(218); sidebar.getStyleClass().add("sidebar");
        setLeft(sidebar); showPage(weatherPage);
    }
    public void loadInitial() { loadWeather(City.warsaw()); loadRates(); }
    private void showPage(VBox page) {
        ScrollPane scroll = new ScrollPane(page); scroll.setFitToWidth(true); scroll.getStyleClass().add("page-scroll");
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER); setCenter(scroll);
    }
    private void buildWeatherPage() {
        weatherPage.setPadding(new Insets(30));
        weatherPage.getChildren().add(header("POGODA NA DZIŚ", "Dobry dzień zaczyna się od prognozy.", "Sprawdź warunki w mieście i zaplanuj kolejne dni.", weatherRefresh));
        cityQuery.setId("cityQuery"); cityQuery.setPromptText("Wpisz miasto, np. Kraków lub Paris, France");
        search.setId("searchCity"); cities.setId("cityResults"); cities.setMaxWidth(Double.MAX_VALUE);
        cities.getItems().add(City.warsaw()); cities.getSelectionModel().selectFirst();
        HBox queryRow = new HBox(10, cityQuery, search); HBox.setHgrow(cityQuery, Priority.ALWAYS);
        VBox searchCard = card(label("GDZIE SPRAWDZAMY POGODĘ?", "eyebrow"), queryRow, cities);
        search.setOnAction(event -> searchCities()); cityQuery.setOnAction(event -> searchCities());
        cities.valueProperty().addListener((observable, old, selected) -> { if (selected != null) loadWeather(selected); });
        weatherRefresh.setOnAction(event -> { if (cities.getValue() != null) loadWeather(cities.getValue()); });
        HBox status = new HBox(9, weatherProgress, weatherStatus); status.setAlignment(Pos.CENTER_LEFT);
        hero.getStyleClass().add("weather-hero"); hero.setPadding(new Insets(24, 30, 24, 30)); hero.setMinHeight(185);
        hero.getChildren().addAll(label("Twoja prognoza", "hero-location"), label("— °C", "hero-temperature"), label("Pogoda pojawi się po pobraniu danych.", "hero-detail"));
        for (String metric : List.of("ODCZUWALNA", "WILGOTNOŚĆ", "WIATR")) addMetric(metric, "—");
        forecast.setHgap(10);
        for (int i = 0; i < 7; i++) { ColumnConstraints column = new ColumnConstraints(); column.setPercentWidth(100.0 / 7); forecast.getColumnConstraints().add(column); }
        forecast.add(label("Pobierz prognozę, aby zobaczyć kolejne dni.", "muted"), 0, 0, 7, 1);
        temperatureChart.setAnimated(false); temperatureChart.setLegendVisible(false); temperatureChart.setCreateSymbols(false);
        temperatureChart.setMinHeight(190); temperatureChart.setPrefHeight(190); temperatureChart.setMaxHeight(190);
        ((NumberAxis)temperatureChart.getYAxis()).setForceZeroInRange(false);
        temperatureChart.getYAxis().setLabel("°C"); temperatureChart.getXAxis().setTickLabelRotation(-35);
        temperatureChart.setId("temperatureChart");
        VBox chartCard = card(label("Temperatura w kolejnych 24 godzinach", "section-title"), temperatureChart);
        weatherPage.getChildren().addAll(searchCard, status, hero, metrics, label("Prognoza na 7 dni", "section-title"), forecast, chartCard,
                label("Dane pogodowe: Open-Meteo (CC BY 4.0) · Lokalizacje: GeoNames · Czas lokalny wybranego miasta", "source-note"));
    }
    private void buildCurrencyPage() {
        currencyPage.setPadding(new Insets(30));
        currencyPage.getChildren().add(header("KURSY WALUT", "Przelicz bez zgadywania.", "Kursy średnie z ostatniej opublikowanej tabeli NBP.", rateRefresh));
        rateRefresh.setId("refreshRates"); rateRefresh.setOnAction(event -> loadRates());
        HBox status = new HBox(9, currencyProgress, currencyStatus); status.setAlignment(Pos.CENTER_LEFT);
        amount.setId("amount"); from.setId("fromCurrency"); to.setId("toCurrency"); result.setId("conversionResult");
        amount.setPromptText("np. 100,50"); from.setMaxWidth(Double.MAX_VALUE); to.setMaxWidth(Double.MAX_VALUE);
        amountError.setWrapText(true); amountError.setVisible(false); amountError.managedProperty().bind(amountError.visibleProperty());
        Button swap = button("⇄", "swap-button"); swap.setId("swapCurrencies"); swap.setAccessibleText("Zamień waluty");
        swap.setOnAction(event -> { CurrencyRate previous = from.getValue(); from.setValue(to.getValue()); to.setValue(previous); });
        VBox fromField = new VBox(7, label("Z waluty", "field-label"), from), toField = new VBox(7, label("Na walutę", "field-label"), to);
        HBox pair = new HBox(12, fromField, swap, toField); pair.setAlignment(Pos.BOTTOM_CENTER);
        HBox.setHgrow(fromField, Priority.ALWAYS); HBox.setHgrow(toField, Priority.ALWAYS); fromField.setMinWidth(0); toField.setMinWidth(0);
        VBox resultCard = new VBox(8, label("OTRZYMASZ", "result-caption"), result, unitRate);
        resultCard.getStyleClass().add("result-card"); resultCard.setPadding(new Insets(22));
        VBox converter = card(label("Przelicznik walut", "section-title"), label("Kwota", "field-label"), amount, pair, amountError, resultCard, rateDate);
        amount.textProperty().addListener((observable, old, value) -> updateConversion());
        from.valueProperty().addListener((observable, old, value) -> updateConversion());
        to.valueProperty().addListener((observable, old, value) -> updateConversion());
        TextField filter = new TextField(); filter.setId("rateFilter"); filter.setPromptText("Filtruj: kod lub nazwa waluty");
        filter.textProperty().addListener((observable, old, value) -> filteredRates.setPredicate(rate ->
                (rate.code() + " " + rate.name()).toLowerCase(POLISH).contains(value.strip().toLowerCase(POLISH))));
        TableColumn<CurrencyRate, String> codeColumn = new TableColumn<>("Waluta"), nameColumn = new TableColumn<>("Nazwa"), valueColumn = new TableColumn<>("1 jednostka w PLN");
        codeColumn.setCellValueFactory(value -> new ReadOnlyStringWrapper(value.getValue().code()));
        nameColumn.setCellValueFactory(value -> new ReadOnlyStringWrapper(value.getValue().name()));
        valueColumn.setCellValueFactory(value -> new ReadOnlyStringWrapper(rateFormat(value.getValue().plnRate())));
        codeColumn.setPrefWidth(100); nameColumn.setPrefWidth(360); valueColumn.setPrefWidth(190);
        rateTable.getColumns().add(codeColumn); rateTable.getColumns().add(nameColumn); rateTable.getColumns().add(valueColumn);
        rateTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        rateTable.setMinHeight(290); rateTable.setPrefHeight(340); rateTable.setPlaceholder(label("Brak kursów do wyświetlenia", "muted"));
        rateTable.setId("rateTable"); rateTable.setOnMouseClicked(event -> { if (event.getClickCount() == 2 && rateTable.getSelectionModel().getSelectedItem() != null) from.setValue(rateTable.getSelectionModel().getSelectedItem()); });
        currencyPage.getChildren().addAll(status, converter, card(label("Wszystkie dostępne waluty", "section-title"), filter, rateTable),
                label("Źródło: NBP, tabela A + PLN · Kursy średnie mają charakter informacyjny. Przeliczenie nie uwzględnia opłat.", "source-note"));
    }
    private void searchCities() {
        String query = cityQuery.getText();
        if (query == null || query.strip().length() < 2) { status(weatherStatus, "Wpisz co najmniej 2 znaki nazwy miasta.", true); return; }
        search.setDisable(true); status(weatherStatus, "Szukam lokalizacji…", false);
        submit(() -> weatherService.search(query), locations -> {
            search.setDisable(false);
            if (locations.isEmpty()) { status(weatherStatus, "Nie znaleziono miasta. Spróbuj innej nazwy lub dopisz kraj.", true); return; }
            cities.getItems().setAll(locations); cities.getSelectionModel().selectFirst();
        }, error -> { search.setDisable(false); status(weatherStatus, errorText(error), true); });
    }
    private void loadWeather(City city) {
        long request = ++weatherRequest;
        weatherProgress.setVisible(true); weatherRefresh.setDisable(true);
        status(weatherStatus, "Pobieram pogodę: " + city.name() + "…", false);
        submit(() -> weatherService.weather(city), report -> {
            if (request != weatherRequest) return;
            weatherProgress.setVisible(false); weatherRefresh.setDisable(false); lastWeather = report; renderWeather(report);
            status(weatherStatus, "Zaktualizowano · " + report.observedAt().format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")) + " · " + report.timezone(), false);
        }, error -> {
            if (request != weatherRequest) return;
            weatherProgress.setVisible(false); weatherRefresh.setDisable(false);
            status(weatherStatus, errorText(error) + (lastWeather == null ? "" : " Wyświetlana jest poprzednia prognoza dla " + lastWeather.city().name() + "."), true);
        });
    }
    private void renderWeather(WeatherReport report) {
        hero.getChildren().clear();
        VBox location = new VBox(5, label(report.city().name(), "hero-location"), label(report.city().details(), "hero-detail"),
                label(report.observedAt().format(DATE), "hero-detail"));
        Region space = new Region(); HBox.setHgrow(space, Priority.ALWAYS);
        Label temperature = label(String.format(POLISH, "%.0f°", report.temperature()), "hero-temperature"); temperature.setId("currentTemperature");
        VBox reading = new VBox(0, temperature, label(WeatherCondition.description(report.code()), "hero-detail"));
        HBox row = new HBox(24, location, space, new WeatherIcon(report.code(), report.day(), 120), reading); row.setAlignment(Pos.CENTER_LEFT);
        hero.getChildren().add(row); metrics.getChildren().clear();
        addMetric("ODCZUWALNA", String.format(POLISH, "%.1f °C", report.apparentTemperature()));
        addMetric("WILGOTNOŚĆ", report.humidity() + "%"); addMetric("WIATR", String.format(POLISH, "%.1f km/h", report.windSpeed()));
        forecast.getChildren().clear();
        for (int i = 0; i < report.days().size(); i++) {
            var day = report.days().get(i);
            VBox dayCard = new VBox(7, label(i == 0 ? "Dzisiaj" : day.date().format(DateTimeFormatter.ofPattern("EEE", POLISH)), "day-title"),
                    label(day.date().format(DateTimeFormatter.ofPattern("dd.MM")), "muted"), new WeatherIcon(day.code(), true, 58),
                    label(String.format(POLISH, "%.0f° / %.0f°", day.maximum(), day.minimum()), "day-temperature"), label("Opady " + day.rainProbability() + "%", "day-rain"));
            dayCard.getStyleClass().add("day-card"); dayCard.setAlignment(Pos.CENTER); dayCard.setMinWidth(0);
            Tooltip.install(dayCard, new Tooltip(WeatherCondition.description(day.code()))); forecast.add(dayCard, i, 0);
        }
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        for (var hour : report.hours()) series.getData().add(new XYChart.Data<>(hour.time().format(DateTimeFormatter.ofPattern("HH:mm")), hour.temperature()));
        temperatureChart.getData().clear(); temperatureChart.getData().add(series);
    }
    private void loadRates() {
        currencyProgress.setVisible(true); rateRefresh.setDisable(true); status(currencyStatus, "Pobieram najnowszą tabelę NBP…", false);
        submit(currencyService::latest, table -> {
            String fromCode = from.getValue() == null ? "EUR" : from.getValue().code(), toCode = to.getValue() == null ? "PLN" : to.getValue().code();
            rates = table; updatingRates = true; allRates.setAll(table.rates()); from.getItems().setAll(table.rates()); to.getItems().setAll(table.rates());
            from.setValue(table.rates().stream().filter(rate -> rate.code().equals(fromCode)).findFirst().orElse(table.rates().getFirst()));
            to.setValue(table.currency(table.rates().stream().anyMatch(rate -> rate.code().equals(toCode)) ? toCode : "PLN"));
            updatingRates = false; updateConversion(); currencyProgress.setVisible(false); rateRefresh.setDisable(false);
            rateDate.setText("NBP · tabela " + table.number() + " · data publikacji: " + table.date().format(DATE));
            status(currencyStatus, "Wczytano " + table.rates().size() + " walut · ostatnia opublikowana tabela: " + table.date().format(DATE), false);
        }, error -> { currencyProgress.setVisible(false); rateRefresh.setDisable(false); status(currencyStatus, errorText(error) + (rates == null ? "" : " Pozostawiono tabelę z " + rates.date() + "."), true); });
    }
    private void updateConversion() {
        if (updatingRates || rates == null || from.getValue() == null || to.getValue() == null) return;
        try {
            BigDecimal converted = CurrencyConverter.convert(CurrencyConverter.parseAmount(amount.getText()), from.getValue().code(), to.getValue().code(), rates);
            DecimalFormat money = new DecimalFormat("#,##0.00", DecimalFormatSymbols.getInstance(POLISH)); money.setRoundingMode(RoundingMode.HALF_UP);
            result.setText(money.format(converted) + " " + to.getValue().code());
            unitRate.setText("1 " + from.getValue().code() + " = " + rateFormat(CurrencyConverter.convert(BigDecimal.ONE, from.getValue().code(), to.getValue().code(), rates)) + " " + to.getValue().code());
            amountError.setVisible(false);
        } catch (IllegalArgumentException ex) { result.setText("—"); amountError.setText(ex.getMessage()); amountError.setVisible(true); }
    }
    private <T> void submit(Callable<T> job, Consumer<T> success, Consumer<Exception> failure) {
        worker.submit(() -> {
            try { T value = job.call(); if (!closed) Platform.runLater(() -> { if (!closed) success.accept(value); }); }
            catch (Exception error) { if (error instanceof InterruptedException) Thread.currentThread().interrupt(); if (!closed) Platform.runLater(() -> { if (!closed) failure.accept(error); }); }
        });
    }
    private static String errorText(Exception error) {
        if (error instanceof HttpTimeoutException) return "Serwis odpowiada zbyt długo. Spróbuj ponownie.";
        if (error instanceof ConnectException || error instanceof java.net.UnknownHostException) return "Brak połączenia z serwisem. Sprawdź internet i spróbuj ponownie.";
        return error.getMessage() == null ? "Nie udało się pobrać danych. Spróbuj ponownie." : error.getMessage();
    }
    private void addMetric(String title, String value) {
        VBox item = card(label(title, "eyebrow"), label(value, "metric-value")); item.setMinWidth(0); HBox.setHgrow(item, Priority.ALWAYS); metrics.getChildren().add(item);
    }
    private static HBox header(String eyebrow, String title, String subtitle, Button action) {
        VBox text = new VBox(7, label(eyebrow, "eyebrow"), label(title, "page-title"), label(subtitle, "muted"));
        Region space = new Region(); HBox.setHgrow(space, Priority.ALWAYS); HBox row = new HBox(14, text, space, action); row.setAlignment(Pos.CENTER_LEFT); return row;
    }
    private static VBox card(Node... children) { VBox box = new VBox(12, children); box.getStyleClass().add("card"); box.setPadding(new Insets(20)); return box; }
    private static Label label(String text, String style) { Label label = new Label(text); label.getStyleClass().add(style); return label; }
    private static Button button(String text, String style) { Button button = new Button(text); button.getStyleClass().add(style); return button; }
    private static ProgressIndicator progress() { ProgressIndicator indicator = new ProgressIndicator(); indicator.setMaxSize(18, 18); indicator.setPrefSize(18, 18); indicator.setVisible(false); indicator.managedProperty().bind(indicator.visibleProperty()); return indicator; }
    private static void status(Label label, String message, boolean error) { label.setText(message); label.setWrapText(true); label.getStyleClass().remove("error"); if (error) label.getStyleClass().add("error"); }
    private static String rateFormat(BigDecimal value) { return new DecimalFormat("0.0000####", DecimalFormatSymbols.getInstance(POLISH)).format(value); }
    @Override public void close() { closed = true; worker.shutdownNow(); }
}
