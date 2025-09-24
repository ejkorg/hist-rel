```markdown
# Run the backend with H2 (local development)

This repository supports an H2 profile for local development and CI verification. The H2 profile is DB-agnostic friendly and uses MODE=Oracle for compatibility with some DDL.

How to run locally with H2:
1. Build:
   mvn -f backend/pom.xml -DskipTests clean package

2. Run with the `h2` profile:
   export SPRING_PROFILES_ACTIVE=h2
   java -jar backend/target/hist-rel-backend-0.1.0.jar

   or with Maven:
   mvn -f backend spring-boot:run -Dspring-boot.run.profiles=h2

3. Access the H2 console (dev only):
   Open: http://localhost:8080/h2-console
   JDBC URL: jdbc:h2:mem:histrel;DB_CLOSE_DELAY=-1;MODE=Oracle
   User: sa
   Password: (empty)

What to expect:
- Liquibase changeSets under src/main/resources/db/changelog will be applied and create the `sender_queue` and `sender_config` tables.
- The sender scheduled job will run (default every 5 minutes), and you can use the existing REST endpoints to enqueue items and trigger runs.
- This profile is intended for local testing and development only. Do not use H2 in production.

Switching to another DB in production:
- Add a profile for your production DB (oracle/postgres/mysql) with proper JDBC URL and credentials.
- Keep Liquibase changelogs vendor-agnostic; for vendor-specific DDL use `dbms` attributes or separate change sets.
```