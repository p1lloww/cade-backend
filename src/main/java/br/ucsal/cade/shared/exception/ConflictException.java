package br.ucsal.cade.shared.exception;

import org.springframework.http.HttpStatus;

/** A requisição não combina com a situação atual do registro (HTTP 409), por exemplo uma transição proibida. */
public class ConflictException extends ApiException {

    public ConflictException(String userMessage) {
        super(userMessage);
    }

    @Override
    public HttpStatus status() {
        return HttpStatus.CONFLICT;
    }
}
