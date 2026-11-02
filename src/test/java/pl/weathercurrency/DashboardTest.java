package pl.weathercurrency;

import org.junit.jupiter.api.*;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.WritableImage;
import java.awt.image.BufferedImage;
import java.nio.file.*;
import java.util.concurrent.*;
import java.util.function.BooleanSupplier;
import javax.imageio.ImageIO;
import pl.weathercurrency.service.*;
import pl.weathercurrency.ui.Dashboard;
import static org.junit.jupiter.api.Assertions.*;

class DashboardTest {
    private FixtureServer server;
    private Dashboard dashboard;
    @BeforeAll static void startToolkit() throws Exception {
        CountDownLatch ready = new CountDownLatch(1);
        Platform.startup(() -> { Platform.setImplicitExit(false); ready.countDown(); });
        assertTrue(ready.await(15, TimeUnit.SECONDS));
    }
    @AfterAll static void stopToolkit() { Platform.exit(); }
    @BeforeEach void setup() throws Exception {
        server = new FixtureServer();
        fx(() -> {
            JsonHttpClient http = new JsonHttpClient();
            dashboard = new Dashboard(new WeatherService(http, server.endpoint("/geo"), server.endpoint("/forecast")),
                    new CurrencyService(http, server.endpoint("/rates")), link -> {});
            Scene scene = new Scene(dashboard, 1280, 1080);
            scene.getStylesheets().add(getClass().getResource("/styles/app.css").toExternalForm());
            dashboard.resize(1280, 1080); dashboard.applyCss(); dashboard.layout(); dashboard.loadInitial(); return null;
        });
    }
    @AfterEach void cleanup() throws Exception { fx(() -> { dashboard.close(); return null; }); server.close(); }
    @Test void rendersWeatherAndSearchesAnotherCity() throws Exception {
        await(() -> dashboard.lookup("#currentTemperature") != null);
        fx(() -> {
            assertEquals("18°", ((Label)dashboard.lookup("#currentTemperature")).getText());
            capture("weather-preview.png");
            ((TextField)dashboard.lookup("#cityQuery")).setText("Łódź"); ((Button)dashboard.lookup("#searchCity")).fire(); return null;
        });
        await(() -> ((ComboBox<?>)dashboard.lookup("#cityResults")).getValue().toString().startsWith("Łódź"));
    }
    @Test void convertsSwapsFiltersAndValidatesInput() throws Exception {
        fx(() -> { ((ToggleButton)dashboard.lookup("#currencyNav")).fire(); dashboard.applyCss(); dashboard.layout(); return null; });
        await(() -> ((Label)dashboard.lookup("#conversionResult")).getText().contains("PLN"));
        fx(() -> {
            ((TextField)dashboard.lookup("#amount")).setText("100,50");
            assertEquals("432,15 PLN", ((Label)dashboard.lookup("#conversionResult")).getText());
            capture("currency-preview.png");
            ((Button)dashboard.lookup("#swapCurrencies")).fire();
            assertEquals("23,37 EUR", ((Label)dashboard.lookup("#conversionResult")).getText());
            ((TextField)dashboard.lookup("#rateFilter")).setText("USD");
            assertEquals(1, ((TableView<?>)dashboard.lookup("#rateTable")).getItems().size());
            ((TextField)dashboard.lookup("#amount")).setText("-20");
            assertTrue(dashboard.lookup("#amountError").isVisible());
            assertEquals("—", ((Label)dashboard.lookup("#conversionResult")).getText()); return null;
        });
    }
    @Test void failedRefreshKeepsLastSuccessfulCurrencyTable() throws Exception {
        fx(() -> { ((ToggleButton)dashboard.lookup("#currencyNav")).fire(); dashboard.applyCss(); dashboard.layout(); return null; });
        await(() -> ((Label)dashboard.lookup("#conversionResult")).getText().contains("PLN"));
        server.rateStatus.set(503);
        fx(() -> { ((Button)dashboard.lookup("#refreshRates")).fire(); return null; });
        await(() -> ((Label)dashboard.lookup("#currencyStatus")).getText().contains("Pozostawiono"));
        fx(() -> { assertEquals("430,00 PLN", ((Label)dashboard.lookup("#conversionResult")).getText()); return null; });
    }
    private void capture(String filename) throws Exception {
        dashboard.applyCss(); dashboard.layout();
        WritableImage image = dashboard.snapshot(null, null);
        BufferedImage output = new BufferedImage((int)image.getWidth(), (int)image.getHeight(), BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < output.getHeight(); y++) for (int x = 0; x < output.getWidth(); x++) output.setRGB(x, y, image.getPixelReader().getArgb(x, y));
        Path target = Path.of("target", filename); Files.createDirectories(target.getParent()); ImageIO.write(output, "png", target.toFile());
    }
    private static <T> T fx(Callable<T> task) throws Exception {
        FutureTask<T> future = new FutureTask<>(task); Platform.runLater(future); return future.get(15, TimeUnit.SECONDS);
    }
    private static void await(BooleanSupplier condition) throws Exception {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
        while (System.nanoTime() < deadline) { if (fx(condition::getAsBoolean)) return; Thread.sleep(25); }
        fail("UI did not reach the expected state.");
    }
}
