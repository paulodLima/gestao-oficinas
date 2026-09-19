package br.com.gestao.oficinas_api.cadastro;

import br.com.gestao.oficinas_api.identidade.ApiException;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class CpfPlateTest {
    @Test void normalizesValidCpfAndRejectsInvalidValues() {
        assertEquals("52998224725", Cpf.normalize("529.982.247-25"));
        assertEquals("16899535009", Cpf.normalize("16899535009"));
        for (String invalid : new String[]{"", "111.111.111-11", "529.982.247-24", "123"})
            assertEquals("CPF_INVALIDO", assertThrows(ApiException.class, () -> Cpf.normalize(invalid)).code);
    }

    @Test void normalizesOldAndMercosulPlates() {
        assertEquals("ABC1234", Plate.normalize("abc-1234"));
        assertEquals("BRA1E23", Plate.normalize("bra 1e23"));
        for (String invalid : new String[]{"AB12345", "ABC12D3", "ABC-12345", ""})
            assertEquals("PLACA_INVALIDA", assertThrows(ApiException.class, () -> Plate.normalize(invalid)).code);
    }
}
