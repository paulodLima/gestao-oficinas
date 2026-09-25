package br.com.gestao.oficinas_api.portal;

import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class PortalAccessPolicyTest {
    private final PortalAccessPolicy policy = new PortalAccessPolicy();
    private final Instant now = Instant.parse("2026-09-25T12:00:00Z");

    @Test
    void acceptsOnlyFreshUnusedChallengeBelowAttemptLimitWithMatchingHash() {
        assertTrue(policy.validChallenge(now.plusSeconds(1), 4, null, "abc", "abc", now));
        assertFalse(policy.validChallenge(now, 4, null, "abc", "abc", now));
        assertFalse(policy.validChallenge(now.plusSeconds(1), 5, null, "abc", "abc", now));
        assertFalse(policy.validChallenge(now.plusSeconds(1), 0, now, "abc", "abc", now));
        assertFalse(policy.validChallenge(now.plusSeconds(1), 0, null, "abc", "abd", now));
    }

    @Test
    void rejectsExpiredOrRevokedLinksAndExpiresSessionAtBoundary() {
        assertTrue(policy.validLink(now.plusSeconds(1), null, now));
        assertFalse(policy.validLink(now, null, now));
        assertFalse(policy.validLink(now.plusSeconds(1), now.minusSeconds(1), now));
        assertFalse(policy.expiredSession(now.minus(Duration.ofHours(24)).plusMillis(1), now,
            Duration.ofHours(24)));
        assertTrue(policy.expiredSession(now.minus(Duration.ofHours(24)), now, Duration.ofHours(24)));
    }
}
