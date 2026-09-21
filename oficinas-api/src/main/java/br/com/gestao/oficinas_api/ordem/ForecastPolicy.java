package br.com.gestao.oficinas_api.ordem;

import java.time.Instant;

public final class ForecastPolicy {
    private ForecastPolicy() {}
    public static boolean isLate(ServiceOrderStatus status, Instant forecast, Instant now) {
        return status.active() && status != ServiceOrderStatus.PRONTO_PARA_RETIRADA
            && forecast != null && forecast.isBefore(now);
    }
}
