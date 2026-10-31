package dev.ledgerflow.config;
import dev.ledgerflow.repo.Repositories.UserRepo;
import org.springframework.context.annotation.*; import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.*; import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder; import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
@Configuration
public class SecurityConfig {
  @Bean PasswordEncoder passwordEncoder(){ return new BCryptPasswordEncoder(); }
  @Bean UserDetailsService userDetailsService(UserRepo repo){
    return username -> repo.findByUsername(username).map(u->User.withUsername(u.getUsername())
      .password(u.getPasswordHash()).roles(u.getRole()).build())
      .orElseThrow(()->new UsernameNotFoundException(username));
  }
  @Bean SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
    http
      // CSRF on for browser forms; the JSON API is session-authenticated and excluded so scripted clients can use it.
      // Documented tradeoff in docs/decisions.md.
      .csrf(c->c.csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse()).ignoringRequestMatchers("/api/**"))
      .authorizeHttpRequests(a->a.requestMatchers("/healthz","/readyz","/login","/css/**","/swagger-ui/**","/v3/api-docs/**").permitAll()
        .anyRequest().authenticated())
      .formLogin(f->f.loginPage("/login").defaultSuccessUrl("/",true).permitAll())
      .logout(l->l.logoutSuccessUrl("/login?logout"));
    return http.build();
  }
}
