# Quarkus REST Client Timeouts Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build a minimal Java 21 Quarkus application whose tests visibly demonstrate the exception chains produced by REST Client read and connect timeouts.

**Architecture:** A declarative `@RegisterRestClient` interface is configured through client-specific Quarkus properties. A Quarkus test resource runs WireMock on a random port and delays an HTTP response for the deterministic read-timeout case; a separate test profile points the same client at a configurable black-hole IP for the genuine TCP connect-timeout case.

**Tech Stack:** Java 21, Maven, Quarkus 3.40.1.1 LTS, `quarkus-rest-client`, Quarkus JUnit 5, WireMock standalone 3.13.2.

**Spec:** `docs/superpowers/specs/2026-11-05-rest-client-timeouts-design.md`

## Global Constraints

- Keep the example small and use Java 21.
- Use `quarkus-rest-client`; do not add a custom exception mapper.
- Use WireMock only for the delayed HTTP response, not for the TCP connection timeout.
- Do not require Docker or Testcontainers.
- Print complete exception chains and identify both the outer exception and timeout cause.
- Make the black-hole host overridable because routing behavior is environment-dependent.

## Review Focus

- A delayed response must exceed `read-timeout` but not be confused with connection establishment; Task 1 verifies WireMock received the request before the timeout.
- The outer exception must remain Quarkus REST Client's native `jakarta.ws.rs.ProcessingException`; both timeout tests assert it.
- The read test must prove the nested cause is the Vert.x timeout rather than merely matching an arbitrary message; Task 1 checks its concrete simple name and timeout text.
- A network that rejects the black-hole IP immediately must produce an actionable failure explaining the override property; Task 2 checks the cause chain and supplies that diagnostic.
- The short timeout configuration must be visibly client-specific and measured in milliseconds; Tasks 1 and 2 assert completion within bounded JUnit timeouts and README documents the values.

---

### Task 1: Minimal client and deterministic read-timeout test

**Files:**
- Modify: `pom.xml`
- Create: `src/main/java/dev/ldv/timeout/DelayedServiceClient.java`
- Create: `src/main/resources/application.properties`
- Create: `src/test/java/dev/ldv/timeout/DelayedServiceWireMockResource.java`
- Create: `src/test/java/dev/ldv/timeout/ReadTimeoutTest.java`

**Interfaces:**
- Consumes: no application interfaces; the dynamic WireMock URL is returned as `quarkus.rest-client.delayed-service.url`.
- Produces: `DelayedServiceClient#get()` returning `String`, registered with config key `delayed-service`; shared timeout configuration used by Task 2.

- [ ] **Step 1: Write the failing read-timeout integration test and WireMock resource**

Create `ReadTimeoutTest#throwsProcessingExceptionWithVertxTimeoutCause()` to inject `@RestClient DelayedServiceClient`, invoke `get()`, print the caught exception with `printStackTrace(System.out)`, and assert `ProcessingException`, a nested `NoStackTraceTimeoutException`, timeout text, WireMock request receipt, and completion under five seconds. Configure the resource to delay `GET /delayed` for 1,000 ms.

- [ ] **Step 2: Run the focused test to verify RED**

Run: `mvn -Dtest=ReadTimeoutTest test`

Expected: compilation/test failure because the Quarkus build and `DelayedServiceClient` are not implemented yet.

- [ ] **Step 3: Add the minimal Quarkus build, REST client, and timeout configuration**

Import `io.quarkus.platform:quarkus-bom:3.40.1.1`, add `quarkus-rest-client`, `quarkus-junit5`, and test-scoped `org.wiremock:wiremock-standalone:3.13.2`, plus the Quarkus Maven plugin. Define `DelayedServiceClient` as `@Path("/delayed")`, `@RegisterRestClient(configKey = "delayed-service")`, with `@GET String get()`. Set the client-specific connect and read timeouts to 250 ms and provide a harmless default URL in `application.properties`.

- [ ] **Step 4: Run the focused test to verify GREEN**

Run: `mvn -Dtest=ReadTimeoutTest test`

