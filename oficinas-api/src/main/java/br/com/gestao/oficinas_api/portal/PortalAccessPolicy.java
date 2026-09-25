package br.com.gestao.oficinas_api.portal;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.Instant;
import org.springframework.stereotype.Component;

@Component
public class PortalAccessPolicy {
    public boolean validChallenge(Instant expires, int attempts, Instant usedAt, String expectedHash,
                                  String candidateHash, Instant now) {
        return usedAt == null
            && attempts < 5
            && now.isBefore(expires)
            && MessageDigest.isEqual(expectedHash.getBytes(StandardCharsets.US_ASCII),
                candidateHash.getBytes(StandardCharsets.US_ASCII));
    }

    public boolean validLink(Instant expires, Instant revokedAt, Instant now) {
        return revokedAt == null && now.isBefore(expires);
    }

    public boolean expiredSession(Instant createdAt, Instant now, Duration duration) {
        return !now.isBefore(createdAt.plus(duration));
    }
}
