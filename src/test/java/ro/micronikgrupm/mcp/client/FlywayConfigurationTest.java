package ro.micronikgrupm.mcp.client;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class FlywayConfigurationTest {

    @Autowired
    private Flyway flyway;

    @Test
    void flywayBeanIsConfigured() {
        assertThat(flyway).isNotNull();
    }

    @Test
    void h2ProfileDoesNotApplyPostgresOnlyMigrations() {
        var versions = java.util.Arrays.stream(flyway.info().all())
                .map(mi -> mi.getVersion().toString())
                .toList();

        assertThat(versions).contains("1", "2", "3", "4");
        assertThat(versions).doesNotContain("5");
    }
}
