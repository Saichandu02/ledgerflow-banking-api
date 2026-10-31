package dev.ledgerflow;
import dev.ledgerflow.domain.*; import dev.ledgerflow.repo.Repositories.*; import dev.ledgerflow.service.TransferService;
import org.junit.jupiter.api.*; import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest; import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.support.TransactionTemplate; import org.springframework.transaction.PlatformTransactionManager;
import java.util.concurrent.*; import static org.junit.jupiter.api.Assertions.*;
/** Integration tests against a REAL local PostgreSQL (profile "test", db ledgerflow_test).
 *  In CI / Docker environments the same suite can be pointed at Testcontainers via TEST_DB_URL.
 *  Requires the test DB to exist; setUp truncates all tables. */
@SpringBootTest @ActiveProfiles("test")
class TransferIntegrationTest {
  @Autowired TransferService svc; @Autowired UserRepo users; @Autowired AccountRepo accounts;
  @Autowired TransferRepo transfers; @Autowired LedgerRepo ledger; @Autowired PlatformTransactionManager txm;
  BankAccount aliceAcc, bobAcc;
  @BeforeEach void setUp(){
    ledger.deleteAll(); transfers.deleteAll(); accounts.deleteAll(); users.deleteAll();
    var alice=users.save(new AppUser("alice","x","USER")); var bob=users.save(new AppUser("bob","x","USER"));
    aliceAcc=accounts.save(new BankAccount(alice,"Alice Checking","USD"));
    bobAcc=accounts.save(new BankAccount(bob,"Bob Checking","USD"));
    fund(aliceAcc,100_000); fund(bobAcc,0);
  }
  private void fund(BankAccount a,long amt){ new TransactionTemplate(txm).executeWithoutResult(s->{
    var acc=accounts.findById(a.getId()).orElseThrow(); acc.applyDelta(amt); accounts.save(acc);
    ledger.save(new LedgerEntry(null,acc,amt,"OPENING")); }); }
  private long balance(Long id){ return accounts.findById(id).orElseThrow().getBalanceMinor(); }

  @Test void transferMovesMoneyAndWritesBalancedLedger(){
    var rec=svc.transfer("alice","k-1",aliceAcc.getId(),bobAcc.getId(),2500,"USD");
    assertEquals(97_500,balance(aliceAcc.getId())); assertEquals(2_500,balance(bobAcc.getId()));
    assertEquals(0, ledger.findAll().stream().filter(e->e.getTransfer()!=null&&e.getTransfer().getId().equals(rec.getId())).mapToLong(LedgerEntry::getAmountMinor).sum());
    assertTrue(svc.reconciles(aliceAcc.getId())); assertTrue(svc.reconciles(bobAcc.getId()));
  }
  @Test void idempotencyRetryReturnsOriginalAndMovesMoneyOnce(){
    var first=svc.transfer("alice","retry-1",aliceAcc.getId(),bobAcc.getId(),2500,"USD");
    var second=svc.transfer("alice","retry-1",aliceAcc.getId(),bobAcc.getId(),2500,"USD");
    assertEquals(first.getId(),second.getId());
    assertEquals(97_500,balance(aliceAcc.getId())); assertEquals(1,transfers.count());
  }
  @Test void sameKeyDifferentPayloadIsRejected(){
    svc.transfer("alice","k-conflict",aliceAcc.getId(),bobAcc.getId(),2500,"USD");
    var e=assertThrows(TransferService.ApiException.class,()->svc.transfer("alice","k-conflict",aliceAcc.getId(),bobAcc.getId(),9999,"USD"));
    assertEquals(409,e.status); assertEquals(97_500,balance(aliceAcc.getId()));
  }
  @Test void insufficientFundsRejectedAndNothingMoves(){
    var e=assertThrows(TransferService.ApiException.class,()->svc.transfer("alice","k-poor",aliceAcc.getId(),bobAcc.getId(),100_001,"USD"));
    assertEquals(422,e.status); assertEquals(100_000,balance(aliceAcc.getId())); assertEquals(0,transfers.count());
  }
  @Test void cannotDebitAnotherUsersAccount(){
    var e=assertThrows(TransferService.ApiException.class,()->svc.transfer("bob","k-theft",aliceAcc.getId(),bobAcc.getId(),100,"USD"));
    assertEquals(403,e.status); assertEquals(100_000,balance(aliceAcc.getId()));
  }
  @Test void sameAccountAndInvalidAmountRejected(){
    assertThrows(TransferService.ApiException.class,()->svc.transfer("alice","k-same",aliceAcc.getId(),aliceAcc.getId(),100,"USD"));
    assertThrows(TransferService.ApiException.class,()->svc.transfer("alice","k-zero",aliceAcc.getId(),bobAcc.getId(),0,"USD"));
  }
  @Test void concurrentTransfersExceedingBalanceNeverGoNegative() throws Exception {
    int threads=10; long amt=20_000; // 10 x 20_000 = 200_000 > 100_000 balance; at most 5 may succeed
    var pool=Executors.newFixedThreadPool(threads); var latch=new CountDownLatch(threads);
    var successes=new java.util.concurrent.atomic.AtomicInteger(); var failures=new java.util.concurrent.atomic.AtomicInteger();
    for(int i=0;i<threads;i++){ final int n=i; pool.submit(()->{ latch.countDown();
      try{ svc.transfer("alice","conc-"+n,aliceAcc.getId(),bobAcc.getId(),amt,"USD"); successes.incrementAndGet(); }
      catch(Exception e){ failures.incrementAndGet(); } }); }
    pool.shutdown(); assertTrue(pool.awaitTermination(60,TimeUnit.SECONDS));
    assertEquals(5,successes.get(), "exactly five 20k transfers fit in a 100k balance");
    assertEquals(0,balance(aliceAcc.getId())); assertEquals(100_000,balance(bobAcc.getId()));
    assertTrue(svc.reconciles(aliceAcc.getId())); assertTrue(svc.reconciles(bobAcc.getId()));
  }
}
