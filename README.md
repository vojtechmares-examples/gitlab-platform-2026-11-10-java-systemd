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

## CI configuration

The deploy jobs read the CI/CD variables below (*Settings > CI/CD > Variables*). Variables that start with `CI_` are predefined by GitLab. This project needs none of its own (`KEEP_RELEASES` is optional).

| Variable | Set on | Type | Used for |
|----------|--------|------|----------|
| `DEPLOY_HOST` | group `examples` | Variable | Address of the deploy server |
| `DEPLOY_SSH_PORT` | group `examples` | Variable | SSH port of the deploy server (`6022`) |
| `DEPLOY_USER` | group `examples` | Variable | Account the pipelines log in as (`deploy`) |
| `SSH_KNOWN_HOSTS` | group `examples` | Variable | Host keys of the deploy server; the connection uses `StrictHostKeyChecking yes` |
| `SSH_PRIVATE_KEY` | group `examples` | File, **protected** | Private key of the `deploy` account |
| `EXAMPLES_DOMAIN` | group `examples` | Variable | Parent domain of the applications (`examples.maresdemo.com`); `deploy/release.sh` waits for `https://java-systemd-<env>.$EXAMPLES_DOMAIN` |
| `KEEP_RELEASES` | optional, this project | Variable | How many release directories `deploy/release.sh` keeps on the server (default `5`) |

The group variables are inherited by every project in the `examples` group, so nothing has to be set for them here. The `.ssh-setup` snippet of `examples/ci-templates` turns the first five into `ssh deploy`.
A **protected** variable is passed only to pipelines on protected branches and tags (`main`), which is where the deploy jobs run.

The pipeline does not read a database password. Flyway connects when the application starts, with the password in `/etc/examples/java-systemd-<env>.env`, which the `ansible` project writes from its variable `DB_PASSWORD_JAVA_SYSTEMD`. The `test` job uses a throwaway PostgreSQL service with the credentials written in `.gitlab-ci.yml`.
