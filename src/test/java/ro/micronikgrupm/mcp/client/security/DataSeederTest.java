package ro.micronikgrupm.mcp.client.security;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class DataSeederTest {

    @Autowired
    private AppUserRepository repository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void seedsAdminAndUserOnStartup() {
        AppUser admin = repository.findByUsername("admin").orElseThrow();
        assertThat(admin.getRole()).isEqualTo(Role.ADMIN);
        assertThat(passwordEncoder.matches("admin123", admin.getPasswordHash())).isTrue();

        AppUser user = repository.findByUsername("user").orElseThrow();
        assertThat(user.getRole()).isEqualTo(Role.USER);
        assertThat(passwordEncoder.matches("user123", user.getPasswordHash())).isTrue();
    }
}
