package br.com.gestao.oficinas_api.avaliacao;

import br.com.gestao.oficinas_api.identidade.ApiException;
import java.math.BigDecimal;
import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import org.springframework.stereotype.Component;

@Component
public class ReviewPolicy {
    public static final Duration VALIDITY = Duration.ofDays(7);
    public static final String CONSENT = "Autorizo a oficina a publicar minha nota e meu comentário, sem nome, placa, CPF, contatos ou fotos do veículo. A autorização é opcional e não publica automaticamente a avaliação.";
    public Rating validate(Input input) {
        if (input == null || input.nota() == null) throw invalid();
        int score;
        try { score = input.nota().intValueExact(); } catch (ArithmeticException error) { throw invalid(); }
        String comment = input.comentario() == null ? "" : input.comentario().strip();
        if (score < 1 || score > 5 || comment.length() > 2000) throw invalid();
        return new Rating(score, comment.isEmpty() ? null : comment, Boolean.TRUE.equals(input.consentimentoPublicacao()));
    }
    public boolean valid(Instant expires, Instant revoked, Instant now) {
        return revoked == null && now.isBefore(expires);
    }
    public String googleUrl(String value) {
        if (value == null || value.isBlank()) return null;
        String url = value.strip();
        try {
            URI uri = URI.create(url);
            if (url.length() > 500 || !"https".equals(uri.getScheme()) || uri.getUserInfo() != null
                || uri.getPort() != -1 || uri.getFragment() != null) throw new IllegalArgumentException();
            boolean shortLink = "g.page".equals(uri.getHost()) && uri.getPath().matches("/r/[A-Za-z0-9_-]+/review") && uri.getQuery() == null;
            boolean placeLink = "search.google.com".equals(uri.getHost()) && "/local/writereview".equals(uri.getPath())
                && uri.getRawQuery() != null && uri.getRawQuery().matches("placeid=[A-Za-z0-9_-]+");
            if (!shortLink && !placeLink) throw new IllegalArgumentException();
            return url;
        } catch (IllegalArgumentException error) {
            throw new ApiException(400, "LINK_GOOGLE_INVALIDO", "Use o link de avaliação do Google: https://g.page/r/.../review ou https://search.google.com/local/writereview?placeid=...");
        }
    }
    private ApiException invalid() { return new ApiException(400, "AVALIACAO_INVALIDA", "Informe nota inteira de 1 a 5 e comentário de até 2000 caracteres."); }
    public record Input(BigDecimal nota, String comentario, Boolean consentimentoPublicacao) {}
    public record Rating(int score, String comment, boolean consent) {}
}