Expected: one passing test; output includes `jakarta.ws.rs.ProcessingException` and the concrete Vert.x timeout cause.

- [ ] **Step 5: Commit Task 1**

```powershell
git add -- pom.xml src/main src/test/java/dev/ldv/timeout/DelayedServiceWireMockResource.java src/test/java/dev/ldv/timeout/ReadTimeoutTest.java
git commit -m "test: demonstrate REST client read timeout"
```

### Task 2: Genuine TCP connect-timeout test

**Files:**
- Create: `src/test/java/dev/ldv/timeout/ConnectTimeoutProfile.java`
- Create: `src/test/java/dev/ldv/timeout/ConnectTimeoutTest.java`

**Interfaces:**
- Consumes: `DelayedServiceClient#get()` and config key `delayed-service` from Task 1.
- Produces: `ConnectTimeoutProfile`, which reads system property `timeout.blackhole.host` (default `192.0.2.1`) and overrides the client URL and timeout values.

- [ ] **Step 1: Write the failing connect-timeout integration test**

Create `ConnectTimeoutTest#throwsProcessingExceptionWithVertxConnectTimeoutCause()` under `@TestProfile(ConnectTimeoutProfile.class)`. Invoke the client, print the full exception, assert outer `ProcessingException`, a nested Vert.x timeout exception and connection-specific timeout text, and complete under five seconds. If the network immediately refuses/rejects the address, fail with a message telling the user to rerun with `-Dtimeout.blackhole.host=<dropping-address>` and include the observed chain. Leave `ConnectTimeoutProfile` absent for the RED run.

- [ ] **Step 2: Run the focused test to verify RED**

Run: `mvn -Dtest=ConnectTimeoutTest test`

Expected: failure until the profile supplies the black-hole URL and exact observed timeout assertions are aligned with Quarkus 3.40.1.1.

- [ ] **Step 3: Implement the minimal profile and align assertions to the observed native exception**

Implement `QuarkusTestProfile#getConfigOverrides()` with `http://<host>:81`, `connect-timeout=250`, and `read-timeout=5000`. Preserve the native exception unchanged; only test assertions and diagnostics inspect it.

- [ ] **Step 4: Run the focused test to verify GREEN**

Run: `mvn -Dtest=ConnectTimeoutTest test`

Expected on a dropping route: one passing test; output includes `jakarta.ws.rs.ProcessingException` and the concrete Vert.x connection-timeout cause. On an intercepting/rejecting route: the intentional actionable assertion failure names the override property.

- [ ] **Step 5: Commit Task 2**

```powershell
git add -- src/test/java/dev/ldv/timeout/ConnectTimeoutProfile.java src/test/java/dev/ldv/timeout/ConnectTimeoutTest.java
git commit -m "test: demonstrate REST client connect timeout"
```

### Task 3: Usage documentation and full verification

**Files:**
- Create: `README.md`
- Modify: `docs/superpowers/plans/2026-11-05-rest-client-timeouts.md`

**Interfaces:**
- Consumes: exact exception class names and messages observed in Tasks 1 and 2.
- Produces: commands and explanation sufficient to reproduce and interpret both timeout cases.

- [ ] **Step 1: Write README usage and expected-output documentation**

Document `mvn test`, focused test commands, the `timeout.blackhole.host` override, the exact observed outer/cause classes, the distinction between read and connect timeout, and why WireMock cannot emulate the latter. Link the relevant official Quarkus REST Client guide.

- [ ] **Step 2: Run the complete test suite**

Run: `mvn test`

Expected: all tests pass on a dropping route; otherwise only the connect test fails with the documented actionable network diagnostic.

- [ ] **Step 3: Inspect the final diff and requirements**

Run: `git diff --check` and `git status --short`

Expected: no whitespace errors; only intended application, tests, documentation, and Maven build changes remain.

- [ ] **Step 4: Commit documentation and plan completion**

```powershell
git add -- README.md docs/superpowers
git commit -m "docs: explain REST client timeout exceptions"
```
