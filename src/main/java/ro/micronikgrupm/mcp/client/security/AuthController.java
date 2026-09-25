package ro.micronikgrupm.mcp.client.security;

import jakarta.validation.Valid;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Auto-generated documentation. */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

  private final AuthenticationManager authenticationManager;
  private final JwtService jwtService;
  private final AppUserRepository userRepository;

  /** Handles AuthController operation. */
  public AuthController(
      AuthenticationManager authenticationManager,
      JwtService jwtService,
      AppUserRepository userRepository) {
    this.authenticationManager = authenticationManager;
    this.jwtService = jwtService;
    this.userRepository = userRepository;
  }

  /** Handles PostMapping operation. */
  @PostMapping("/login")
  public LoginResponse login(@Valid @RequestBody LoginRequest request) {
    authenticationManager.authenticate(
        new UsernamePasswordAuthenticationToken(request.username(), request.password()));
    AppUser user = userRepository.findByUsername(request.username()).orElseThrow();
    String token = jwtService.generateToken(user.getUsername(), user.getRole().name());
    return new LoginResponse(token);
  }
}
