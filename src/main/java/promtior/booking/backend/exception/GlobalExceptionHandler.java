package promtior.booking.backend.exception;

import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;

import com.openai.errors.OpenAIException;
import com.openai.errors.OpenAIIoException;
import com.openai.errors.RateLimitException;


@RestControllerAdvice
public class GlobalExceptionHandler {

   private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

   @ExceptionHandler(ResourceNotFoundException.class)
   public ProblemDetail handleResourceNotFound(ResourceNotFoundException ex) {
      return build(HttpStatus.NOT_FOUND, ex.getMessage());
   }

   @ExceptionHandler(BusinessException.class)
   public ProblemDetail handleBusiness(BusinessException ex) {
      return build(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage());
   }


   @ExceptionHandler(AuthenticationException.class)
   public ProblemDetail handleAuthentication(AuthenticationException ex) {
      return build(HttpStatus.UNAUTHORIZED, "Usuario o password inválidos");
   }

   @ExceptionHandler(MethodArgumentNotValidException.class)
   public ProblemDetail handleValidation(MethodArgumentNotValidException ex) {
      String fields = ex.getBindingResult().getFieldErrors().stream()
            .map(fe -> fe.getField() + ": " + (fe.getDefaultMessage() != null ? fe.getDefaultMessage() : "invalid"))
            .collect(Collectors.joining(", "));
      return build(HttpStatus.BAD_REQUEST, fields);
   }

   @ExceptionHandler(HandlerMethodValidationException.class)
   public ProblemDetail handleHandlerMethodValidation(HandlerMethodValidationException ex) {
      String fields = ex.getAllErrors().stream()
            .map(err -> err.getDefaultMessage())
            .filter(msg -> msg != null)
            .collect(Collectors.joining(", "));
      return build(HttpStatus.BAD_REQUEST, fields);
   }


   @ExceptionHandler(RateLimitException.class)
   public ProblemDetail handleOpenAiRateLimit(RateLimitException ex) {
      log.warn("OpenAI rate limit: {}", ex.getMessage());
      return build(HttpStatus.SERVICE_UNAVAILABLE,
            "El asistente está recibiendo muchas consultas en este momento. Esperá unos segundos y volvé a intentar.");
   }


   @ExceptionHandler(OpenAIIoException.class)
   public ProblemDetail handleOpenAiIo(OpenAIIoException ex) {
      log.warn("OpenAI I/O error: {}", ex.getMessage());
      return build(HttpStatus.GATEWAY_TIMEOUT,
            "El asistente tardó demasiado en responder. Intentá de nuevo en unos segundos.");
   }

   @ExceptionHandler(OpenAIException.class)
   public ProblemDetail handleOpenAi(OpenAIException ex) {
      log.error("OpenAI error", ex);
      return build(HttpStatus.SERVICE_UNAVAILABLE,
            "El asistente no está disponible en este momento. Intentá de nuevo más tarde.");
   }

   @ExceptionHandler(Exception.class)
   public ProblemDetail handleGeneric(Exception ex) {
      log.error("Unhandled exception", ex);
      return build(HttpStatus.INTERNAL_SERVER_ERROR, "Internal server error");
   }

   private ProblemDetail build(HttpStatus status, String message) {
      return ProblemDetail.forStatusAndDetail(status, message);
   }
}
