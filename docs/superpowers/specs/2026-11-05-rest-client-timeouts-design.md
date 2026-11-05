# Quarkus REST Client timeout demonstration

## Intent

Create a small Java 21 Quarkus application that makes the exception behavior of
the Quarkus REST Client visible when `read-timeout` and `connect-timeout` expire.
The example is intended for learning and local experimentation, not as a
production error-handling layer.

## Goals

- Use the current `quarkus-rest-client` extension and a declarative REST client.
- Configure short, client-specific `read-timeout` and `connect-timeout` values.
- Reproduce `read-timeout` deterministically with a delayed WireMock response.
- Exercise a real TCP `connect-timeout` against a non-responsive address.
- Print the complete exception cause chain and assert the observed exception
  types/messages so test output answers the original question directly.
- Document how to run both tests and why connect-timeout behavior can depend on
  the host network.

## Non-goals and constraints

- No Docker or Testcontainers dependency.
- WireMock cannot reproduce `connect-timeout`, because it accepts the TCP
  connection before it can delay an HTTP response.
- A black-hole address can be rejected immediately by some networks instead of
  timing out. The connect test will therefore use a configurable address and
  clearly distinguish a genuine timeout from an immediate routing/refusal
  error rather than presenting the latter as a Quarkus timeout.
- No custom exception mapper: the example must expose Quarkus REST Client's
  native exception and cause chain.

## Approaches considered

1. **Selected:** WireMock for read timeout plus a non-responsive IP for connect
   timeout. This keeps the project small and exercises the real client stack,
   with an explicitly documented network caveat.
2. Docker/Testcontainers with controlled firewall rules. More reproducible for
   connect timeout, but too heavy for this small demonstration.
3. WireMock for both cases. Rejected because WireMock cannot delay the TCP
   handshake and therefore cannot prove `connect-timeout` behavior.

## Proposed structure

- A minimal Maven/Quarkus build using Java 21.
- One `@RegisterRestClient` interface with a simple `GET` operation.
- Client-specific timeout configuration in test properties.
- A Quarkus test resource that starts WireMock on a dynamic port and exposes
  that URL to the REST client.
- One test class for delayed-response/read timeout and one test class for the
  black-hole/connect timeout, sharing a small test-only cause-chain formatter.
- A README containing commands, expected exception shape, and instructions for
  overriding the black-hole host when the default address is intercepted by the
  local network.

## Success criteria

The read-timeout test reliably passes and prints the outer Quarkus/Jakarta REST
exception and its timeout cause. On a network where the configured destination
drops connection attempts, the connect-timeout test likewise passes and prints
its chain; otherwise it fails with an actionable explanation that the network
returned an immediate connection error and shows how to override the target.
