package dev.ledgerflow;
import dev.ledgerflow.service.TransferService; import org.junit.jupiter.api.Test; import static org.junit.jupiter.api.Assertions.*;
class TransferServiceUnitTest {
  @Test void requestHashIsDeterministicAndPayloadSensitive(){
    var a=TransferService.requestHash(1,2,2500,"USD"); var b=TransferService.requestHash(1,2,2500,"USD");
    var c=TransferService.requestHash(1,2,2501,"USD"); assertEquals(a,b); assertNotEquals(a,c); assertEquals(64,a.length());
  }
}
