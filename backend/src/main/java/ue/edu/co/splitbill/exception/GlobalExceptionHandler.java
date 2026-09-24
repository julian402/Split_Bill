package ue.edu.co.splitbill.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Traduce las excepciones a respuestas HTTP con el formato estandar ProblemDetail (RFC 9457):
 *
 * { "status": 400, "title": "Bad Request", "detail": "La descripcion del gasto es obligatoria" }
 *
 * Asi ningun controlador tiene try/catch, y la app recibe siempre errores con la misma forma. Al
 * heredar de ResponseEntityExceptionHandler, los errores propios de Spring (JSON mal formado, metodo
 * no permitido, etc.) tambien salen en este formato.
 */
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger LOG = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(NotFoundException.class)
    public ProblemDetail handleNotFound(NotFoundException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, e.getMessage());
    }

    @ExceptionHandler(ForbiddenException.class)
    public ProblemDetail handleForbidden(ForbiddenException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, e.getMessage());
    }

    @ExceptionHandler(ConflictException.class)
    public ProblemDetail handleConflict(ConflictException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, e.getMessage());
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    public ProblemDetail handleInvalidCredentials(InvalidCredentialsException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, e.getMessage());
    }

    /** Las reglas de negocio de los metodos validar() lanzan IllegalArgumentException. */
    @ExceptionHandler(IllegalArgumentException.class)
    public ProblemDetail handleBusinessRule(IllegalArgumentException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, e.getMessage());
    }

    /** Ultima defensa: una restriccion de la base de datos (UNIQUE, FK, CHECK) rechazo la operacion. */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ProblemDetail handleDataIntegrity(DataIntegrityViolationException e) {
        LOG.warn("ERROR AL GUARDAR: restriccion de la base de datos", e);
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT,
                "La operacion entra en conflicto con los datos existentes");
    }

    /** Cualquier otro error: se registra completo en el log y al cliente no se le muestra el detalle. */
    @ExceptionHandler(Exception.class)
    public ProblemDetail handleUnexpected(Exception e) {
        LOG.error("ERROR INESPERADO", e);
        return ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR,
                "Ocurrio un error inesperado en el servidor");
    }

    /**
     * Errores de @Valid en los DTO: ademas del mensaje general, se devuelve cada campo con su error
     * para que la app pueda marcarlo en el formulario.
     */
    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
            HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        Map<String, String> errors = new LinkedHashMap<>();
        for (FieldError fieldError : ex.getBindingResult().getFieldErrors()) {
            errors.putIfAbsent(fieldError.getField(), fieldError.getDefaultMessage());
        }
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST,
                "Hay campos invalidos en la solicitud");
        problem.setProperty("errors", errors);
        return ResponseEntity.badRequest().body(problem);
    }
}
