# Quarkus REST Client: исключения при таймаутах

Минимальный проект на Java 21 и Quarkus 3.40.1.1, который показывает реальные
исключения `quarkus-rest-client` при срабатывании `read-timeout` и
`connect-timeout`.

## Короткий ответ

В обоих случаях вызывающий код получает
`jakarta.ws.rs.ProcessingException`, но вложенные причины различаются:

| Сценарий | Исключение, получаемое приложением | Вложенная причина |
| --- | --- | --- |
| Ответ сервера не пришёл вовремя (`read-timeout`) | `jakarta.ws.rs.ProcessingException` | `io.vertx.core.impl.NoStackTraceTimeoutException` |
| TCP-соединение не установлено вовремя (`connect-timeout`) | `jakarta.ws.rs.ProcessingException` | `io.netty.channel.ConnectTimeoutException` |

Это наблюдаемое поведение Quarkus 3.40.1.1 с REST Client на базе Vert.x/Netty.
В прикладном коде обычно следует перехватывать стабильное внешнее исключение
`ProcessingException`, а конкретную причину использовать для диагностики:
внутренние классы могут измениться при обновлении Quarkus.

## Конфигурация клиента

Интерфейс [`DelayedServiceClient`](src/main/java/dev/ldv/timeout/DelayedServiceClient.java)
зарегистрирован с `configKey = "delayed-service"`. Таймауты задаются для этого
клиента в миллисекундах:

```properties
quarkus.rest-client.delayed-service.connect-timeout=250
quarkus.rest-client.delayed-service.read-timeout=250
```

Имена и семантика свойств соответствуют
[официальному руководству Quarkus REST Client](https://quarkus.io/guides/rest-client/).

## Read timeout

[`ReadTimeoutTest`](src/test/java/dev/ldv/timeout/ReadTimeoutTest.java) запускает
WireMock на случайном локальном порту. WireMock принимает соединение и запрос
`GET /delayed`, но задерживает ответ на 1000 мс. Клиент прекращает ожидание через
250 мс и печатает цепочку:

```text
jakarta.ws.rs.ProcessingException:
  The timeout period of 250ms has been exceeded while executing GET /delayed ...
Caused by: io.vertx.core.impl.NoStackTraceTimeoutException: ...
```

Запустить только этот сценарий:

```shell
mvn -Dtest=ReadTimeoutTest test
```

## Connect timeout

WireMock не подходит для `connect-timeout`: когда WireMock может задержать
HTTP-ответ, TCP-соединение уже установлено. Поэтому
[`ConnectTimeoutTest`](src/test/java/dev/ldv/timeout/ConnectTimeoutTest.java)
подключается к адресу `192.0.2.1:81`. Диапазон `192.0.2.0/24` предназначен для
документации; в проверенной сети пакеты к этому адресу отбрасываются, поэтому
срабатывает настоящий таймаут TCP-подключения:

```text
jakarta.ws.rs.ProcessingException:
  io.netty.channel.ConnectTimeoutException: connection timed out after 250 ms ...
Caused by: io.netty.channel.ConnectTimeoutException: ...
```

Запустить только этот сценарий:

```shell
mvn -Dtest=ConnectTimeoutTest test
```

Сетевой маршрутизатор, VPN или корпоративный firewall может немедленно вернуть
`Connection refused`/`No route to host` вместо отбрасывания пакетов. Это другой
сбой, а не достижение `connect-timeout`, поэтому тест завершится с понятной
ошибкой. Укажите адрес, который в вашей сети действительно не отвечает:

```shell
mvn -Dtimeout.blackhole.host=10.255.255.1 -Dtest=ConnectTimeoutTest test
```

## Запуск всех тестов

```shell
mvn test
```

Оба теста печатают полные stack trace в стандартный вывод и одновременно
проверяют внешний `ProcessingException` и конкретную вложенную причину.
