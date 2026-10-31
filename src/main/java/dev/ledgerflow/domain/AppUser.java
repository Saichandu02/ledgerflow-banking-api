package dev.ledgerflow.domain;
import jakarta.persistence.*;
@Entity @Table(name="app_user")
public class AppUser {
  @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
  @Column(nullable=false, unique=true) private String username;
  @Column(nullable=false) private String passwordHash;
  @Column(nullable=false) private String role = "USER";
  protected AppUser(){}
  public AppUser(String u,String p,String r){username=u;passwordHash=p;role=r;}
  public Long getId(){return id;} public String getUsername(){return username;}
  public String getPasswordHash(){return passwordHash;} public String getRole(){return role;}
}
