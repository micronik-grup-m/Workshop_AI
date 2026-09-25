package ro.micronikgrupm.mcp.client.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;

class SecurityConfigTest {

  private SecurityConfig securityConfig;

  @BeforeEach
  void setUp() {
    securityConfig = new SecurityConfig(mock(JwtAuthenticationFilter.class), "/api/");
  }

  @Test
  void authenticationManagerReturnsConfiguredManager() {
    AuthenticationManager manager = mock(AuthenticationManager.class);
    AuthenticationConfiguration configuration =
        new AuthenticationConfiguration() {
          @Override
          public AuthenticationManager getAuthenticationManager() {
            return manager;
          }
        };

    AuthenticationManager result = securityConfig.authenticationManager(configuration);

    assertThat(result).isSameAs(manager);
  }

  @Test
  void authenticationManagerWrapsInitializationFailure() {
    assertThatThrownBy(() -> securityConfig.authenticationManager(null))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("Nu am putut inițializa AuthenticationManager.");
  }

  @Test
  void corsConfigurationSourceContainsExpectedSettings() {
    CorsConfigurationSource source = securityConfig.corsConfigurationSource();
    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/employees");

    CorsConfiguration config = source.getCorsConfiguration(request);

    assertThat(config).isNotNull();
    assertThat(config.getAllowedOrigins()).contains("http://localhost:4200");
    assertThat(config.getAllowedMethods()).contains("GET", "POST", "PUT", "DELETE", "OPTIONS");
    assertThat(config.getAllowedHeaders()).contains("Authorization", "Content-Type");
  }
}
