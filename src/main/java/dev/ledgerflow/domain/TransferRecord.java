package dev.ledgerflow.domain;
import jakarta.persistence.*; import java.time.Instant;
@Entity @Table(name="transfer_record", uniqueConstraints=@UniqueConstraint(name="uq_transfer_idempotency_key", columnNames="idempotency_key"))
public class TransferRecord {
  public enum Status { COMMITTED }
  @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
  @Column(name="idempotency_key", nullable=false) private String idempotencyKey;
  @Column(nullable=false) private String requestHash;
  @ManyToOne(fetch=FetchType.LAZY, optional=false) private BankAccount source;
  @ManyToOne(fetch=FetchType.LAZY, optional=false) private BankAccount destination;
  @Column(nullable=false) private long amountMinor;
  @Column(nullable=false, length=3) private String currency;
  @Enumerated(EnumType.STRING) @Column(nullable=false) private Status status = Status.COMMITTED;
  @Column(nullable=false) private Instant createdAt = Instant.now();
  protected TransferRecord(){}
  public TransferRecord(String k,String h,BankAccount s,BankAccount d,long a,String c){idempotencyKey=k;requestHash=h;source=s;destination=d;amountMinor=a;currency=c;}
  public Long getId(){return id;} public String getIdempotencyKey(){return idempotencyKey;}
  public String getRequestHash(){return requestHash;} public BankAccount getSource(){return source;}
  public BankAccount getDestination(){return destination;} public long getAmountMinor(){return amountMinor;}
  public String getCurrency(){return currency;} public Status getStatus(){return status;} public Instant getCreatedAt(){return createdAt;}
}
