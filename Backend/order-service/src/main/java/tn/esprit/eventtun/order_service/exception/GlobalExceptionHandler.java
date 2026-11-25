package tn.esprit.eventtun.order_service.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.net.URI;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Toutes les erreurs suivent le format Problem Details (application/problem+json),
 * sans stack trace ni message SQL.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    // 404 : commande introuvable
    @ExceptionHandler(OrderNotFoundException.class)
    public ResponseEntity<ProblemDetail> handleNotFound(OrderNotFoundException ex, HttpServletRequest request) {
        ProblemDetail problem = build(HttpStatus.NOT_FOUND,
            "Commande introuvable : " + ex.getOrderId(), request);
        return respond(problem);
    }

    // 409 : achat refuse par la Saga (plus de places, paiement refuse, utilisateur bloque...)
    @ExceptionHandler(OrderRejectedException.class)
    public ResponseEntity<ProblemDetail> handleRejected(OrderRejectedException ex, HttpServletRequest request) {
        ProblemDetail problem = build(HttpStatus.CONFLICT,
            "L'achat a ete refuse (plus de places, paiement refuse ou utilisateur non autorise)", request);
        problem.setProperty("orderId", ex.getOrderId());
        problem.setProperty("orderStatus", "REJECTED");
        return respond(problem);
    }

    // 503 : une compensation reste a resoudre
    @ExceptionHandler(OrderCompensationPendingException.class)
    public ResponseEntity<ProblemDetail> handleCompensation(OrderCompensationPendingException ex,
                                                            HttpServletRequest request) {
        ProblemDetail problem = build(HttpStatus.SERVICE_UNAVAILABLE,
            "Une compensation (remboursement ou liberation des places) reste a resoudre", request);
        problem.setProperty("orderId", ex.getOrderId());
        problem.setProperty("orderStatus", "COMPENSATION_PENDING");
        return respond(problem);
    }

    // 409 : action impossible dans l'etat actuel (ex. annuler une commande confirmee)
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ProblemDetail> handleIllegalState(IllegalStateException ex, HttpServletRequest request) {
        return respond(build(HttpStatus.CONFLICT, ex.getMessage(), request));
    }

    // 400 : parametre invalide
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ProblemDetail> handleIllegalArgument(IllegalArgumentException ex,
                                                               HttpServletRequest request) {
        return respond(build(HttpStatus.BAD_REQUEST, ex.getMessage(), request));
    }

    // 400 : validation du corps JSON (@Valid)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ProblemDetail> handleValidation(MethodArgumentNotValidException ex,
                                                          HttpServletRequest request) {
        Map<String, String> errors = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors()
            .forEach(error -> errors.put(error.getField(), error.getDefaultMessage()));

        ProblemDetail problem = build(HttpStatus.BAD_REQUEST, "Requete invalide", request);
        problem.setProperty("errors", errors);
        return respond(problem);
    }

    // 400 : JSON mal forme ou valeur d'enumeration inconnue dans le corps
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ProblemDetail> handleUnreadable(HttpMessageNotReadableException ex,
                                                          HttpServletRequest request) {
        return respond(build(HttpStatus.BAD_REQUEST, "Corps de la requete illisible ou invalide", request));
    }

    // 400 : mauvais type de parametre (ex. status=INCONNU)
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ProblemDetail> handleTypeMismatch(MethodArgumentTypeMismatchException ex,
                                                            HttpServletRequest request) {
        return respond(build(HttpStatus.BAD_REQUEST,
            "Valeur invalide pour le parametre '" + ex.getName() + "'", request));
    }

    // Reste : 404 pour l'ancien message "Order not found", sinon 500 sans detail technique
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ProblemDetail> handleRuntime(RuntimeException ex, HttpServletRequest request) {
        if ("Order not found".equals(ex.getMessage())) {
            return respond(build(HttpStatus.NOT_FOUND, "Commande introuvable", request));
        }
        log.error("Erreur interne sur {}", request.getRequestURI(), ex);
        return respond(build(HttpStatus.INTERNAL_SERVER_ERROR, "Erreur interne du serveur", request));
    }

    private ProblemDetail build(HttpStatus status, String detail, HttpServletRequest request) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setTitle(status.getReasonPhrase());
        problem.setInstance(URI.create(request.getRequestURI()));
        return problem;
    }

    private ResponseEntity<ProblemDetail> respond(ProblemDetail problem) {
        return ResponseEntity.status(problem.getStatus())
            .contentType(MediaType.APPLICATION_PROBLEM_JSON)
            .body(problem);
    }
}
