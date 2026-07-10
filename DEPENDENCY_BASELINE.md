# Dependency baseline — 2026-07-05

- Branch: `enhancements/2026-07-01`
- Starting commit: `abb723942a4ee255f84253797520c4740091010c`
- Starting tag: none
- `pom.xml` SHA-256:
  `3C42693447160E5938F66E64143FD77784D6AE247C95508CD2CF190543FD9D99`
- Java: Oracle JDK 21.0.9
- Maven Wrapper: 3.9.12

`pom.xml` is the requested dependency manifest. Maven resolves transitive
versions through the Spring Boot parent and the dependency-management entries;
CI records the effective tree with `mvn dependency:tree`.
