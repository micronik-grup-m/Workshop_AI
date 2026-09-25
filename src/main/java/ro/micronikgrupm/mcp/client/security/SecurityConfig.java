package ro.micronikgrupm.mcp.client.security;

import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import ro.micronikgrupm.mcp.client.common.ApiError;
import tools.jackson.databind.ObjectMapper;

/** Auto-generated documentation. */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

  private static final String AUTHORIZATION_HEADER = "Authorization";
  private static final String BEARER_PREFIX = "Bearer ";
  private static final String ROLE_ADMIN = "ADMIN";
  private final JwtAuthenticationFilter jwtAuthenticationFilter;
  private final String apiPathPrefix;
  private final ObjectMapper objectMapper = new ObjectMapper();

  public SecurityConfig(
      JwtAuthenticationFilter jwtAuthenticationFilter,
      @Value("${security.api.path-prefix:/api/}") String apiPathPrefix) {
    this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    this.apiPathPrefix = apiPathPrefix;
  }

  /** Provides AuthenticationManager from Spring Security configuration. */
  @Bean
  public AuthenticationManager authenticationManager(AuthenticationConfiguration config) {
    try {
      return config.getAuthenticationManager();
    } catch (Exception ex) {
      throw new IllegalStateException("Nu am putut inițializa AuthenticationManager.", ex);
    }
  }

  /** Auto-generated documentation. */
  @Bean
  public CorsConfigurationSource corsConfigurationSource() {
    CorsConfiguration configuration = new CorsConfiguration();
    configuration.setAllowedOrigins(List.of("http://localhost:4200"));
    configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
    configuration.setAllowedHeaders(List.of(AUTHORIZATION_HEADER, "Content-Type"));
    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", configuration);
    return source;
  }

  /** Auto-generated documentation. */
  @Bean
  public SecurityFilterChain securityFilterChain(HttpSecurity http) {
    try {
      http.cors(cors -> cors.configurationSource(corsConfigurationSource()))
          .csrf(
              csrf ->
                  csrf.ignoringRequestMatchers("/api/auth/login", "/mcp/**", "/actuator/**")
                      .ignoringRequestMatchers(this::isStatelessApiRequest))
          .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
          .authorizeHttpRequests(
              auth ->
                  auth.dispatcherTypeMatchers(jakarta.servlet.DispatcherType.ERROR)
                      .permitAll()
                      .requestMatchers("/api/auth/**")
                      .permitAll()
                      .requestMatchers("/mcp/**")
                      .permitAll()
                      .requestMatchers("/actuator/health", "/actuator/health/**", "/actuator/info")
                      .permitAll()
                      .requestMatchers("/actuator/**")
                      .hasRole(ROLE_ADMIN)
                      .requestMatchers(HttpMethod.GET, "/api/employees/**")
                      .hasAnyRole(ROLE_ADMIN, "USER")
                      .requestMatchers("/api/employees/**")
                      .hasRole(ROLE_ADMIN)
                      .requestMatchers("/api/documents/**")
                      .hasRole(ROLE_ADMIN)
                      .anyRequest()
                      .authenticated())
          .exceptionHandling(
              eh ->
                  eh.authenticationEntryPoint(
                          (request, response, ex) ->
                              writeError(response, 401, "UNAUTHORIZED", "Autentificare necesară"))
                      .accessDeniedHandler(
                          (request, response, ex) ->
                              writeError(response, 403, "FORBIDDEN", "Acces interzis")))
          .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
      return http.build();
    } catch (Exception ex) {
      throw new IllegalStateException("Nu am putut construi SecurityFilterChain.", ex);
    }
  }

  private void writeError(
      jakarta.servlet.http.HttpServletResponse response, int status, String error, String message)
      throws java.io.IOException {
    response.setStatus(status);
    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
    response.getWriter().write(objectMapper.writeValueAsString(new ApiError(error, message)));
  }

  private boolean isStatelessApiRequest(jakarta.servlet.http.HttpServletRequest request) {
    String authorization = request.getHeader(AUTHORIZATION_HEADER);
    return request.getRequestURI().startsWith(apiPathPrefix)
        && authorization != null
        && authorization.startsWith(BEARER_PREFIX);
  }
}
