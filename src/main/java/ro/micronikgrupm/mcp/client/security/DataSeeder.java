package ro.micronikgrupm.mcp.client.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/** Auto-generated documentation. */
@Configuration
public class DataSeeder {

  @Bean
  public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }

  /** Auto-generated documentation. */
  @Bean
  public CommandLineRunner seedUsers(
      AppUserRepository repository,
      PasswordEncoder passwordEncoder,
      @Value("${app.seed.admin-username}") String adminUsername,
      @Value("${app.seed.admin-password}") String adminPassword,
      @Value("${app.seed.user-username}") String userUsername,
      @Value("${app.seed.user-password}") String userPassword) {
    return args -> {
      repository
          .findByUsername(adminUsername)
          .ifPresentOrElse(
              u -> {},
              () -> {
                AppUser admin = new AppUser();
                admin.setUsername(adminUsername);
                admin.setPasswordHash(passwordEncoder.encode(adminPassword));
                admin.setRole(Role.ADMIN);
                repository.save(admin);
              });
      repository
          .findByUsername(userUsername)
          .ifPresentOrElse(
              u -> {},
              () -> {
                AppUser user = new AppUser();
                user.setUsername(userUsername);
                user.setPasswordHash(passwordEncoder.encode(userPassword));
                user.setRole(Role.USER);
                repository.save(user);
              });
    };
  }
}
