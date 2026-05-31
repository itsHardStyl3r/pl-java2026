# Project Loom

## Wirtualne wątki

### Problem tradycyjnych wątków

Przez ponad 25 lat podstawowym mechanizmem współbieżności w Javie były wątki systemowe (ang. Platform Threads).

Każdy obiekt klasy `Thread` był mapowany praktycznie 1:1 na wątek systemu operacyjnego.

```java
Thread thread = new Thread(() -> System.out.println("Hello"));
thread.start();
```

Rozwiązanie to działa bardzo dobrze dla niewielkiej liczby zadań, jednak w nowoczesnych aplikacjach serwerowych pojawił się problem skalowalności.

Przykład:
* aplikacja REST
* 10 000 jednoczesnych użytkowników
* każdy request wykonuje zapytanie do bazy danych

Przy klasycznych wątkach potrzebowalibyśmy tysięcy wątków systemowych.

Każdy taki wątek:
* zajmuje pamięć (stos)
* jest zarządzany przez system operacyjny
* wymaga kosztownego przełączania kontekstu (context switching)

Project Loom to wieloletni projekt OpenJDK mający uprościć programowanie współbieżne. Jego najważniejszym elementem są Virtual Threads.
* jest obiektem klasy `Thread`
* zachowuje się jak zwykły wątek
* nie jest bezpośrednio przypisany do wątku systemowego

Tworzony jest przez JVM.

### Tworzenie Virtual Thread

Od Java 21:

```java
Thread.startVirtualThread(() -> {
    System.out.println("Hello Virtual Thread");
});
```

lub

```java
Thread.Builder builder = Thread.ofVirtual();
builder.start(() -> System.out.println("Hello"));
```

### Czy wirtualne wątki są szybsze?

Virtual Thread nie oznacza szybszego wykonywania kodu. Oznacza możliwość uruchomienia ogromnej liczby operacji równolegle.

Przykład:

```java
import com.sun.net.httpserver.HttpServer;

static void sleep(long ms) {
    try {
        Thread.sleep(ms);
    } catch (InterruptedException e) {
        throw new RuntimeException(e);
    }
}

void main() throws Exception {

    var server = HttpServer.create(new InetSocketAddress(8080), 0);

    // każdy request w osobnym wirtualnym wątku
    server.setExecutor(Executors.newVirtualThreadPerTaskExecutor());

    server.createContext("/slow", exchange -> {
        var thread = Thread.currentThread();
        System.out.println("→ START request on " + thread);

        sleep(500); // symulacja wolnego I/O

        var response = ("Handled by: " + thread).getBytes();
        exchange.sendResponseHeaders(200, response.length);
        exchange.getResponseBody().write(response);
        exchange.close();

        System.out.println("← END request on " + thread);
    });

    server.start();
    System.out.println("Server running on http://localhost:8080/slow");
}
```

Uruchom na terminalu: `seq 1 1000 | xargs -n1 -P1000 curl -s http://localhost:8080/slow > /dev/null`

### Zastosowania biznesowe

Virtual Threads są idealne dla:

* REST API
* mikroserwisów
* aplikacji komunikujących się z bazami danych
* systemów kolejkowych
* integracji z zewnętrznymi API 
* tam, gdzie występuje dużo oczekiwania na I/O.

### Zalety

* bardzo prosty model programowania
* brak callback hell
* brak reaktywnego kodu w wielu przypadkach
* ogromna skalowalność
* pełna zgodność z istniejącym kodem

Największe korzyści pojawiają się przy operacjach I/O.

## Structured Concurrency-sposób zarządzania zadaniami

### Problem klasycznych Future

```java
static ExecutorService executor = Executors.newFixedThreadPool(10);

void main() throws Exception {

    Future<String> userFuture = executor.submit(() -> loadUser());
    Future<String> ordersFuture = executor.submit(() -> loadOrders());
    Future<String> paymentsFuture = executor.submit(() -> loadPayments());

    // czekanie na wszystkie wyniki
    String user = userFuture.get();        // blokuje
    String orders = ordersFuture.get();    // blokuje
    String payments = paymentsFuture.get();// blokuje

    System.out.println("User: " + user);
    System.out.println("Orders: " + orders);
    System.out.println("Payments: " + payments);

    executor.shutdown();
}

static String loadUser() throws Exception {
    Thread.sleep(300);
    return "User#123";
}

static String loadOrders() throws Exception {
    Thread.sleep(500);
    return "Orders[5]";
}

static String loadPayments() throws Exception {
    Thread.sleep(200);
    return "Payments[OK]";
}
```

Powodowało to kilka problemów:

* trudne zarządzanie błędami
* trudne anulowanie zadań
* wycieki wątków
* skomplikowany kod

### Idea Structured Concurrency

Inspiracja pochodzi z:

* Go
* Kotlin Coroutines
* Swift

Założenie:

> zadania uruchomione razem powinny kończyć się razem

```java
void main() throws Exception {

    try (var scope = StructuredTaskScope.open()) {

        var user = scope.fork(() -> load("User", 300));
        var orders = scope.fork(() -> load("Orders", 500));

        scope.join(); // czeka na wszystkie subtasks

        System.out.println(user.get());
        System.out.println(orders.get());
    }
}

static String load(String name, long ms) throws Exception {
    Thread.sleep(ms);
    return name + " loaded";
}
```

### Powiązanie z Virtual Threads

Structured Concurrency została zaprojektowana razem z Project Loom.

Najczęściej każde zadanie działa jako osobny Virtual Thread.

Dzięki temu:

* kod pozostaje prosty
* zachowana jest ogromna skalowalność

### Zalety

* prostsza obsługa błędów
* automatyczne anulowanie
* lepsza czytelność kodu
* łatwiejsze debugowanie
* naturalne połączenie z Virtual Threads