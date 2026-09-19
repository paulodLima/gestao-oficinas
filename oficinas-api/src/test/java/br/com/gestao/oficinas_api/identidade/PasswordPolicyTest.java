package br.com.gestao.oficinas_api.identidade;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class PasswordPolicyTest {
 @Test void acceptsBoundaryAndRejectsShortOrOversizedUtf8() {
  assertTrue(PasswordPolicy.valid("a".repeat(12)));
  assertTrue(PasswordPolicy.valid("a".repeat(72)));
  assertFalse(PasswordPolicy.valid("a".repeat(11)));
  assertFalse(PasswordPolicy.valid("a".repeat(73)));
  assertFalse(PasswordPolicy.valid("á".repeat(37)));
  assertFalse(PasswordPolicy.valid(null));
 }
 @Test void hashesAndNormalizesDeterministically() {
  assertEquals("owner@example.test",AuthService.normalize(" OWNER@example.test "));
  assertEquals(64,AuthService.hash("token").length());
  assertEquals(AuthService.hash("token"),AuthService.hash("token"));
  assertNotEquals(AuthService.hash("token"),AuthService.hash("other"));
 }
}

