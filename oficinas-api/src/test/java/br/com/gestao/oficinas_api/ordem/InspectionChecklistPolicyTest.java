package br.com.gestao.oficinas_api.ordem;

import br.com.gestao.oficinas_api.identidade.ApiException;
import java.util.Map;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class InspectionChecklistPolicyTest {
    @Test void acceptsControlledChecklist() {
        assertDoesNotThrow(() -> InspectionChecklistPolicy.validate(Map.of(
            "quilometragem", 48210, "combustivel", "METADE", "objetos", "Chave reserva",
            "avarias", "Risco no para-choque", "observacoes", "Cliente acompanhou",
            "fotos", Map.of())));
    }

    @Test void rejectsInvalidMileageFuelTextAndPhotoSlot() {
        assertThrows(ApiException.class, () -> InspectionChecklistPolicy.validate(Map.of("quilometragem", -1)));
        assertThrows(ApiException.class, () -> InspectionChecklistPolicy.validate(Map.of("combustivel", "QUASE_CHEIO")));
        assertThrows(ApiException.class, () -> InspectionChecklistPolicy.validate(Map.of("avarias", "x".repeat(2001))));
        assertThrows(ApiException.class, () -> InspectionChecklistPolicy.validate(Map.of("fotos", Map.of("motor", "id"))));
    }

    @Test void requiresAConciseCorrectionReason() {
        assertThrows(ApiException.class, () -> InspectionChecklistPolicy.validateCorrectionReason(" "));
        assertThrows(ApiException.class, () -> InspectionChecklistPolicy.validateCorrectionReason("x".repeat(1001)));
        assertDoesNotThrow(() -> InspectionChecklistPolicy.validateCorrectionReason("Quilometragem informada incorretamente."));
    }
}
