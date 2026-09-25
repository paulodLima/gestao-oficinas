package br.com.gestao.oficinas_api.support;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import javax.imageio.ImageIO;

public final class PhotoFixture {
    private PhotoFixture() {}
    public static byte[] cameraJpeg() throws Exception {
        var source = new BufferedImage(80, 40, BufferedImage.TYPE_INT_RGB);
        var graphics = source.createGraphics();
        graphics.setColor(Color.RED); graphics.fillRect(0, 0, 40, 40);
        graphics.setColor(Color.BLUE); graphics.fillRect(40, 0, 40, 40); graphics.dispose();
        var jpeg = new ByteArrayOutputStream(); ImageIO.write(source, "jpg", jpeg);
        var tiff = ByteBuffer.allocate(112).order(ByteOrder.LITTLE_ENDIAN);
        tiff.putShort((short) 0x4949).putShort((short) 42).putInt(8).putShort((short) 3);
        tiff.putShort((short) 0x0112).putShort((short) 3).putInt(1).putInt(6);
        tiff.putShort((short) 0x010f).putShort((short) 2).putInt(7).putInt(50);
        tiff.putShort((short) 0x8825).putShort((short) 4).putInt(1).putInt(58).putInt(0);
        tiff.put("QA-Cam\0".getBytes(StandardCharsets.US_ASCII));
        tiff.position(58); tiff.putShort((short) 2);
        tiff.putShort((short) 1).putShort((short) 2).putInt(2).putInt('N');
        tiff.putShort((short) 2).putShort((short) 5).putInt(3).putInt(88).putInt(0);
        tiff.putInt(12).putInt(1).putInt(34).putInt(1).putInt(56).putInt(1);
        var bytes = new ByteArrayOutputStream(); bytes.write(jpeg.toByteArray(), 0, 2);
        bytes.write(new byte[]{(byte) 0xff, (byte) 0xe1, 0, 120, 'E', 'x', 'i', 'f', 0, 0});
        bytes.write(tiff.array()); bytes.write(jpeg.toByteArray(), 2, jpeg.size() - 2);
        return bytes.toByteArray();
    }
}
