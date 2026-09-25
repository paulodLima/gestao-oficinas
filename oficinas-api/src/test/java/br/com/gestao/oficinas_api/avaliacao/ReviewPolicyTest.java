package br.com.gestao.oficinas_api.avaliacao;

import br.com.gestao.oficinas_api.identidade.ApiException;
import java.math.BigDecimal;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import static org.junit.jupiter.api.Assertions.*;

class ReviewPolicyTest {
    final ReviewPolicy policy = new ReviewPolicy();
    @ParameterizedTest @ValueSource(strings = {"0", "6", "-1", "2.5", "10000000000000000000"})
    void rejectsInvalidScores(String value) {
        assertThrows(ApiException.class, () -> policy.validate(new ReviewPolicy.Input(new BigDecimal(value), null, null)));
    }
    @Test void normalizesCommentAndDefaultsConsentToPrivate() {
        assertEquals(new ReviewPolicy.Rating(1, "Ruim", false), policy.validate(new ReviewPolicy.Input(BigDecimal.ONE, " Ruim ", null)));
        assertEquals(new ReviewPolicy.Rating(5, null, true), policy.validate(new ReviewPolicy.Input(BigDecimal.valueOf(5), " ", true)));
        assertThrows(ApiException.class, () -> policy.validate(null));
        assertThrows(ApiException.class, () -> policy.validate(new ReviewPolicy.Input(null, null, null)));
        assertThrows(ApiException.class, () -> policy.validate(new ReviewPolicy.Input(BigDecimal.ONE, "a".repeat(2001), false)));
    }
    @Test void expiresExactlyAtSevenDaysAndRevocationWins() {
        var delivered = Instant.parse("2026-09-25T12:00:00Z");
        var expires = delivered.plus(ReviewPolicy.VALIDITY);
        assertTrue(policy.valid(expires, null, expires.minusNanos(1)));
        assertFalse(policy.valid(expires, null, expires));
        assertFalse(policy.valid(expires, delivered, delivered));
    }
    @Test void acceptsOnlyDedicatedGoogleReviewDestinations() {
        assertNull(policy.googleUrl(" "));
        assertEquals("https://g.page/r/Test_123/review", policy.googleUrl(" https://g.page/r/Test_123/review "));
        assertEquals("https://search.google.com/local/writereview?placeid=Test_123", policy.googleUrl("https://search.google.com/local/writereview?placeid=Test_123"));
        for (String url : new String[] {"javascript:alert(1)", "http://g.page/r/test/review", "https://g.page.evil.test/r/test/review",
            "https://user@g.page/r/test/review", "https://g.page/r/test/review?token=secret", "https://g.page/r/test/review#secret",
            "https://search.google.com/url?q=evil", "https://search.google.com/local/writereview?placeid=a&rating=5"}) {
            assertThrows(ApiException.class, () -> policy.googleUrl(url), url);
        }
    }
}
