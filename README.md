# Sportradar

Recruitment task.

## Stack

- Java 21
- Spring Boot 4.1.2 (Spring MVC, embedded Tomcat 11)
- Maven (Maven Wrapper, Maven 3.10.0)
- Lombok

## Module

The application lives in the `sportradar/` module (`com.sportradar.SportradarApplication`).

## Package conventions

Code follows the production reverse-domain convention:

- Maven `groupId`: `com.sportradar`
- Base Java package: `com.sportradar`
- Application entry point: `com.sportradar.SportradarApplication`
- New code goes into feature/layer packages under `com.sportradar` (for example `com.sportradar.<feature>.controller`, `com.sportradar.<feature>.service`, `com.sportradar.<feature>.repository`, `com.sportradar.<feature>.model`)
- Tests mirror the production package structure under `src/test/java`

## Build

Run from the `sportradar/` directory:

```powershell
# Windows
.\mvnw.cmd clean verify

# Linux / macOS
./mvnw clean verify
```

Requires `JAVA_HOME` pointing to a JDK 21 installation (or `java` available on `PATH`).

## Run

```powershell
.\mvnw.cmd spring-boot:run
```

The application starts on <http://localhost:8080>.
