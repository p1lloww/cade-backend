package br.ucsal.cade.shared.exception;

import org.springframework.http.HttpStatus;

/**
 * Business error that reaches the user. The message is shown in the front end, so it is written in Portuguese;
 * each subclass fixes the HTTP status. A single handler turns any subclass into an error response, so a new kind of
 * error needs no change in the handler.
 */
public abstract class ApiException extends RuntimeException {

    protected ApiException(String userMessage) {
        super(userMessage);
    }

    public abstract HttpStatus status();
}
