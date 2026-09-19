package br.com.gestao.oficinas_api.oficina;

import br.com.gestao.oficinas_api.identidade.ApiException;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Set;
import javax.imageio.ImageIO;
import javax.imageio.stream.MemoryCacheImageInputStream;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

@Component
public class LogoValidator {
    public static final int MAX_BYTES = 2 * 1024 * 1024;
    private static final long MAX_PIXELS = 4_000_000;
    private static final int MAX_EDGE = 512;
    public byte[] normalize(MultipartFile file) {
        if (file.isEmpty()) throw invalid();
        if (file.getSize() > MAX_BYTES) throw new ApiException(413, "LOGO_GRANDE", "A logo deve ter até 2 MiB.");
        if (!Set.of("image/png", "image/jpeg").contains(String.valueOf(file.getContentType()))) throw invalid();
        try (var input = new MemoryCacheImageInputStream(new ByteArrayInputStream(file.getBytes()))) {
            var readers = ImageIO.getImageReaders(input);
            if (!readers.hasNext()) throw invalid();
            var reader = readers.next();
            try {
                reader.setInput(input, true, true);
                String format = reader.getFormatName().toLowerCase(java.util.Locale.ROOT);
                if (!Set.of("png", "jpeg").contains(format)) throw invalid();
                if (!file.getContentType().equals("image/" + format)) throw invalid();
                long pixels = (long) reader.getWidth(0) * reader.getHeight(0);
                if (pixels <= 0 || pixels > MAX_PIXELS) throw invalid();
                return resize(reader.read(0));
            } finally { reader.dispose(); }
        } catch (IOException | IllegalArgumentException exception) { throw invalid(); }
    }
    private byte[] resize(BufferedImage source) throws IOException {
        double scale = Math.min(1.0, (double) MAX_EDGE / Math.max(source.getWidth(), source.getHeight()));
        var image = new BufferedImage(Math.max(1, (int) (source.getWidth() * scale)),
            Math.max(1, (int) (source.getHeight() * scale)), BufferedImage.TYPE_INT_ARGB);
        var graphics = image.createGraphics();
        try {
            graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            graphics.drawImage(source, 0, 0, image.getWidth(), image.getHeight(), null);
        } finally { graphics.dispose(); }
        var output = new ByteArrayOutputStream();
        ImageIO.write(image, "png", output);
        return output.toByteArray();
    }
    private ApiException invalid() {
        return new ApiException(415, "LOGO_INVALIDA", "Selecione uma imagem PNG ou JPEG válida, de até 4 megapixels.");
    }
}
