package cz.maresdemo.examples;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

/** A plain unit test: no Spring context, no database. */
class InfoControllerTest {

    @Test
    void infoNamesTheBuildAndTheEnvironment() throws Exception {
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles("staging");

        var info = new InfoController("java-systemd", "1.0.0", "1234", environment).info();

        assertThat(info).containsEntry("application", "java-systemd")
                .containsEntry("version", "1.0.0")
                .containsEntry("build", "1234")
                .containsEntry("environment", "staging");
    }
}
