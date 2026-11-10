# java-systemd

Spring Boot application (JDBC, Flyway, PostgreSQL) deployed to a Linux server as a **systemd unit**.

| Topic | How |
|-------|-----|
| Build | Maven 3.9 and OpenJDK 21 from Rocky Linux 10 packages, in a Rocky Linux job image |
| Tests | JUnit; `MigrationsTest` starts the application against a PostgreSQL **job service** (`services:` in `.gitlab-ci.yml`, pulled through the group's Dependency Proxy) |
| Database migrations | Flyway, **when the application starts** (`src/main/resources/db/migration`), not in the pipeline |
| Upload | `rsync` over SSH into `/srv/java-systemd/<env>/releases/<pipeline id>/app.jar` |
| Switching versions | the symlink `/srv/java-systemd/<env>/current` is replaced atomically (`ln -sfn` + `mv -T`); the unit runs `current/app.jar` |
| Run | `java-systemd-<env>.service` (systemd, user `svc-java`), configured by the Ansible project |
| HTTPS | Caddy: https://java-systemd-staging.examples.maresdemo.com, https://java-systemd-production.examples.maresdemo.com |
| Environments | `staging` (automatic on `main`), `production` (manual job; protected environment) |
| Rollback | manual `rollback:<env>` job points `current` at the previous release; the last 5 releases are kept |

Run it on a laptop (any PostgreSQL):

```bash
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/app SPRING_DATASOURCE_USERNAME=app SPRING_DATASOURCE_PASSWORD=app mvn verify
```
