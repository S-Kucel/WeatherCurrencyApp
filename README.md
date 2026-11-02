# Weather & Currency

[Polski](#polski) | [English](#english)

## Polski

Weather & Currency to aplikacja desktopowa, którą napisałem w **Javie 21 i JavaFX**.
Łączy prognozę pogody dla wybranego miasta z przelicznikiem walut opartym na
kursach Narodowego Banku Polskiego. Interfejs aplikacji jest w języku polskim.

### Funkcje

- **Wyszukiwanie lokalizacji** — wybór miasta wraz z regionem i krajem.
- **Aktualna pogoda** — temperatura, warunki atmosferyczne, temperatura
  odczuwalna, wilgotność i prędkość wiatru.
- **Prognoza** — widok na 7 dni oraz wykres temperatury na najbliższe 24 godziny,
  z uwzględnieniem strefy czasowej wybranego miasta.
- **Przelicznik walut** — przeliczanie między walutami z tabeli A NBP i PLN,
  zamiana kierunku oraz obsługa kwot z przecinkiem lub kropką.
- **Tabela kursów** — filtrowanie po kodzie lub nazwie waluty, ręczne odświeżanie
  i data ostatniej publikacji NBP.
- **Obsługa połączenia** — pobieranie danych w tle, limity czasu i komunikaty
  o błędach. Przy nieudanym odświeżeniu pozostają widoczne ostatnio pobrane dane.

### Technologie

| Obszar | Technologie |
| --- | --- |
| Język | Java 21 |
| Interfejs | JavaFX 21, CSS |
| Komunikacja i dane | `java.net.http.HttpClient`, Gson |
| Budowanie i testy | Maven, JUnit 5 |

### Uruchomienie

Wymagania: **JDK 21 lub nowsze**, Windows oraz dostęp do internetu do pobrania
zależności i danych z API. Ustaw `JAVA_HOME` albo dodaj katalog `bin` JDK do `PATH`.

W katalogu projektu uruchom:

```powershell
.\run.cmd
```

Możesz też otworzyć `run.cmd` dwuklikiem. Przy pierwszym uruchomieniu skrypt
pobiera Maven, weryfikuje sumę SHA-512 archiwum i instaluje zależności w `.tools/`.
JavaFX jest pobierany automatycznie. Konfiguracja kluczy API nie jest wymagana.

Z zainstalowanym Mavenem aplikację można uruchomić również na Linuxie i macOS:

```shell
mvn javafx:run
```

### Kompilacja i testy

```powershell
.\run.cmd -Action Build     # kompilacja, testy i plik JAR w target/
.\test.cmd                 # standardowy zestaw testów
.\run.cmd -Action CheckApi  # sprawdzenie połączenia z rzeczywistymi API
```

Przy korzystaniu z własnej instalacji Mavena testy uruchamia polecenie `mvn clean test`.
Testy obejmują przeliczanie walut, walidację danych, odczyt JSON, błędy HTTP,
limity czasu oraz interakcje w JavaFX. Korzystają z lokalnego serwera HTTP,
więc po pobraniu zależności standardowy zestaw działa bez dostępu do zewnętrznych API.
Testy interfejsu wymagają sesji graficznej.

Raporty znajdują się w `target/surefire-reports/`, a podglądy interfejsu
w `target/*-preview.png`. Plik JAR nie zawiera środowiska JavaFX;
do uruchomienia aplikacji służy `run.cmd` lub cel Maven `javafx:run`.

### Przeliczanie walut

Obliczenia wykorzystują `BigDecimal`. Kursy krzyżowe wyznaczane są przez PLN:

```text
wynik = kwota × kurs waluty źródłowej w PLN / kurs waluty docelowej w PLN
```

Kursy odnoszą się do jednej jednostki waluty, również w przypadku JPY.
Wyświetlany wynik jest zaokrąglany do dwóch miejsc po przecinku metodą `HALF_UP`.
Aplikacja pokazuje rzeczywistą datę tabeli NBP, także w weekendy i święta.
Przeliczenie korzysta z kursów średnich i nie uwzględnia prowizji ani opłat.

### Struktura projektu

```text
src/main/java/pl/weathercurrency/
  Launcher.java          punkt wejścia
  WeatherCurrencyApp.java  inicjalizacja JavaFX
  ApiCheck.java          sprawdzenie zewnętrznych API
  model/                 modele danych
  service/               komunikacja HTTP, JSON i obliczenia
  ui/                    widoki i komponenty JavaFX
src/main/resources/styles/app.css  style interfejsu
src/test/java/pl/weathercurrency/  testy i lokalny serwer HTTP
```

W IDE otwórz katalog jako projekt Maven lub zaimportuj `pom.xml`, ustaw JDK 21+
i uruchom cel `javafx:run`. W VS Code dostępne jest także zadanie
**Uruchom Weather & Currency**.

### GitHub Desktop

Dodaj katalog projektu przez **File → Add Local Repository…**. Aby opublikować
lokalne repozytorium, wybierz **Publish repository**, ustaw nazwę i zdecyduj,
czy pozostawić zaznaczone **Keep this code private**. Kolejne zmiany wysyłaj
przez **Commit to main**, a następnie **Push origin**.
[Instrukcja publikacji](https://docs.github.com/en/desktop/adding-and-cloning-repositories/adding-an-existing-project-to-github-using-github-desktop).

`.gitignore` pomija pliki wynikowe w `target/`, narzędzia w `.tools/`, lokalne
pliki `.env` i logi. Po sklonowaniu repozytorium aplikację uruchamia `run.cmd`.

### Źródła danych

- **Pogoda:** [Open-Meteo Forecast API](https://open-meteo.com/en/docs).
  Dane są udostępniane na licencji [CC BY 4.0](https://open-meteo.com/en/licence).
- **Lokalizacje:** [Open-Meteo Geocoding API](https://open-meteo.com/en/docs/geocoding-api),
  baza GeoNames.
- **Kursy walut:** [NBP Web API](https://api.nbp.pl/), ostatnia opublikowana tabela A.

Publiczne API Open-Meteo jest przeznaczone do użytku niekomercyjnego
i podlega [zasadom dostępu oraz limitom zapytań](https://open-meteo.com/en/pricing).

---

## English

Weather & Currency is a desktop application I built with **Java 21 and JavaFX**.
It combines weather forecasts for a selected city with a currency converter
using exchange rates from the National Bank of Poland (NBP). The application
interface is in Polish.

### Features

- **Location search** — select a city with its region and country.
- **Current weather** — temperature, conditions, feels-like temperature,
  humidity and wind speed.
- **Forecasts** — a 7-day outlook and a temperature chart for the next 24 hours,
  using the selected city's time zone.
- **Currency conversion** — convert between currencies from NBP Table A and PLN,
  swap the conversion direction and enter amounts with a comma or decimal point.
- **Exchange rate table** — filter by currency code or name, refresh manually
  and view the date of the latest NBP publication.
- **Connection handling** — background requests, timeouts and error messages.
  The last retrieved data remains visible if a refresh fails.

### Technologies

| Area | Technologies |
| --- | --- |
| Language | Java 21 |
| Interface | JavaFX 21, CSS |
| Networking and data | `java.net.http.HttpClient`, Gson |
| Build and testing | Maven, JUnit 5 |

### Getting started

Requirements: **JDK 21 or newer**, Windows and an internet connection to download
dependencies and retrieve API data. Set `JAVA_HOME` or add your JDK's `bin`
directory to `PATH`.

Run the following command from the project directory:

```powershell
.\run.cmd
```

You can also double-click `run.cmd`. On the first run, the script downloads
Maven, verifies the archive's SHA-512 checksum and installs dependencies in
`.tools/`. JavaFX is downloaded automatically. API key configuration is not required.

With Maven installed, the application can also be launched on Linux and macOS:

```shell
mvn javafx:run
```

### Building and testing

```powershell
.\run.cmd -Action Build     # compile, run tests and create a JAR in target/
.\test.cmd                 # run the standard test suite
.\run.cmd -Action CheckApi  # check connectivity to the live APIs
```

With your own Maven installation, run tests using `mvn clean test`.
Tests cover currency calculations, input validation, JSON parsing, HTTP errors,
timeouts and JavaFX interactions. They use a local HTTP server, so the standard
suite runs without access to external APIs once dependencies have been downloaded.
Interface tests require a graphical session.

Test reports are saved in `target/surefire-reports/` and interface previews
in `target/*-preview.png`. The JAR does not bundle the JavaFX runtime;
launch the application using `run.cmd` or the Maven goal `javafx:run`.

### Currency calculations

Calculations use `BigDecimal`. Cross rates are calculated through PLN:

```text
result = amount × source currency rate in PLN / target currency rate in PLN
```

Rates represent one unit of each currency, including JPY. Displayed results
are rounded to two decimal places using `HALF_UP`. The application shows the
actual NBP table date, including on weekends and public holidays. Conversion
uses average exchange rates and excludes commissions and fees.

### Project structure

```text
src/main/java/pl/weathercurrency/
  Launcher.java          entry point
  WeatherCurrencyApp.java  JavaFX initialization
  ApiCheck.java          external API connectivity check
  model/                 data models
  service/               HTTP communication, JSON and calculations
  ui/                    JavaFX views and components
src/main/resources/styles/app.css  interface styles
src/test/java/pl/weathercurrency/  tests and local HTTP server
```

In your IDE, open the directory as a Maven project or import `pom.xml`, select
JDK 21+ and run the `javafx:run` goal. VS Code also includes the task
**Uruchom Weather & Currency**.

### GitHub Desktop

Add the project directory using **File → Add Local Repository…**. To publish
the local repository, select **Publish repository**, choose a name and decide
whether to keep **Keep this code private** selected. Upload subsequent changes
using **Commit to main**, followed by **Push origin**.
[Publishing guide](https://docs.github.com/en/desktop/adding-and-cloning-repositories/adding-an-existing-project-to-github-using-github-desktop).

`.gitignore` excludes build output in `target/`, tools in `.tools/`, local `.env`
files and logs. After cloning the repository, launch the application with `run.cmd`.

### Data sources

- **Weather:** [Open-Meteo Forecast API](https://open-meteo.com/en/docs).
  Data is provided under [CC BY 4.0](https://open-meteo.com/en/licence).
- **Locations:** [Open-Meteo Geocoding API](https://open-meteo.com/en/docs/geocoding-api),
  GeoNames database.
- **Exchange rates:** [NBP Web API](https://api.nbp.pl/), the latest published Table A.

The public Open-Meteo API is intended for non-commercial use and is subject
to its [access terms and request limits](https://open-meteo.com/en/pricing).
