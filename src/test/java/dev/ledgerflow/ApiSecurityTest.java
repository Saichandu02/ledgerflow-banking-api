package dev.ledgerflow;
import org.junit.jupiter.api.Test; import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc; import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser; import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc; import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*; import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
@SpringBootTest @AutoConfigureMockMvc @ActiveProfiles("test")
class ApiSecurityTest {
  @Autowired MockMvc mvc;
  @Test void healthIsPublic() throws Exception { mvc.perform(get("/healthz")).andExpect(status().isOk()); }
  @Test void apiRequiresAuthentication() throws Exception { mvc.perform(get("/api/accounts")).andExpect(status().is3xxRedirection()); }
  @Test @WithMockUser("alice") void otherUsersStatementIsForbidden() throws Exception {
    mvc.perform(get("/api/accounts/999999/statement")).andExpect(status().isForbidden());
  }
  @Test @WithMockUser("alice") void transferWithoutIdempotencyKeyFails() throws Exception {
    mvc.perform(post("/api/transfers").contentType("application/json")
      .content("{\"sourceAccountId\":1,\"destinationAccountId\":2,\"amountMinor\":100,\"currency\":\"USD\"}"))
      .andExpect(status().isBadRequest());
  }
}
