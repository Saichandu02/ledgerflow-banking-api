package dev.ledgerflow.web;
import dev.ledgerflow.service.TransferService;
import org.springframework.http.*; import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*; import java.util.Map;
@RestControllerAdvice
public class ApiErrorHandler {
  @ExceptionHandler(TransferService.ApiException.class)
  public ResponseEntity<?> api(TransferService.ApiException e){
    return ResponseEntity.status(e.status).body(Map.of("error",e.getMessage()));
  }
  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<?> invalid(MethodArgumentNotValidException e){
    var msg=e.getBindingResult().getFieldErrors().stream().findFirst().map(f->f.getField()+": "+f.getDefaultMessage()).orElse("invalid request");
    return ResponseEntity.badRequest().body(Map.of("error",msg));
  }
}
