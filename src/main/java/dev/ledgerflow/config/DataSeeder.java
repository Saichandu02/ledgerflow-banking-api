package dev.ledgerflow.config;
import dev.ledgerflow.domain.*; import dev.ledgerflow.repo.Repositories.*;
import org.slf4j.*; import org.springframework.boot.CommandLineRunner; import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder; import org.springframework.stereotype.Component; import org.springframework.transaction.annotation.Transactional;
/** Local/demo seed ONLY: two fictional users. Passwords are documented demo credentials, never for production. */
@Component @Profile("!test")
public class DataSeeder implements CommandLineRunner {
  private static final Logger log=LoggerFactory.getLogger(DataSeeder.class);
  private final UserRepo users; private final AccountRepo accounts; private final LedgerRepo ledger; private final PasswordEncoder enc;
  public DataSeeder(UserRepo u,AccountRepo a,LedgerRepo l,PasswordEncoder e){users=u;accounts=a;ledger=l;enc=e;}
  @Override @Transactional public void run(String... args){
    if (users.count()>0) return;
    var alice=users.save(new AppUser("alice",enc.encode("alice-demo-123"),"USER"));
    var bob=users.save(new AppUser("bob",enc.encode("bob-demo-123"),"USER"));
    var a1=accounts.save(new BankAccount(alice,"Alice Checking","USD"));
    var a2=accounts.save(new BankAccount(bob,"Bob Checking","USD"));
    fund(a1,100_000); fund(a2,50_000);
    log.info("Seeded demo users alice/bob with funded fictional accounts {} and {}",a1.getId(),a2.getId());
  }
  private void fund(BankAccount a,long amt){ a.applyDelta(amt); accounts.save(a); ledger.save(new LedgerEntry(null,a,amt,"OPENING")); }
}
