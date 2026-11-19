package br.ucsal.cade.shared.web.error;

import br.ucsal.cade.shared.exception.ApiException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.TypeMismatchException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Transforma os erros em problem details da RFC 9457 ({@code application/problem+json}), para o front-end ler todo
 * erro do mesmo jeito: {@code detail} traz a mensagem a mostrar e, nos erros de validação, {@code errors} liga cada
 * campo à sua mensagem.
 *
 * <p>Além dos erros de negócio, responde também os 401 e 403 do Spring Security (encaminhados para cá pelo
 * {@code SecurityConfiguration}) e qualquer erro inesperado, que vira um 500 com mensagem neutra: os detalhes vão só
 * para o log, nunca para o usuário.
 */
@RestControllerAdvice
public class ApiExceptionHandler extends ResponseEntityExceptionHandler {

    static final String INVALID_FIELDS_MESSAGE = "Confira os campos enviados.";
    static final String UNREADABLE_BODY_MESSAGE = "O corpo da requisição está ausente ou mal formatado.";
    static final String UNAUTHORIZED_MESSAGE = "Entre com o seu e-mail institucional para continuar.";
    static final String FORBIDDEN_MESSAGE = "Você não tem permissão para fazer isso.";
    static final String UNEXPECTED_MESSAGE = "Algo deu errado do nosso lado. Tente de novo em instantes.";
    static final String INVALID_VALUE_MESSAGE = "Valor inválido.";
    static final String TOO_LARGE_MESSAGE = "O arquivo enviado é grande demais.";

    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

    @ExceptionHandler(ApiException.class)
    ResponseEntity<ProblemDetail> handleApiException(ApiException exception) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(exception.status(), exception.getMessage());
        return ResponseEntity.status(exception.status()).headers(exception.headers()).body(problem);
    }

    /**
     * Token ausente, vencido ou inválido. O cabeçalho {@code WWW-Authenticate} já foi definido pelo entry point do
     * bearer, então aqui só entra o corpo.
     */
    @ExceptionHandler(AuthenticationException.class)
    ResponseEntity<ProblemDetail> handleAuthentication(AuthenticationException exception) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, UNAUTHORIZED_MESSAGE));
    }

    /** Token válido, mas o perfil não pode fazer isso; também lançado pelas checagens de perfil nos controllers. */
    @ExceptionHandler(AccessDeniedException.class)
    ResponseEntity<ProblemDetail> handleAccessDenied(AccessDeniedException exception) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, FORBIDDEN_MESSAGE));
    }

    /** Tudo o que nenhum outro tratador conhece: um bug ou um serviço fora do ar. Os erros do Spring mantêm os tratadores acima. */
    @ExceptionHandler(Exception.class)
    ResponseEntity<ProblemDetail> handleUnexpected(Exception exception) {
        log.error("Unexpected error", exception);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR, UNEXPECTED_MESSAGE));
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException exception,
                                                                  HttpHeaders headers, HttpStatusCode status,
                                                                  WebRequest request) {
        Map<String, String> errors = new LinkedHashMap<>();
        for (FieldError error : exception.getBindingResult().getFieldErrors()) {
            // Falha de conversão (data ou id mal formatados) traz uma mensagem técnica em inglês do Spring.
            errors.putIfAbsent(error.getField(),
                    error.isBindingFailure() ? INVALID_VALUE_MESSAGE : error.getDefaultMessage());
        }
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, INVALID_FIELDS_MESSAGE);
        problem.setProperty("errors", errors);
        return handleExceptionInternal(exception, problem, headers, HttpStatus.BAD_REQUEST, request);
    }

    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(HttpMessageNotReadableException exception,
                                                                  HttpHeaders headers, HttpStatusCode status,
                                                                  WebRequest request) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, UNREADABLE_BODY_MESSAGE);
        return handleExceptionInternal(exception, problem, headers, HttpStatus.BAD_REQUEST, request);
    }

    /** Parâmetro de consulta com valor que não converte, como uma data mal formatada. */
    @Override
    protected ResponseEntity<Object> handleTypeMismatch(TypeMismatchException exception, HttpHeaders headers,
                                                        HttpStatusCode status, WebRequest request) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, INVALID_FIELDS_MESSAGE);
        return handleExceptionInternal(exception, problem, headers, HttpStatus.BAD_REQUEST, request);
    }

    @Override
    protected ResponseEntity<Object> handleMissingServletRequestParameter(
            MissingServletRequestParameterException exception, HttpHeaders headers, HttpStatusCode status,
            WebRequest request) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, INVALID_FIELDS_MESSAGE);
        return handleExceptionInternal(exception, problem, headers, HttpStatus.BAD_REQUEST, request);
    }

    /** Upload acima do limite do multipart, recusado antes de chegar ao controller. */
    @Override
    protected ResponseEntity<Object> handleMaxUploadSizeExceededException(MaxUploadSizeExceededException exception,
                                                                          HttpHeaders headers, HttpStatusCode status,
                                                                          WebRequest request) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONTENT_TOO_LARGE, TOO_LARGE_MESSAGE);
        return handleExceptionInternal(exception, problem, headers, HttpStatus.CONTENT_TOO_LARGE, request);
    }
}
