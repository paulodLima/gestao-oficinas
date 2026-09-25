package br.com.gestao.oficinas_api.ordem;

import br.com.gestao.oficinas_api.identidade.ApiException;
import com.drew.imaging.ImageMetadataReader;
import com.drew.imaging.ImageProcessingException;
import com.drew.metadata.exif.ExifIFD0Directory;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.*;
import java.util.UUID;
import javax.imageio.ImageIO;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

@Component
public class PhotoStorage {
    private static final long MAX_SIZE = 10L * 1024 * 1024;
    private static final long MAX_PIXELS = 40_000_000L;
    private static final int DISPLAY_SIZE = 2048;
    private static final int THUMB_SIZE = 480;
    private final Path root;
    public PhotoStorage(PhotoProperties properties) {
        try { root = Paths.get(properties.storagePath()).toAbsolutePath().normalize(); Files.createDirectories(root); }
        catch (IOException e) { throw new IllegalStateException("Não foi possível preparar o armazenamento de fotos.", e); }
    }
    public StoredPhoto store(MultipartFile file) {
        if (file == null || file.isEmpty()) throw invalid("Selecione uma foto para enviar.");
        if (file.getSize() > MAX_SIZE) throw new ApiException(413, "ARQUIVO_GRANDE", "Cada foto deve ter até 10 MiB.");
        byte[] bytes;
        try { bytes = file.getBytes(); } catch (IOException e) { throw invalid("Não foi possível ler a foto."); }
        DisplayPhoto display = normalize(bytes);
        String extension = display.contentType().equals("image/jpeg") ? "jpg" : "png";
        String key = "view-" + UUID.randomUUID() + "." + extension;
        String thumb = key.substring(0, key.lastIndexOf('.')) + "-thumb.jpg";
        try {
            byte[] preview = encode(scale(decode(display.bytes()), THUMB_SIZE, BufferedImage.TYPE_INT_RGB), "jpg");
            write(key, display.bytes());
            write(thumb, preview);
        } catch (IOException | RuntimeException e) {
            delete(key); delete(thumb);
            throw invalid("A imagem enviada é inválida ou não pôde ser armazenada.");
        }
        return new StoredPhoto(key, thumb, display.contentType(), display.bytes().length);
    }
    public byte[] read(String key) {
        try { return Files.readAllBytes(path(key)); }
        catch (IOException e) { throw new ApiException(404, "FOTO_NAO_ENCONTRADA", "A foto não está mais disponível."); }
    }
    public DisplayPhoto readDisplay(String key) {
        byte[] bytes = read(key);
        // Only this storage writer creates view-* keys; legacy files are never overwritten.
        return key.startsWith("view-") ? new DisplayPhoto(bytes, detect(bytes)) : normalize(bytes);
    }
    public void delete(String key) { if (key == null) return; try { Files.deleteIfExists(path(key)); } catch (IOException ignored) {} }
    private DisplayPhoto normalize(byte[] bytes) {
        String type = detect(bytes);
        String format = type.equals("image/jpeg") ? "jpg" : "png";
        try {
            var bounded = scale(decode(bytes), DISPLAY_SIZE,
                format.equals("jpg") ? BufferedImage.TYPE_INT_RGB : BufferedImage.TYPE_INT_ARGB);
            return new DisplayPhoto(encode(orient(bounded, orientation(bytes)), format),
                format.equals("jpg") ? "image/jpeg" : "image/png");
        } catch (IOException | RuntimeException e) {
            throw invalid("A imagem enviada é inválida ou está corrompida.");
        }
    }
    private BufferedImage decode(byte[] bytes) throws IOException {
        try (var input = ImageIO.createImageInputStream(new ByteArrayInputStream(bytes))) {
            var readers = ImageIO.getImageReaders(input);
            if (!readers.hasNext()) throw new IOException("invalid");
            var reader = readers.next();
            try {
                reader.setInput(input);
                int width = reader.getWidth(0), height = reader.getHeight(0);
                if (width <= 0 || height <= 0 || (long) width * height > MAX_PIXELS) throw new IOException("invalid");
                var parameters = reader.getDefaultReadParam();
                int sampling = Math.max(1, Math.max(width, height) / DISPLAY_SIZE);
                parameters.setSourceSubsampling(sampling, sampling, 0, 0);
                return reader.read(0, parameters);
            } finally { reader.dispose(); }
        }
    }
    private BufferedImage scale(BufferedImage source, int limit, int type) {
        double scale = Math.min(1d, (double) limit / Math.max(source.getWidth(), source.getHeight()));
        BufferedImage output = new BufferedImage(Math.max(1, (int) (source.getWidth() * scale)), Math.max(1, (int) (source.getHeight() * scale)), type);
        Graphics2D graphics = output.createGraphics(); graphics.drawImage(source, 0, 0, output.getWidth(), output.getHeight(), null); graphics.dispose();
        return output;
    }
    private byte[] encode(BufferedImage image, String format) throws IOException {
        var output = new ByteArrayOutputStream();
        if (!ImageIO.write(image, format, output)) throw new IOException("No image encoder");
        return output.toByteArray();
    }
    private int orientation(byte[] bytes) {
        try {
            var directory = ImageMetadataReader.readMetadata(new ByteArrayInputStream(bytes))
                .getFirstDirectoryOfType(ExifIFD0Directory.class);
            Integer value = directory == null ? null : directory.getInteger(ExifIFD0Directory.TAG_ORIENTATION);
            return value == null ? 1 : value;
        } catch (ImageProcessingException | IOException ignored) { return 1; }
    }
    // Normalize orientation only after downsampling to bound memory usage.
    static BufferedImage orient(BufferedImage source, int orientation) {
        if (orientation < 2 || orientation > 8) return source;
        int width = source.getWidth(), height = source.getHeight();
        boolean swapped = orientation >= 5;
        var result = new BufferedImage(swapped ? height : width, swapped ? width : height,
            source.getColorModel().hasAlpha() ? BufferedImage.TYPE_INT_ARGB : BufferedImage.TYPE_INT_RGB);
        for (int y = 0; y < height; y++) for (int x = 0; x < width; x++) {
            int targetX = switch (orientation) {
                case 2, 3 -> width - 1 - x;
                case 5, 8 -> y;
                case 6, 7 -> height - 1 - y;
                default -> x;
            };
            int targetY = switch (orientation) {
                case 3, 4 -> height - 1 - y;
                case 5, 6 -> x;
                case 7, 8 -> width - 1 - x;
                default -> y;
            };
            result.setRGB(targetX, targetY, source.getRGB(x, y));
        }
        return result;
    }
    private void write(String key, byte[] bytes) { try { Files.write(path(key), bytes, StandardOpenOption.CREATE_NEW); } catch (IOException e) { throw new IllegalStateException("Não foi possível armazenar a foto.", e); } }
    private Path path(String key) { Path result = root.resolve(key).normalize(); if (!result.startsWith(root)) throw invalid("Arquivo inválido."); return result; }
    private String detect(byte[] value) {
        if (value.length > 3 && (value[0]&255)==255 && (value[1]&255)==216 && (value[2]&255)==255) return "image/jpeg";
        if (value.length > 8 && value[0]==(byte)137 && value[1]==80 && value[2]==78 && value[3]==71) return "image/png";
        if (value.length > 12 && new String(value, 0, 4).equals("RIFF") && new String(value, 8, 4).equals("WEBP")) return "image/webp";
        throw invalid("Envie uma imagem JPEG, PNG ou WebP válida.");
    }
    private ApiException invalid(String detail) { return new ApiException(400, "FOTO_INVALIDA", detail); }
    public record StoredPhoto(String key, String thumbnailKey, String contentType, long size) {}
    public record DisplayPhoto(byte[] bytes, String contentType) {}
}
