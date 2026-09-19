package br.com.gestao.oficinas_api.identidade;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import static org.junit.jupiter.api.Assertions.*;

class ClientAddressTest {
 @Test void onlyConfiguredProxyCanForwardDistinctClients() {
  var request=new MockHttpServletRequest();
  request.setRemoteAddr("127.0.0.1");
  request.addHeader("X-Oficinas-Client-IP","203.0.113.1");
  var addresses=new ClientAddress("127.0.0.1");
  assertEquals("203.0.113.1",addresses.resolve(request));
  request.removeHeader("X-Oficinas-Client-IP");
  request.addHeader("X-Oficinas-Client-IP","203.0.113.2");
  assertEquals("203.0.113.2",addresses.resolve(request));
  request.setRemoteAddr("192.0.2.5");
  assertEquals("192.0.2.5",addresses.resolve(request));
  request.setRemoteAddr("127.0.0.1");
  assertEquals("127.0.0.1",new ClientAddress("").resolve(request));
 }
}
