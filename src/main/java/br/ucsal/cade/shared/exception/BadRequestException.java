package br.ucsal.cade.shared.exception;

import org.springframework.http.HttpStatus;

/** A requisição viola uma regra de negócio (HTTP 400). */
public class BadRequestException extends ApiException {

    public BadRequestException(String userMessage) {
        super(userMessage);
    }

    @Override
    public HttpStatus status() {
        return HttpStatus.BAD_REQUEST;
    }
}
