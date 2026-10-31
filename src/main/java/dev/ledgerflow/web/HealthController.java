package dev.ledgerflow.web;
import org.springframework.http.*; import org.springframework.web.bind.annotation.*; import javax.sql.DataSource; import java.util.Map;
@RestController
public class HealthController {
  private final DataSource ds; public HealthController(DataSource ds){this.ds=ds;}
  @GetMapping("/healthz") public Map<String,String> health(){ return Map.of("status","UP"); }
  @GetMapping("/readyz") public ResponseEntity<?> ready(){
    try(var c=ds.getConnection()){ if(c.isValid(2)) return ResponseEntity.ok(Map.of("status","READY")); }
    catch(Exception e){ return ResponseEntity.status(503).body(Map.of("status","NOT_READY","error",e.getClass().getSimpleName())); }
    return ResponseEntity.status(503).body(Map.of("status","NOT_READY"));
  }
}
