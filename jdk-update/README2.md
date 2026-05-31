# Virtual Threads (Project Loom)

## Problem tradycyjnych wątków

Przez ponad 25 lat podstawowym mechanizmem współbieżności w Javie były wątki systemowe (ang. Platform Threads).

Każdy obiekt klasy `Thread` był mapowany praktycznie 1:1 na wątek systemu operacyjnego.

```java
Thread thread = new Thread(() -> {
    System.out.println("Hello");
});

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

W praktyce większość aplikacji kończyła z pulą:

```java
ExecutorService executor =
        Executors.newFixedThreadPool(200);
```

i musiała ręcznie ograniczać liczbę równoległych operacji.

---

## Project Loom

Project Loom to wieloletni projekt OpenJDK mający uprościć programowanie współbieżne.

Jego najważniejszym elementem są Virtual Threads.

Virtual Thread:

* jest obiektem klasy `Thread`
* zachowuje się jak zwykły wątek
* nie jest bezpośrednio przypisany do wątku systemowego

Tworzony jest przez JVM.

---

## Tworzenie Virtual Thread

Od Java 21:

```java
Thread.startVirtualThread(() -> {
    System.out.println("Hello Virtual Thread");
});
```

lub

```java
Thread.Builder builder = Thread.ofVirtual();

Thread thread = builder.start(() -> {
    System.out.println("Hello");
});
```

---

## Executor dla Virtual Threads

Najczęściej używana forma:

```java
try (var executor =
        Executors.newVirtualThreadPerTaskExecutor()) {

    executor.submit(() -> {
        Thread.sleep(Duration.ofSeconds(1));
        return "Done";
    });
}
```

Każde zadanie otrzymuje własny Virtual Thread.

---

## Dlaczego są szybsze?

Virtual Thread nie oznacza szybszego wykonywania kodu.

Oznacza możliwość uruchomienia ogromnej liczby operacji równolegle.

Przykład:

```java
for (int i = 0; i < 1_000_000; i++) {
    Thread.startVirtualThread(() -> {
        Thread.sleep(Duration.ofSeconds(1));
    });
}
```

Milion takich wątków jest realnie osiągalny.

Dla klasycznych wątków byłoby to praktycznie niemożliwe.

---

## Carrier Threads

Virtual Threads wykonują się na niewielkiej liczbie prawdziwych wątków systemowych.

Nazywamy je Carrier Threads.

Schemat:

Virtual Threads
↓
Carrier Threads
↓
System Operacyjny

Dzięki temu JVM może efektywnie przełączać zadania bez angażowania systemu operacyjnego.

---

## Kiedy Virtual Thread zostaje "odłączony"?

Jeżeli wykonuje operację blokującą:

```java
Thread.sleep(...)
Socket.read(...)
HttpClient.send(...)
```

JVM odłącza Virtual Thread od Carrier Thread.

Carrier Thread może obsługiwać inne zadania.

Po zakończeniu operacji Virtual Thread zostaje ponownie podłączony.

To właśnie daje ogromne korzyści wydajnościowe.

---

## Zastosowania biznesowe

Virtual Threads są idealne dla:

* REST API
* aplikacji Spring Boot
* mikroserwisów
* aplikacji komunikujących się z bazami danych
* systemów kolejkowych
* integracji z zewnętrznymi API

Szczególnie tam, gdzie występuje dużo oczekiwania na I/O.

---

## Zalety

* bardzo prosty model programowania
* brak callback hell
* brak reaktywnego kodu w wielu przypadkach
* ogromna skalowalność
* pełna zgodność z istniejącym kodem

---

## Ograniczenia

Virtual Threads nie przyspieszają:

* obliczeń CPU-intensive
* algorytmów matematycznych
* renderingu grafiki

Największe korzyści pojawiają się przy operacjach I/O.

# Structured Concurrency

## Problem klasycznych Future

Przez wiele lat programiści używali:

```java
Future<User> user = executor.submit(...);
Future<Order> order = executor.submit(...);
```

Powodowało to kilka problemów:

* trudne zarządzanie błędami
* trudne anulowanie zadań
* wycieki wątków
* skomplikowany kod

Przykład:

```java
Future<User> user = executor.submit(this::loadUser);
Future<Account> account = executor.submit(this::loadAccount);

User u = user.get();
Account a = account.get();
```

Jeżeli jedno zadanie zakończy się błędem, drugie może nadal działać.

---

## Idea Structured Concurrency

Inspiracja pochodzi z:

* Go
* Kotlin Coroutines
* Swift

Założenie:

„zadania uruchomione razem powinny kończyć się razem”.

---

## Scope

Tworzymy zakres współbieżności:

```java
try (var scope =
         new StructuredTaskScope.ShutdownOnFailure()) {

}
```

Wszystkie zadania należą do tego samego kontekstu.

---

## Przykład

```java
try (var scope =
         new StructuredTaskScope.ShutdownOnFailure()) {

    var user =
            scope.fork(this::loadUser);

    var account =
            scope.fork(this::loadAccount);

    scope.join();
    scope.throwIfFailed();

    return new Result(
            user.get(),
            account.get()
    );
}
```

---

## Co daje join()

```java
scope.join();
```

Czeka na zakończenie wszystkich zadań.

---

## Co daje throwIfFailed()

```java
scope.throwIfFailed();
```

Jeżeli którekolwiek zadanie zakończyło się błędem:

* wyjątek zostanie zgłoszony
* pozostałe zadania zostaną anulowane

---

## ShutdownOnFailure

Najpopularniejsza strategia.

Jeżeli jedno zadanie zakończy się błędem:

* pozostałe są automatycznie zatrzymywane

To zachowanie jest zwykle pożądane w systemach biznesowych.

---

## ShutdownOnSuccess

Alternatywa:

```java
new StructuredTaskScope.ShutdownOnSuccess<>();
```

Pierwszy poprawny wynik kończy działanie pozostałych zadań.

Przykład:

* wyszukiwanie danych w wielu centrach danych
* pobieranie z wielu serwerów cache

---

## Powiązanie z Virtual Threads

Structured Concurrency została zaprojektowana razem z Project Loom.

Najczęściej każde zadanie działa jako osobny Virtual Thread.

Dzięki temu:

* kod pozostaje prosty
* zachowana jest ogromna skalowalność

---

## Zastosowania biznesowe

Przykład agregacji danych:

```text
Pobierz użytkownika
Pobierz zamówienia
Pobierz płatności
Pobierz historię logowania
```

Wszystkie operacje wykonywane są równolegle.

Wynik zwracany jest dopiero po zakończeniu wszystkich zadań.

---

## Zalety

* prostsza obsługa błędów
* automatyczne anulowanie
* lepsza czytelność kodu
* łatwiejsze debugowanie
* naturalne połączenie z Virtual Threads

A dla pozostałych tematów proponuję krótsze sekcje:

---

## Scoped Values

Warto pokazać:

* czym różnią się od `ThreadLocal`,
* dlaczego powstały wraz z Virtual Threads,
* problem wycieków pamięci w `ThreadLocal`,
* propagację kontekstu użytkownika.

Minimalny przykład:

```java
private static final ScopedValue<String> USER =
        ScopedValue.newInstance();

ScopedValue.where(USER, "admin")
        .run(() -> {
            System.out.println(USER.get());
        });
```

Najważniejszy przekaz:

> Scoped Values są dla Virtual Threads tym, czym ThreadLocal był dla klasycznych wątków.








