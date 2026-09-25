package ro.micronikgrupm.mcp.client.security;

import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/** Auto-generated documentation. */
@Service
public class AppUserDetailsService implements UserDetailsService {

  private final AppUserRepository repository;

  public AppUserDetailsService(AppUserRepository repository) {
    this.repository = repository;
  }

  @Override
  public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
    AppUser user =
        repository
            .findByUsername(username)
            .orElseThrow(() -> new UsernameNotFoundException("Utilizator necunoscut: " + username));
    return User.withUsername(user.getUsername())
        .password(user.getPasswordHash())
        .authorities("ROLE_" + user.getRole().name())
        .build();
  }
}
