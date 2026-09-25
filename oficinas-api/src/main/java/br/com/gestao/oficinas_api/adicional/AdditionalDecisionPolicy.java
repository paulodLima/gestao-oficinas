package br.com.gestao.oficinas_api.adicional;

import java.time.Instant;
import java.util.Objects;
import org.springframework.stereotype.Component;

@Component
public class AdditionalDecisionPolicy {
    public boolean validChallenge(Instant expires, int attempts, Instant used, String expected,
                                  String received, Instant now) {
        return used == null && attempts < 5 && now.isBefore(expires)
            && constantTimeEquals(expected, received);
    }

    private boolean constantTimeEquals(String left, String right) {
        if (left == null || right == null || left.length() != right.length()) return false;
        int difference = 0;
        for (int index = 0; index < left.length(); index++) {
            difference |= left.charAt(index) ^ right.charAt(index);
        }
        return difference == 0 && Objects.equals(left.length(), right.length());
    }
}
