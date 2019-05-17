package de.adorsys.keycloak.config.exception;

public class InvalidImportException extends RuntimeException {
    public InvalidImportException(String message) {
        super(message);
    }

    public InvalidImportException(Throwable cause) {
        super(cause);
    }
}
