package dev.ledgerflow.web;
import dev.ledgerflow.repo.Repositories.*; import dev.ledgerflow.service.TransferService;
import org.springframework.data.domain.PageRequest; import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller; import org.springframework.ui.Model; import org.springframework.web.bind.annotation.*; import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.util.UUID;
@Controller
public class WebController {
  private final AccountRepo accounts; private final LedgerRepo ledger; private final TransferService svc;
  public WebController(AccountRepo a,LedgerRepo l,TransferService s){accounts=a;ledger=l;svc=s;}
  @GetMapping("/login") public String login(){ return "login"; }
  @GetMapping("/") public String home(Authentication auth,Model m){
    m.addAttribute("accounts",accounts.findByOwnerUsername(auth.getName())); return "home";
  }
  @GetMapping("/accounts/{id}") public String statement(@PathVariable Long id,Authentication auth,Model m){
    var a=accounts.findByIdAndOwnerUsername(id,auth.getName()).orElseThrow(()->new TransferService.ApiException(403,"not your account"));
    m.addAttribute("account",a);
    m.addAttribute("entries",ledger.findByAccountIdOrderByCreatedAtDesc(id,PageRequest.of(0,50)));
    return "statement";
  }
  @PostMapping("/transfers") public String transfer(@RequestParam Long sourceAccountId,@RequestParam Long destinationAccountId,
      @RequestParam long amountMinor,Authentication auth,RedirectAttributes ra){
    try{ var rec=svc.transfer(auth.getName(),UUID.randomUUID().toString(),sourceAccountId,destinationAccountId,amountMinor,"USD");
      ra.addFlashAttribute("message","Transfer #"+rec.getId()+" committed: "+amountMinor+" minor units USD");
    }catch(TransferService.ApiException e){ ra.addFlashAttribute("error",e.getMessage()); }
    return "redirect:/";
  }
}
