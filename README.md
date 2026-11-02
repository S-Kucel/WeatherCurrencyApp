# Weather and Currency Application

Samodzielna aplikacja desktopowa w **Javie 21 i JavaFX** do sprawdzania pogody
i przeliczania walut. Interfejs jest po polsku. Dane są pobierane z rzeczywistych
API przez `java.net.http.HttpClient`, a JSON jest odczytywany przez Gson.

## Uruchomienie

Wymagania: **JDK 21 lub nowsze**, Windows i dostęp do internetu.
Ustaw `JAVA_HOME` albo dodaj katalog `bin` swojego JDK do `PATH`.
JavaFX ani Maven nie muszą być instalowane ręcznie.

Uruchom **`run.cmd` dwuklikiem** lub z PowerShell:

```powershell
.\run.cmd
```

Przy pierwszym uruchomieniu skrypt pobiera Maven z Maven Central, sprawdza
jego sumę SHA-512 i pobiera zależności. Narzędzia i zależności są przechowywane
w `.tools` wewnątrz projektu. Skrypt działa z dowolnego katalogu roboczego
oraz po zmianie nazwy lub przeniesieniu projektu. Nie korzysta z plików SteamLite.

## Funkcje

- **Pogoda:** wyszukiwanie miast na świecie, wybór konkretnej lokalizacji,
  aktualna temperatura i opis warunków, temperatura odczuwalna, wilgotność,
  wiatr, prognoza na 7 dni i wykres temperatury na najbliższe 24 godziny.
- **Waluty:** ostatnia opublikowana tabela A NBP, PLN jako waluta bazowa,
  przeliczanie między dowolnymi dostępnymi walutami, zamiana kierunku,
  filtrowanie tabeli i ręczne odświeżanie. Kwota akceptuje kropkę lub przecinek.
- **Obsługa sieci:** zapytania w tle, limity czasu, komunikaty o błędach i
  zachowanie ostatnich pobranych danych, gdy odświeżenie się nie powiedzie.

Wynik przeliczenia używa `BigDecimal` i jest zaokrąglany do 2 miejsc
(`HALF_UP`). Kursy krzyżowe są obliczane jako `kwota × kurs źródłowy PLN /
kurs docelowy PLN`. Kursy NBP dotyczą jednej jednostki waluty, również JPY.
Zawsze wyświetlana jest rzeczywista data tabeli, także w weekendy i święta.

## Kompilacja i testy

```powershell
.\run.cmd -Action Build   # kompilacja, testy i plik JAR w target
.\test.cmd               # testy bez dostępu do zewnętrznych API
.\run.cmd -Action CheckApi # osobny test rzeczywistych API (wymaga internetu)
```

Testy JUnit sprawdzają obliczenia, walidację, odczyt JSON, błędy HTTP,
limity czasu oraz interakcje w JavaFX. Korzystają z lokalnego serwera HTTP,
więc nie zależą od bieżącej pogody ani aktualnego kursu. Testy GUI wymagają
sesji graficznej; zapisują podglądy ekranów w `target/*-preview.png`.
Raporty testów znajdują się w `target/surefire-reports`.

Plik JAR nie zawiera środowiska JavaFX. Do uruchomienia używaj `run.cmd`
albo Maven. Mając zainstalowany Maven, możesz też wykonać na Windows,
Linux lub macOS:

```text
mvn clean test
mvn javafx:run
```

## Otwarcie w IDE

Otwórz ten katalog lub zaimportuj `pom.xml` jako projekt Maven. Ustaw Project SDK
na JDK 21 lub nowsze i poczekaj na pobranie zależności. Punkt wejścia:
`pl.weathercurrency.Launcher`. Najprostsze uruchomienie w IDE to cel Maven
`javafx:run` lub zadanie VS Code „Uruchom Weather & Currency”.

## GitHub Desktop i publikacja na GitHubie

1. Otwórz GitHub Desktop i zaloguj się na swoje konto GitHub.
2. Wybierz **File → Add Local Repository…**, wskaż katalog projektu
   (ten z `pom.xml` i `run.cmd`) i kliknij **Add Repository**.
3. Kliknij **Publish repository** i wpisz nazwę, np. `WeatherCurrencyApp`.
4. Zostaw zaznaczone **Keep this code private**, aby repozytorium było prywatne,
   lub odznacz tę opcję, aby było publiczne. Kliknij **Publish Repository**.

Po kolejnych zmianach wpisz opis w polu **Summary**, kliknij **Commit to main**,
a następnie **Push origin**, aby wysłać je na GitHuba.

Do repozytorium trafiają kod, testy, skrypty uruchomienia i konfiguracja projektu.
`.gitignore` pomija m.in. `target/`, `.tools/`, lokalne pliki `.env` oraz logi.
Po sklonowaniu projektu wystarczy JDK 21+ i uruchomienie `run.cmd`.

Instrukcja GitHuba: [publikacja istniejącego projektu przez GitHub Desktop](https://docs.github.com/en/desktop/adding-and-cloning-repositories/adding-an-existing-project-to-github-using-github-desktop).

## Struktura

```text
src/main/java/pl/weathercurrency/
  Launcher.java, WeatherCurrencyApp.java   start aplikacji
  ApiCheck.java                           kontrola rzeczywistych API
  model/                                  niemutowalne modele danych
  service/                                HTTP, JSON i przelicznik walut
  ui/                                     widoki JavaFX i grafiki wektorowe
src/main/resources/styles/app.css         wygląd aplikacji
src/test/java/pl/weathercurrency/          testy JUnit i lokalny serwer API
```

## Źródła i warunki użycia danych

- Pogoda: [Open-Meteo Forecast API](https://open-meteo.com/en/docs).
  Dane pogodowe: [CC BY 4.0](https://open-meteo.com/en/licence).
- Lokalizacje: [Open-Meteo Geocoding API](https://open-meteo.com/en/docs/geocoding-api),
  dane GeoNames. Nazwę można doprecyzować, np. `Paris, France`.
- Waluty: [NBP Web API](https://api.nbp.pl/), endpoint
  `https://api.nbp.pl/api/exchangerates/tables/a/?format=json`.

Nie trzeba podawać kluczy API do tego projektu edukacyjnego. Publiczne API
Open-Meteo podlega [zasadom dostępu i limitom](https://open-meteo.com/en/pricing);
komercyjne wykorzystanie może wymagać innego planu. Kursy są średnimi kursami
referencyjnymi, nie ofertą wymiany walut; przelicznik nie uwzględnia prowizji.
