package ro.micronikgrupm.mcp.client.security;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** AppUserRepository component. */
@SuppressWarnings("PMD.ImplicitFunctionalInterface")
public interface AppUserRepository extends JpaRepository<AppUser, Long> {
  Optional<AppUser> findByUsername(String username);
}
