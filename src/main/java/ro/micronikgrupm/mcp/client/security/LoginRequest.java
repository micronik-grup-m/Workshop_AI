package ro.micronikgrupm.mcp.client.security;

import jakarta.validation.constraints.NotBlank;

/** LoginRequest component. */
public record LoginRequest(@NotBlank String username, @NotBlank String password) {}
