package cz.maresdemo.examples;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.simple.JdbcClient;

/**
 * Starts the whole application against a real PostgreSQL. In the pipeline that database is a job
 * "service" (a sidecar container named "postgres"); on a laptop use any PostgreSQL and set
 * SPRING_DATASOURCE_URL / _USERNAME / _PASSWORD.
 */
@SpringBootTest
class MigrationsTest {

    @Autowired
    JdbcClient jdbc;
    @Autowired
    NoteController notes;

    @Test
    void flywayMigratedTheSchemaOnStartup() {
        long applied = jdbc.sql("SELECT count(*) FROM flyway_schema_history WHERE success").query(Long.class).single();
        assertThat(applied).isGreaterThanOrEqualTo(2);
    }

    @Test
    void notesCanBeCreatedAndListed() {
        var created = notes.create(java.util.Map.of("text", "from the integration test"));

        assertThat(created).containsEntry("text", "from the integration test");
        assertThat(notes.list()).extracting(row -> row.get("text")).contains("from the integration test");
    }
}
