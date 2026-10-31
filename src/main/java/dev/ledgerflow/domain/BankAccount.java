package dev.ledgerflow.domain;
import jakarta.persistence.*;
@Entity @Table(name="bank_account")
public class BankAccount {
  @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
  @ManyToOne(fetch=FetchType.LAZY, optional=false) @JoinColumn(name="owner_id") private AppUser owner;
  @Column(nullable=false) private String name;
  @Column(nullable=false, length=3) private String currency = "USD";
  /** Cached balance in integer minor units, reconciled against ledger_entry. Never floating point. */
  @Column(nullable=false) private long balanceMinor = 0;
  @Version private long version;
  protected BankAccount(){}
  public BankAccount(AppUser o,String n,String c){owner=o;name=n;currency=c;}
  public Long getId(){return id;} public AppUser getOwner(){return owner;}
  public String getName(){return name;} public String getCurrency(){return currency;}
  public long getBalanceMinor(){return balanceMinor;}
  public void applyDelta(long delta){ balanceMinor += delta; }
}
