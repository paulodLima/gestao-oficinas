package br.com.gestao.oficinas_api.adicional;

import java.time.Instant;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AdditionalDecisionPolicyTest {
    private final AdditionalDecisionPolicy policy = new AdditionalDecisionPolicy();
    private final Instant now = Instant.parse("2026-09-25T12:00:00Z");

    @Test void acceptsUnusedMatchingCodeBeforeExpiry() {
        assertTrue(policy.validChallenge(now.plusSeconds(60), 0, null, "hash", "hash", now));
    }

    @Test void rejectsExpiredUsedExhaustedOrDifferentChallenges() {
        assertFalse(policy.validChallenge(now, 0, null, "hash", "hash", now));
        assertFalse(policy.validChallenge(now.plusSeconds(60), 0, now, "hash", "hash", now));
        assertFalse(policy.validChallenge(now.plusSeconds(60), 5, null, "hash", "hash", now));
        assertFalse(policy.validChallenge(now.plusSeconds(60), 0, null, "hash", "other", now));
    }
}
