package br.com.gestao.oficinas_api.ordem;

import br.com.gestao.oficinas_api.identidade.ApiException;
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
        String type = detect(bytes);
        String extension = type.equals("image/jpeg") ? "jpg" : type.equals("image/png") ? "png" : "webp";
        String key = UUID.randomUUID() + "." + extension;
        write(key, bytes);
        String thumb = null;
        if (!type.equals("image/webp")) {
            try { thumb = thumbnail(key, bytes); }
            catch (IOException e) { delete(key); throw invalid("A imagem enviada é inválida ou está corrompida."); }
        }
        return new StoredPhoto(key, thumb, type, bytes.length);
    }
    public byte[] read(String key) {
        try { return Files.readAllBytes(path(key)); }
        catch (IOException e) { throw new ApiException(404, "FOTO_NAO_ENCONTRADA", "A foto não está mais disponível."); }
    }
    public void delete(String key) { if (key == null) return; try { Files.deleteIfExists(path(key)); } catch (IOException ignored) {} }
    private String thumbnail(String key, byte[] bytes) throws IOException {
        BufferedImage source = ImageIO.read(new ByteArrayInputStream(bytes));
        if (source == null || (long) source.getWidth() * source.getHeight() > 40_000_000L) throw new IOException("invalid");
        double scale = Math.min(1d, 480d / Math.max(source.getWidth(), source.getHeight()));
        BufferedImage output = new BufferedImage(Math.max(1, (int) (source.getWidth() * scale)), Math.max(1, (int) (source.getHeight() * scale)), BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = output.createGraphics(); graphics.drawImage(source, 0, 0, output.getWidth(), output.getHeight(), null); graphics.dispose();
        String thumb = key.substring(0, key.lastIndexOf('.')) + "-thumb.jpg";
        ByteArrayOutputStream out = new ByteArrayOutputStream(); ImageIO.write(output, "jpg", out); write(thumb, out.toByteArray()); return thumb;
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
}
