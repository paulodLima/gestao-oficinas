package br.com.gestao.oficinas_api.identidade;

import java.time.Clock;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;

@Service
public class RateLimit {
    private final JdbcTemplate jdbc;
    private final Clock clock;
    public RateLimit(JdbcTemplate jdbc, Clock clock) { this.jdbc=jdbc; this.clock=clock; }
    @Transactional(propagation=Propagation.REQUIRES_NEW, noRollbackFor=ApiException.class)
    public void check(String key, int max) {
        long window=clock.instant().getEpochSecond()/900;
        Integer count=jdbc.queryForObject("""
            INSERT INTO auth_limite(chave,janela,tentativas) VALUES (?,?,1)
            ON CONFLICT(chave) DO UPDATE SET janela=EXCLUDED.janela,
            tentativas=CASE WHEN auth_limite.janela=EXCLUDED.janela THEN auth_limite.tentativas+1 ELSE 1 END
            RETURNING tentativas
            """,Integer.class,AuthService.hash(key),window);
        if(count>max) throw new ApiException(429,"LIMITE_TENTATIVAS","Muitas tentativas. Tente novamente em 15 minutos.");
        jdbc.update("DELETE FROM auth_limite WHERE janela < ?",window-4);
    }
}

