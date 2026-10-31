package dev.ledgerflow.domain;
import jakarta.persistence.*; import java.time.Instant;
/** Immutable double-entry row: every transfer writes a -amount and a +amount row summing to zero. */
@Entity @Table(name="ledger_entry")
public class LedgerEntry {
  @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
  @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="transfer_id") private TransferRecord transfer;
  @ManyToOne(fetch=FetchType.LAZY, optional=false) @JoinColumn(name="account_id") private BankAccount account;
  @Column(nullable=false) private long amountMinor; // signed
  @Column(nullable=false) private String entryType; // DEBIT, CREDIT, OPENING
  @Column(nullable=false) private Instant createdAt = Instant.now();
  protected LedgerEntry(){}
  public LedgerEntry(TransferRecord t,BankAccount a,long amt,String type){transfer=t;account=a;amountMinor=amt;entryType=type;}
  public Long getId(){return id;} public TransferRecord getTransfer(){return transfer;}
  public BankAccount getAccount(){return account;} public long getAmountMinor(){return amountMinor;}
  public String getEntryType(){return entryType;} public Instant getCreatedAt(){return createdAt;}
}
