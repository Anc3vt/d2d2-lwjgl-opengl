package com.ancevt.d2d2.engine.lwjgl.util.texture;

import com.ancevt.d2d2.D2D2;
import com.ancevt.d2d2.asset.Assets;
import com.ancevt.d2d2.display.texture.TextureAtlas;
import org.lwjgl.BufferUtils;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

import static org.lwjgl.opengl.GL11.GL_NEAREST;
import static org.lwjgl.opengl.GL11.GL_RGBA;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_2D;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_MAG_FILTER;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_MIN_FILTER;
import static org.lwjgl.opengl.GL11.GL_UNPACK_ALIGNMENT;
import static org.lwjgl.opengl.GL11.GL_UNSIGNED_BYTE;
import static org.lwjgl.opengl.GL11.glBindTexture;
import static org.lwjgl.opengl.GL11.glGenTextures;
import static org.lwjgl.opengl.GL11.glPixelStorei;
import static org.lwjgl.opengl.GL11.glTexImage2D;
import static org.lwjgl.opengl.GL11.glTexParameteri;
import static org.lwjgl.opengl.GL30.glGenerateMipmap;

public class TextureLoader {

    public static TextureAtlas createTextureAtlas(InputStream pngInputStream) {
        try {
            BufferedImage bufferedImage = ImageIO.read(pngInputStream);
            return createTextureAtlasFromBufferedImage(bufferedImage);
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    public static TextureAtlas createTextureAtlas(String assetPath) {
        try {
            InputStream pngInputStream = Assets.getAssetAsStream(assetPath);
            return createTextureAtlasFromBufferedImage(ImageIO.read(pngInputStream));
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    public static TextureAtlas createTextureAtlasFromBufferedImage(BufferedImage image) {
        int width = image.getWidth();
        int height = image.getHeight();

        int[] pixels = new int[image.getWidth() * image.getHeight()];
        image.getRGB(0, 0, image.getWidth(), image.getHeight(), pixels, 0, image.getWidth());

        ByteBuffer byteBuffer = BufferUtils.createByteBuffer(image.getWidth() * image.getHeight() * 4); //4 for RGBA, 3 for RGB

        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                int pixel = pixels[y * image.getWidth() + x];
                byteBuffer.put((byte) ((pixel >> 16) & 0xFF));     // Red component
                byteBuffer.put((byte) ((pixel >> 8) & 0xFF));      // Green component
                byteBuffer.put((byte) (pixel & 0xFF));             // Blue component
                byteBuffer.put((byte) ((pixel >> 24) & 0xFF));     // Alpha component. Only for RGBA
            }
        }

        byteBuffer.flip();

        TextureAtlas textureAtlas = createTextureAtlasFromByteBuffer(byteBuffer, width, height);
        D2D2.textureManager().addTextureAtlas(textureAtlas);
        return textureAtlas;
    }

    private static TextureAtlas createTextureAtlasFromByteBuffer(ByteBuffer byteBuffer, int width, int height) {
        int textureId = glGenTextures();
        TextureAtlas textureAtlas = new TextureAtlas(textureId, width, height);
        glBindTexture(GL_TEXTURE_2D, textureId);
        glPixelStorei(GL_UNPACK_ALIGNMENT, 1);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, width, height, 0, GL_RGBA, GL_UNSIGNED_BYTE, byteBuffer);
        glGenerateMipmap(GL_TEXTURE_2D);
        return textureAtlas;
    }

}
