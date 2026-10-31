package dev.ledgerflow.repo;
import dev.ledgerflow.domain.*;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page; import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*; import org.springframework.data.repository.query.Param;
import java.util.*;
public class Repositories {
  public interface UserRepo extends JpaRepository<AppUser,Long>{ Optional<AppUser> findByUsername(String u); }
  public interface AccountRepo extends JpaRepository<BankAccount,Long>{
    List<BankAccount> findByOwnerUsername(String username);
    Optional<BankAccount> findByIdAndOwnerUsername(Long id,String username);
    @Lock(LockModeType.PESSIMISTIC_WRITE) @Query("select a from BankAccount a where a.id=:id")
    Optional<BankAccount> findByIdForUpdate(@Param("id") Long id);
  }
  public interface TransferRepo extends JpaRepository<TransferRecord,Long>{
    Optional<TransferRecord> findByIdempotencyKey(String key);
    long countBySourceIdOrDestinationId(Long s, Long d);
  }
  public interface LedgerRepo extends JpaRepository<LedgerEntry,Long>{
    Page<LedgerEntry> findByAccountIdOrderByCreatedAtDesc(Long accountId, Pageable p);
    @Query("select coalesce(sum(e.amountMinor),0) from LedgerEntry e where e.account.id=:id") long sumForAccount(@Param("id") Long id);
  }
}
