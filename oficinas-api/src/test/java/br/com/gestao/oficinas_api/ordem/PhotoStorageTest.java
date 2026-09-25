package br.com.gestao.oficinas_api.ordem;

import static org.junit.jupiter.api.Assertions.*;
import br.com.gestao.oficinas_api.support.PhotoFixture;
import com.drew.imaging.ImageMetadataReader;
import com.drew.metadata.exif.ExifIFD0Directory;
import com.drew.metadata.exif.GpsDirectory;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.file.Path;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.mock.web.MockMultipartFile;

class PhotoStorageTest {
    @TempDir Path directory;

    @ParameterizedTest
    @CsvSource({"1,1,2,3,4", "2,2,1,4,3", "3,4,3,2,1", "4,3,4,1,2",
        "5,1,3,2,4", "6,3,1,4,2", "7,4,2,3,1", "8,2,4,1,3"})
    void appliesAllExifOrientations(int orientation, int a, int b, int c, int d) {
        var source = new BufferedImage(2, 2, BufferedImage.TYPE_INT_RGB);
        source.setRGB(0, 0, 1); source.setRGB(1, 0, 2);
        source.setRGB(0, 1, 3); source.setRGB(1, 1, 4);
        var result = PhotoStorage.orient(source, orientation);
        assertEquals(a, result.getRGB(0, 0) & 0xffffff);
        assertEquals(b, result.getRGB(1, 0) & 0xffffff);
        assertEquals(c, result.getRGB(0, 1) & 0xffffff);
        assertEquals(d, result.getRGB(1, 1) & 0xffffff);
    }

    @Test
    void normalizesOrientationAndRemovesCameraAndGpsFromStoredImages() throws Exception {
        byte[] bytes = PhotoFixture.cameraJpeg();
        var metadata = ImageMetadataReader.readMetadata(new ByteArrayInputStream(bytes));
        assertNotNull(metadata.getFirstDirectoryOfType(GpsDirectory.class));
        assertEquals("QA-Cam", metadata.getFirstDirectoryOfType(ExifIFD0Directory.class).getString(ExifIFD0Directory.TAG_MAKE));
        var storage = new PhotoStorage(new PhotoProperties(directory.toString()));
        var stored = storage.store(new MockMultipartFile("arquivo", "camera.jpg", "image/jpeg", bytes));
        assertSanitized(storage.read(stored.key()));
        assertSanitized(storage.read(stored.thumbnailKey()));
        assertEquals(storage.read(stored.key()).length, stored.size());
    }

    @Test
    void legacyDisplayRemovesMetadataWithoutOverwritingStoredFile() throws Exception {
        byte[] bytes = PhotoFixture.cameraJpeg();
        java.nio.file.Files.write(directory.resolve("legacy.jpg"), bytes);
        var storage = new PhotoStorage(new PhotoProperties(directory.toString()));
        assertSanitized(storage.readDisplay("legacy.jpg").bytes());
        assertArrayEquals(bytes, java.nio.file.Files.readAllBytes(directory.resolve("legacy.jpg")));
    }

    @Test
    void decodesWebpAndStoresMetadataFreePngWithThumbnail() throws Exception {
        byte[] bytes = java.util.Base64.getDecoder().decode("UklGRiIAAABXRUJQVlA4IBYAAAAwAQCdASoBAAEADsD+JaQAA3AAAAAA");
        var storage = new PhotoStorage(new PhotoProperties(directory.toString()));
        var stored = storage.store(new MockMultipartFile("arquivo", "camera.webp", "image/webp", bytes));
        assertEquals("image/png", stored.contentType());
        assertNotNull(ImageIO.read(new ByteArrayInputStream(storage.read(stored.key()))));
        assertNotNull(stored.thumbnailKey());
        java.nio.file.Files.write(directory.resolve("legacy.webp"), bytes);
        assertEquals("image/png", storage.readDisplay("legacy.webp").contentType());
    }

    @Test
    void rejectsOversizedDimensionsBeforeDecoding() throws Exception {
        var out = new ByteArrayOutputStream();
        ImageIO.write(new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB), "png", out);
        byte[] bytes = out.toByteArray();
        var header = java.nio.ByteBuffer.wrap(bytes);
        header.putInt(16, 10_000); header.putInt(20, 5_000);
        var crc = new java.util.zip.CRC32(); crc.update(bytes, 12, 17); header.putInt(29, (int) crc.getValue());
        var storage = new PhotoStorage(new PhotoProperties(directory.toString()));
        assertThrows(br.com.gestao.oficinas_api.identidade.ApiException.class,
            () -> storage.store(new MockMultipartFile("arquivo", "big.png", "image/png", bytes)));
        try (var files = java.nio.file.Files.list(directory)) { assertEquals(0, files.count()); }
    }

    private void assertSanitized(byte[] bytes) throws Exception {
        var image = ImageIO.read(new ByteArrayInputStream(bytes));
        assertEquals(40, image.getWidth()); assertEquals(80, image.getHeight());
        assertTrue(new Color(image.getRGB(20, 15)).getRed() > 200);
        assertTrue(new Color(image.getRGB(20, 65)).getBlue() > 200);
        var metadata = ImageMetadataReader.readMetadata(new ByteArrayInputStream(bytes));
        assertNull(metadata.getFirstDirectoryOfType(ExifIFD0Directory.class));
        assertNull(metadata.getFirstDirectoryOfType(GpsDirectory.class));
    }

    @Test
    void rejectsCorruptImageWithoutLeavingOriginal() throws Exception {
        var storage = new PhotoStorage(new PhotoProperties(directory.toString()));
        assertThrows(RuntimeException.class, () -> storage.store(new MockMultipartFile("arquivo", "x.jpg", "image/jpeg", new byte[]{-1,-40,-1,0})));
        try (var files = java.nio.file.Files.list(directory)) { assertEquals(0, files.count()); }
    }
}
