package dev.ledgerflow.web;
import dev.ledgerflow.domain.*; import dev.ledgerflow.repo.Repositories.*; import dev.ledgerflow.service.TransferService;
import jakarta.validation.*; import jakarta.validation.constraints.*;
import org.springframework.data.domain.PageRequest; import org.springframework.http.*; import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*; import java.util.*;
@RestController @RequestMapping("/api")
public class ApiController {
  private final AccountRepo accounts; private final LedgerRepo ledger; private final TransferService svc;
  public ApiController(AccountRepo a,LedgerRepo l,TransferService s){accounts=a;ledger=l;svc=s;}
  public record TransferRequest(@NotNull Long sourceAccountId,@NotNull Long destinationAccountId,@Positive long amountMinor,@NotBlank String currency){}
  public record AccountView(Long id,String name,String currency,long balanceMinor){}
  public record TransferView(Long id,String status,long amountMinor,String currency,Long sourceAccountId,Long destinationAccountId){}
  public record EntryView(Long id,Long transferId,long amountMinor,String entryType,String createdAt){}
  @GetMapping("/accounts") public List<AccountView> myAccounts(Authentication auth){
    return accounts.findByOwnerUsername(auth.getName()).stream().map(a->new AccountView(a.getId(),a.getName(),a.getCurrency(),a.getBalanceMinor())).toList();
  }
  @GetMapping("/accounts/{id}/statement") public ResponseEntity<?> statement(@PathVariable Long id,@RequestParam(defaultValue="0") int page,Authentication auth){
    if (accounts.findByIdAndOwnerUsername(id,auth.getName()).isEmpty()) return ResponseEntity.status(403).body(Map.of("error","account not owned by user or not found"));
    var p=ledger.findByAccountIdOrderByCreatedAtDesc(id,PageRequest.of(page,20));
    return ResponseEntity.ok(p.map(e->new EntryView(e.getId(),e.getTransfer()==null?null:e.getTransfer().getId(),e.getAmountMinor(),e.getEntryType(),e.getCreatedAt().toString())));
  }
  @PostMapping("/transfers") public ResponseEntity<?> transfer(@RequestHeader(value="Idempotency-Key",required=false) String key,@Valid @RequestBody TransferRequest r,Authentication auth){
    try{ var rec=svc.transfer(auth.getName(),key,r.sourceAccountId(),r.destinationAccountId(),r.amountMinor(),r.currency());
      return ResponseEntity.status(HttpStatus.CREATED).body(new TransferView(rec.getId(),rec.getStatus().name(),rec.getAmountMinor(),rec.getCurrency(),rec.getSource().getId(),rec.getDestination().getId()));
    }catch(TransferService.ApiException e){ return ResponseEntity.status(e.status).body(Map.of("error",e.getMessage())); }
  }
}
