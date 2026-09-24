package br.com.gestao.oficinas_api.ordem;

import br.com.gestao.oficinas_api.identidade.ApiException;
import java.util.Map;
import java.util.Set;

final class InspectionChecklistPolicy {
    private static final Set<String> FUEL_LEVELS = Set.of(
        "VAZIO", "RESERVA", "UM_QUARTO", "METADE", "TRES_QUARTOS", "CHEIO");
    private static final Set<String> PHOTO_SLOTS = Set.of(
        "frente", "traseira", "lateralEsquerda", "lateralDireita", "painel", "detalhes");

    private InspectionChecklistPolicy() {}

    static void validate(Map<String, Object> data) {
        if (data == null || data.size() > 6) invalid("Checklist inválido.");
        Object mileage = data.get("quilometragem");
        if (mileage != null && (!(mileage instanceof Number value)
            || value.longValue() < 0 || value.longValue() > 9_999_999)) {
            invalid("Quilometragem inválida.");
        }
        Object fuel = data.get("combustivel");
        if (fuel != null && (!(fuel instanceof String value) || !FUEL_LEVELS.contains(value))) {
            invalid("Nível de combustível inválido.");
        }
        text(data, "objetos", 1000);
        text(data, "avarias", 2000);
        text(data, "observacoes", 2000);
        Object photos = data.get("fotos");
        if (photos != null && (!(photos instanceof Map<?, ?> references)
            || references.size() > PHOTO_SLOTS.size() || !PHOTO_SLOTS.containsAll(references.keySet()))) {
            invalid("Roteiro de fotos inválido.");
        }
    }

    static void validateCorrectionReason(String reason) {
        if (reason == null || reason.isBlank() || reason.length() > 1000) {
            invalid("Informe o motivo da correção.");
        }
    }

    private static void text(Map<String, Object> data, String field, int limit) {
        Object value = data.get(field);
        if (value != null && (!(value instanceof String text) || text.length() > limit)) {
            invalid("Campo de vistoria inválido: " + field + ".");
        }
    }

    private static void invalid(String detail) {
        throw new ApiException(400, "DADOS_INVALIDOS", detail);
    }
}
