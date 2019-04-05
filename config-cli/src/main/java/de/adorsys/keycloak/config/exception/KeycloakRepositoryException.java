package de.adorsys.keycloak.config.exception;

public class KeycloakRepositoryException extends RuntimeException {
    public KeycloakRepositoryException(String message) {
        super(message);
    }

    public KeycloakRepositoryException(String message, Throwable cause) {
        super(message, cause);
    }
}
