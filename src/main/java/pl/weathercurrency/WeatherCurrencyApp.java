package pl.weathercurrency;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;
import pl.weathercurrency.service.*;
import pl.weathercurrency.ui.Dashboard;

public final class WeatherCurrencyApp extends Application {
    private Dashboard dashboard;
    @Override public void start(Stage stage) {
        JsonHttpClient http = new JsonHttpClient();
        dashboard = new Dashboard(new WeatherService(http), new CurrencyService(http), getHostServices()::showDocument);
        Scene scene = new Scene(dashboard, 1280, 920);
        scene.getStylesheets().add(getClass().getResource("/styles/app.css").toExternalForm());
        stage.setTitle("Weather & Currency · Pogoda i waluty");
        stage.setMinWidth(1100);
        stage.setMinHeight(800);
        stage.setScene(scene);
        stage.show();
        dashboard.loadInitial();
    }
    @Override public void stop() { if (dashboard != null) dashboard.close(); }
}
