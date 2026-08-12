# commons-observability (1.0.3 - simple)

No lambdas, no `var`. Auto-configured **correlation id** + **access logging** for Spring Boot.

## Install
```bash
mvn -q -DskipTests install
```

## Use in a microservice
```xml
<dependency>
  <groupId>com.commons</groupId>
  <artifactId>commons-observability</artifactId>
  <version>1.0.3</version>
</dependency>
```

`application.yml` (optional):
```yaml
commons:
  logging:
    filter:
      enabled: true
    feign:
      enabled: true
```

Service `logback.xml`:
```xml
<configuration>
  <include resource="logback-base.xml"/>
</configuration>
```
