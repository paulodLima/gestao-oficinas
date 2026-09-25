package br.com.gestao.oficinas_api.ordem;

import br.com.gestao.oficinas_api.identidade.ApiException;
import org.springframework.stereotype.Component;

@Component
public class OrderClosurePolicy {
    public String validate(Input input) {
        if (input == null || input.tipo() == null || input.tipo().active()
            || !input.confirmado() || input.expectedVersion() == null || input.expectedVersion() < 0) {
            throw new ApiException(400, "ENCERRAMENTO_INVALIDO", "Confirme a entrega ou o cancelamento e a versão atual da OS.");
        }
        String reason = input.motivo() == null ? "" : input.motivo().strip();
        if (reason.length() > 1000 || (input.tipo() == ServiceOrderStatus.CANCELADO && reason.isEmpty())) {
            throw new ApiException(400, "MOTIVO_INVALIDO", "Informe o motivo do cancelamento (até 1000 caracteres).");
        }
        return reason.isEmpty() ? null : reason;
    }

    public void requirePendingConsent(int pending, boolean cancelPending) {
        if (pending > 0 && !cancelPending) {
            throw new ApiException(409, "ADICIONAIS_PENDENTES", "Resolva os adicionais pendentes ou confirme seu cancelamento.");
        }
    }

    public record Input(ServiceOrderStatus tipo, String motivo, boolean confirmado,
                        boolean cancelarPendencias, Long expectedVersion) {}
}
