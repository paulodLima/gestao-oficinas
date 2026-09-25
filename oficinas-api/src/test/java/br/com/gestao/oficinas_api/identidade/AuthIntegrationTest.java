package br.com.gestao.oficinas_api.identidade;

import br.com.gestao.oficinas_api.notificacoes.TransactionalEmail;
import java.net.*;
import java.net.http.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.regex.*;
import org.junit.jupiter.api.*;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import br.com.gestao.oficinas_api.support.TestPostgres;
import org.testcontainers.junit.jupiter.*;
import tools.jackson.databind.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
class AuthIntegrationTest {
 @Container static final TestPostgres postgres=new TestPostgres();
 @DynamicPropertySource static void properties(DynamicPropertyRegistry r) {
  r.add("spring.datasource.url",postgres::getJdbcUrl);
  r.add("spring.datasource.username",postgres::getUsername);
  r.add("spring.datasource.password",postgres::getPassword);
 }
 @LocalServerPort int port;
 @Autowired JdbcTemplate jdbc;
 @Autowired IdentidadeRepository repository;
 @Autowired RateLimit limits;
 org.springframework.session.FindByIndexNameSessionRepository<org.springframework.session.Session> sessions;
 @Autowired @SuppressWarnings({"unchecked", "rawtypes"})
 void configureSessions(org.springframework.session.jdbc.JdbcIndexedSessionRepository repository) {
  sessions=(org.springframework.session.FindByIndexNameSessionRepository)repository;
 }
 @MockitoBean TransactionalEmail mail;
 final ObjectMapper mapper=new ObjectMapper();
 static final String PASSWORD="Oficina-segura-123";
 class Browser {
  final CookieManager cookies=new CookieManager(null,CookiePolicy.ACCEPT_ALL);
  final HttpClient client=HttpClient.newBuilder().cookieHandler(cookies).build();
  HttpResponse<String> get(String path) throws Exception {
   return client.send(HttpRequest.newBuilder(URI.create("http://localhost:"+port+path)).GET().build(),HttpResponse.BodyHandlers.ofString());
  }
  HttpResponse<String> post(String path,Map<String,?> body) throws Exception {
   String token=mapper.readTree(get("/api/auth/csrf").body()).get("token").asText();
   return client.send(HttpRequest.newBuilder(URI.create("http://localhost:"+port+path))
    .header("Content-Type","application/json").header("X-CSRF-TOKEN",token)
    .POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(body))).build(),HttpResponse.BodyHandlers.ofString());
  }
  String cookie() { return cookies.getCookieStore().getCookies().toString(); }
 }
 String email() { return UUID.randomUUID()+"@example.test"; }
 Browser registered(String email) throws Exception {
  Browser b=new Browser();
  assertEquals(201,b.post("/api/auth/cadastro",Map.of("nome","Dono Teste","email",email,"senha",PASSWORD,"nomeOficina","Oficina "+email)).statusCode());
  return b;
 }
 void login(Browser b,String email,String password) throws Exception {
  var r=b.post("/api/auth/login",Map.of("email",email,"senha",password));
  assertEquals(200,r.statusCode(),r.body());
 }
 String recovery(String email) throws Exception {
  reset(mail);
  var b=new Browser();
  assertEquals(202,b.post("/api/auth/recuperacao",Map.of("email",email)).statusCode());
  var text=ArgumentCaptor.forClass(String.class);
  verify(mail).send(eq(email),anyString(),text.capture());
  var matcher=Pattern.compile("#token=([A-Za-z0-9_-]{43})").matcher(text.getValue());
  assertTrue(matcher.find());return matcher.group(1);
 }
 @Test void registrationIsolationAndSessionRotation() throws Exception {
  String a=email(),b=email();
  Browser ba=registered(a),bb=registered(b);
  String before=ba.cookie();
  login(ba,a,PASSWORD);login(bb,b,PASSWORD);
  assertNotEquals(before,ba.cookie());
  var meA=mapper.readTree(ba.get("/api/auth/me").body());
  var meB=mapper.readTree(bb.get("/api/auth/me").body());
  assertNotEquals(meA.get("oficina").get("id").asText(),meB.get("oficina").get("id").asText());
  assertFalse(meA.toString().contains("senha"));
  var spoof=mapper.readTree(ba.get("/api/oficina?oficinaId="+meB.get("oficina").get("id").asText()).body());
  assertEquals(meA.get("oficina").get("id").asText(),spoof.get("id").asText());
  assertTrue(repository.porIdentidade(new Identidade(UUID.fromString(meA.get("id").asText()),
   UUID.fromString(meB.get("oficina").get("id").asText()),0,Instant.now())).isEmpty());
  assertTrue(jdbc.queryForObject("SELECT count(*) FROM SPRING_SESSION",Integer.class)>0);
 }
 @Test void rejectsAnonymousAndMissingCsrf() throws Exception {
  Browser b=new Browser();
  assertEquals(401,b.get("/api/auth/me").statusCode());
  var r=b.client.send(HttpRequest.newBuilder(URI.create("http://localhost:"+port+"/api/auth/login"))
   .header("Content-Type","application/json").POST(HttpRequest.BodyPublishers.ofString("{}")).build(),HttpResponse.BodyHandlers.ofString());
  assertEquals(403,r.statusCode());
 }
 @Test void passwordAndDuplicateValidation() throws Exception {
  String address=email();Browser b=registered(address);
  assertEquals(409,b.post("/api/auth/cadastro",Map.of("nome","Nome","email",address.toUpperCase(),
   "senha",PASSWORD,"nomeOficina","Oficina")).statusCode());
  assertEquals(400,b.post("/api/auth/cadastro",Map.of("nome","Nome","email",email(),"senha","á".repeat(37),"nomeOficina","Oficina")).statusCode());
  assertEquals(401,b.post("/api/auth/login",Map.of("email",address,"senha","wrong-password")).statusCode());
 }
 @Test void logoutRevokesAccess() throws Exception {
  String address=email();Browser b=registered(address);login(b,address,PASSWORD);
  assertEquals(204,b.post("/api/auth/logout",Map.of()).statusCode());
  assertEquals(401,b.get("/api/auth/me").statusCode());
 }
 @Test void recoveryIsGenericForUnknownAddress() throws Exception {
  reset(mail);
  assertEquals(202,new Browser().post("/api/auth/recuperacao",Map.of("email",email())).statusCode());
  verifyNoInteractions(mail);
 }
 @Test void resetSingleUseRevokesAllSessions() throws Exception {
  String address=email();Browser one=registered(address),two=new Browser();
  login(one,address,PASSWORD);login(two,address,PASSWORD);
  String token=recovery(address);
  Browser anon=new Browser();
  assertEquals(204,anon.post("/api/auth/redefinicao",Map.of("token",token,"novaSenha","Nova-senha-segura-456")).statusCode());
  assertEquals(401,one.get("/api/auth/me").statusCode());
  assertEquals(401,two.get("/api/auth/me").statusCode());
  assertEquals(400,anon.post("/api/auth/redefinicao",Map.of("token",token,"novaSenha",PASSWORD)).statusCode());
  assertEquals(401,anon.post("/api/auth/login",Map.of("email",address,"senha",PASSWORD)).statusCode());
  login(anon,address,"Nova-senha-segura-456");
 }
 @Test void expiredTokenAndReissuedTokenCannotReset() throws Exception {
  String address=email();registered(address);
  String old=recovery(address),fresh=recovery(address);
  assertEquals(400,new Browser().post("/api/auth/redefinicao",Map.of("token",old,"novaSenha",PASSWORD)).statusCode());
  jdbc.update("UPDATE recuperacao_senha SET expires_at=now()-interval '1 minute' WHERE token_hash=?",AuthService.hash(fresh));
  assertEquals(400,new Browser().post("/api/auth/redefinicao",Map.of("token",fresh,"novaSenha",PASSWORD)).statusCode());
 }
 @Test void simultaneousResetConsumesOnce() throws Exception {
  String address=email();registered(address);String token=recovery(address);
  try(var pool=Executors.newFixedThreadPool(2)) {
   var barrier=new CyclicBarrier(2);
   Callable<Integer> request=()->{Browser b=new Browser();barrier.await();return b.post("/api/auth/redefinicao",Map.of("token",token,"novaSenha",PASSWORD)).statusCode();};
   var first=pool.submit(request);var second=pool.submit(request);
   assertEquals(Set.of(204,400),Set.of(first.get(),second.get()));
  }
 }
 @Test void inactiveAccountLosesAccess() throws Exception {
  String address=email();Browser b=registered(address);login(b,address,PASSWORD);
  jdbc.update("UPDATE proprietario SET ativo=false WHERE email=?",address);
  assertEquals(401,b.get("/api/auth/me").statusCode());
  assertEquals(401,b.post("/api/auth/login",Map.of("email",address,"senha",PASSWORD)).statusCode());
 }
 @Test void idleSessionExpiresAfterThirtyMinutes() throws Exception {
  String address=email();Browser b=registered(address);login(b,address,PASSWORD);
  String id=repository.porEmail(address).orElseThrow().id().toString();
  var session=sessions.findByPrincipalName(id).values().iterator().next();
  assertEquals(Duration.ofMinutes(30),session.getMaxInactiveInterval());
  session.setLastAccessedTime(Instant.now().minus(Duration.ofMinutes(31)));sessions.save(session);
  assertEquals(401,b.get("/api/auth/me").statusCode());
 }
 @Test void absoluteSessionExpiresEvenWithRecentActivity() throws Exception {
  String address=email();Browser b=registered(address);login(b,address,PASSWORD);
  String id=repository.porEmail(address).orElseThrow().id().toString();
  var session=sessions.findByPrincipalName(id).values().iterator().next();
  org.springframework.security.core.context.SecurityContext context=session.getAttribute("SPRING_SECURITY_CONTEXT");
  var auth=(org.springframework.security.authentication.UsernamePasswordAuthenticationToken)context.getAuthentication();
  var identity=(Identidade)auth.getDetails();
  auth.setDetails(new Identidade(identity.id(),identity.oficinaId(),identity.authVersion(),Instant.now().minus(Duration.ofHours(13))));
  session.setAttribute("SPRING_SECURITY_CONTEXT",context);sessions.save(session);
  assertEquals(401,b.get("/api/auth/me").statusCode());
 }
 @Test void rateLimitAndMailFailureAreSafe() throws Exception {
  String key=UUID.randomUUID().toString();
  for(int i=0;i<5;i++)limits.check(key,5);
  assertEquals(429,assertThrows(ApiException.class,()->limits.check(key,5)).status);
  String address=email();registered(address);
  doThrow(new ApiException(503,"EMAIL_INDISPONIVEL","Indisponível")).when(mail).send(eq(address),anyString(),anyString());
  assertEquals(202,new Browser().post("/api/auth/recuperacao",Map.of("email",address)).statusCode());
  assertEquals(202,new Browser().post("/api/auth/recuperacao",Map.of("email",email())).statusCode());
  assertEquals(0,jdbc.queryForObject("SELECT count(*) FROM recuperacao_senha r JOIN proprietario p ON p.id=r.proprietario_id WHERE p.email=?",Integer.class,address));
 }
}
