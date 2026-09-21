package br.com.gestao.oficinas_api.ordem;

import br.com.gestao.oficinas_api.cadastro.PageResult;
import br.com.gestao.oficinas_api.identidade.*;
import br.com.gestao.oficinas_api.oficina.ShopRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DashboardService {
    private static final Set<String> PARAMETERS = Set.of("q", "status", "situacao", "sort", "page", "size", "semAtualizacaoHoras", "minHorasEtapa");
    private final DashboardRepository repository;
    private final ShopRepository shops;
    private final Clock clock;
    public DashboardService(DashboardRepository repository, ShopRepository shops, Clock clock) {
        this.repository = repository; this.shops = shops; this.clock = clock;
    }
    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    public Snapshot get(Identidade owner, Map<String, String> parameters) {
        if (!PARAMETERS.containsAll(parameters.keySet())) throw invalid();
        DashboardRepository.Filter filter = filter(parameters);
        Instant now = clock.instant();
        return new Snapshot(repository.indicators(owner.oficinaId(), now), repository.cards(owner.oficinaId(), filter, now),
            shops.find(owner.oficinaId()).fuso(), now);
    }
    static DashboardRepository.Filter filter(Map<String, String> parameters) {
        try {
            String query = parameters.getOrDefault("q", "").strip();
            String rawStatus = parameters.getOrDefault("status", "");
            var status = rawStatus.isBlank() ? null : ServiceOrderStatus.valueOf(rawStatus);
            var situation = DashboardRepository.Situation.valueOf(parameters.getOrDefault("situacao", "ATIVAS"));
            var sort = DashboardRepository.Sort.valueOf(parameters.getOrDefault("sort", "ATUALIZACAO"));
            int page = Integer.parseInt(parameters.getOrDefault("page", "0"));
            int size = Integer.parseInt(parameters.getOrDefault("size", "20"));
            int staleHours = Integer.parseInt(parameters.getOrDefault("semAtualizacaoHoras", "0"));
            int stageHours = Integer.parseInt(parameters.getOrDefault("minHorasEtapa", "0"));
            if (query.length() > 100 || page < 0 || size < 1 || size > 100 || status != null && !status.active()) throw invalid();
            if (staleHours < 0 || stageHours < 0 || staleHours > 87600 || stageHours > 87600) throw invalid();
            return new DashboardRepository.Filter(query, status, situation, sort, page, size, staleHours, stageHours);
        } catch (IllegalArgumentException exception) { throw invalid(); }
    }
    private static ApiException invalid() { return new ApiException(400, "FILTRO_INVALIDO", "Confira os filtros e a paginação do painel."); }
    public record Snapshot(DashboardRepository.Indicators indicadores, PageResult<DashboardRepository.Card> ordens,
                           String fuso, Instant verificadoEm) {}
}
