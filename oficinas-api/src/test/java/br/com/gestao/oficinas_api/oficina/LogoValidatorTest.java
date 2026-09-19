package br.com.gestao.oficinas_api.oficina;

import br.com.gestao.oficinas_api.identidade.ApiException;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import static org.junit.jupiter.api.Assertions.*;

class LogoValidatorTest {
    private final LogoValidator validator = new LogoValidator();
    static byte[] png(int width, int height) throws Exception {
        var output = new ByteArrayOutputStream();
        ImageIO.write(new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB), "png", output);
        return output.toByteArray();
    }
    @Test void normalizesAndResizesValidImage() throws Exception {
        var file = new MockMultipartFile("arquivo", "logo.png", "image/png", png(1024, 512));
        var image = ImageIO.read(new ByteArrayInputStream(validator.normalize(file)));
        assertEquals(512, image.getWidth());
        assertEquals(256, image.getHeight());
    }
    @Test void rejectsFakeMimeEmptySvgAndOversize() throws Exception {
        for (var file : new MockMultipartFile[]{
            new MockMultipartFile("arquivo", "logo.png", "image/png", "<script>alert(1)</script>".getBytes()),
            new MockMultipartFile("arquivo", "logo.svg", "image/svg+xml", "<svg/>".getBytes()),
            new MockMultipartFile("arquivo", "logo.png", "image/png", new byte[0]),
            new MockMultipartFile("arquivo", "logo.jpg", "image/jpeg", png(10, 10))}) {
            assertThrows(ApiException.class, () -> validator.normalize(file));
        }
        assertThrows(ApiException.class, () -> validator.normalize(new MockMultipartFile("arquivo", "big.png", "image/png", new byte[LogoValidator.MAX_BYTES + 1])));
    }
    @Test void rejectsLargePixelDimensions() throws Exception {
        var file = new MockMultipartFile("arquivo", "large.png", "image/png", png(2100, 2000));
        assertThrows(ApiException.class, () -> validator.normalize(file));
    }
}
