package dev.ledgerflow.service;
import dev.ledgerflow.domain.*; import dev.ledgerflow.repo.Repositories.*;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service; import org.springframework.transaction.annotation.Transactional;
import java.nio.charset.StandardCharsets; import java.security.MessageDigest; import java.util.HexFormat;
@Service
public class TransferService {
  public static class ApiException extends RuntimeException {
    public final int status; public ApiException(int s,String m){super(m);status=s;}
  }
  private final AccountRepo accounts; private final TransferRepo transfers; private final LedgerRepo ledger;
  public TransferService(AccountRepo a,TransferRepo t,LedgerRepo l){accounts=a;transfers=t;ledger=l;}

  public static String requestHash(long src,long dst,long amt,String cur){
    try{ var md=MessageDigest.getInstance("SHA-256");
      return HexFormat.of().formatHex(md.digest((src+"|"+dst+"|"+amt+"|"+cur).getBytes(StandardCharsets.UTF_8)));
    }catch(Exception e){ throw new IllegalStateException(e); }
  }

  @Transactional
  public TransferRecord transfer(String username, String idempotencyKey, long srcId, long dstId, long amountMinor, String currency){
    if (idempotencyKey==null||idempotencyKey.isBlank()) throw new ApiException(400,"Idempotency-Key header is required");
    if (amountMinor<=0) throw new ApiException(400,"amountMinor must be positive");
    if (srcId==dstId) throw new ApiException(400,"source and destination must differ");
    String hash=requestHash(srcId,dstId,amountMinor,currency);
    var existing=transfers.findByIdempotencyKey(idempotencyKey);
    if (existing.isPresent()){
      if (!existing.get().getRequestHash().equals(hash)) throw new ApiException(409,"Idempotency-Key already used with a different payload");
      return existing.get();
    }
    // Lock both accounts in consistent ID order to prevent deadlocks.
    long first=Math.min(srcId,dstId), second=Math.max(srcId,dstId);
    var a1=accounts.findByIdForUpdate(first).orElseThrow(()->new ApiException(404,"account not found: "+first));
    var a2=accounts.findByIdForUpdate(second).orElseThrow(()->new ApiException(404,"account not found: "+second));
    BankAccount src = srcId==first?a1:a2, dst = dstId==first?a1:a2;
    if (!src.getOwner().getUsername().equals(username)) throw new ApiException(403,"you do not own the source account");
    if (!src.getCurrency().equals(currency)||!dst.getCurrency().equals(currency)) throw new ApiException(400,"currency mismatch");
    if (src.getBalanceMinor()<amountMinor) throw new ApiException(422,"insufficient funds");
    src.applyDelta(-amountMinor); dst.applyDelta(amountMinor);
    TransferRecord rec;
    try { rec=transfers.saveAndFlush(new TransferRecord(idempotencyKey,hash,src,dst,amountMinor,currency)); }
    catch (DataIntegrityViolationException e){ // concurrent duplicate key: caller should retry and will read the original
      throw new ApiException(409,"duplicate idempotency key submitted concurrently; retry the identical request");
    }
    ledger.save(new LedgerEntry(rec,src,-amountMinor,"DEBIT"));
    ledger.save(new LedgerEntry(rec,dst,amountMinor,"CREDIT"));
    // Reconciliation invariant: cached balances must equal ledger sums after this transaction commits.
    return rec;
  }
  @Transactional(readOnly=true)
  public boolean reconciles(Long accountId){ var a=accounts.findById(accountId).orElseThrow(); return a.getBalanceMinor()==ledger.sumForAccount(accountId); }
}
