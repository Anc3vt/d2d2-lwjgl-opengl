package com.ancevt.d2d2.engine.desktop.render;

import com.ancevt.d2d2.engine.desktop.AwtBitmapTextDrawHelper;
import com.ancevt.d2d2.scene.Color;
import com.ancevt.d2d2.scene.text.BitmapCharInfo;
import com.ancevt.d2d2.scene.text.BitmapFont;
import com.ancevt.d2d2.scene.text.BitmapText;
import com.ancevt.d2d2.scene.texture.Texture;

import java.nio.FloatBuffer;

class BitmapTextDrawInfo implements DrawInfo {
    private final BitmapText text;
    private final float a, b, c, d, e, f;
    private final float alpha;

    public BitmapTextDrawInfo(BitmapText text, float a, float b, float c, float d, float e, float f, float alpha) {
        this.text = text;
        this.a = a;
        this.b = b;
        this.c = c;
        this.d = d;
        this.e = e;
        this.f = f;
        this.alpha = alpha;
    }

    @Override
    public ShaderProgramImpl getCustomShader() {
        return (ShaderProgramImpl) text.getShaderProgram();
    }

    @Override
    public int getTextureId() {
        return text.getBitmapFont().getTexture().getId();
    }

    @Override
    public int render(FloatBuffer buffer, DesktopRenderer renderer) {
        if (text.isEmpty()) return 0;

        int glyphCount = 0;

        BitmapFont font = text.getBitmapFont();
        Texture texture = font.getTexture();

        float texW = texture.getWidth();
        float texH = texture.getHeight();

        double tf = text.getTextureBleedingFix();
        double vf = text.getVertexBleedingFix();

        float spacing = text.getSpacing();
        float lineSpacing = text.getLineSpacing();
        float scaleX = text.getScaleX();
        float scaleY = text.getScaleY();

        float boundWidth = text.getWidth() + 10; // sry bout mag num
        float boundHeight = text.getHeight();

        float cursorX = 0f;
        float cursorY = font.getPaddingTop() * scaleY;

        String content = text.getPlainText();
        BitmapText.ColorTextData colorData = text.isMulticolor() ? text.getColorTextData() : null;

        for (int i = 0; i < content.length(); i++) {
            char ch = content.charAt(i);

            if (ch == '\n') {
                cursorX = 0f;
                cursorY += (font.getZeroCharHeight() + lineSpacing) * scaleY;
                continue;
            }

            // Word wrap
            if (text.isWordWrap() && boundWidth > 0) {
                String nextWord =
                        AwtBitmapTextDrawHelper.getNextWord(content, i);
                float nextWordWidth = AwtBitmapTextDrawHelper.meterStringWidth(text, nextWord) * scaleX;
                if (cursorX + nextWordWidth >= boundWidth) {
                    cursorX = 0f;
                    cursorY += (font.getZeroCharHeight() + lineSpacing) * scaleY;
                    if (boundHeight > 0 && cursorY > boundHeight - font.getZeroCharHeight()) break;
                }
            }

            BitmapCharInfo charInfo = font.getCharInfo(ch);
            if (charInfo == null) continue;

            float charW = charInfo.width();
            float charH = charInfo.height();

            if (cursorX + charW > boundWidth && !text.isWordWrap()) continue;

            float u0 = charInfo.x() / texW;
            float v0 = (texH - charInfo.y()) / texH;
            float u1 = (charInfo.x() + charInfo.width()) / texW;
            float v1 = (texH - (charInfo.y() + charInfo.height())) / texH;

            float x = cursorX;
            float y = cursorY;

            float px = a * x + b * y + c;
            float py = d * x + e * y + f;
            float px1 = a * (x + charW) + b * y + c;
            float py1 = d * (x + charW) + e * y + f;
            float px2 = a * (x + charW) + b * (y + charH) + c;
            float py2 = d * (x + charW) + e * (y + charH) + f;
            float px3 = a * x + b * (y + charH) + c;
            float py3 = d * x + e * (y + charH) + f;

            float r, g, bCol;
            if (text.isMulticolor()) {
                Color color = colorData.getColoredLetter(i).getColor();
                r = color.getR() / 255f;
                g = color.getG() / 255f;
                bCol = color.getB() / 255f;
            } else {
                Color color = text.getColor();
                r = color.getR() / 255f;
                g = color.getG() / 255f;
                bCol = color.getB() / 255f;
            }

            buffer.put(new float[]{
                    px - (float) vf, py - (float) vf, u0 + (float) tf, v0 - (float) tf, r, g, bCol, alpha,
                    px1 + (float) vf, py1 - (float) vf, u1 - (float) tf, v0 - (float) tf, r, g, bCol, alpha,
                    px2 + (float) vf, py2 + (float) vf, u1 - (float) tf, v1 + (float) tf, r, g, bCol, alpha,
                    px3 - (float) vf, py3 + (float) vf, u0 + (float) tf, v1 + (float) tf, r, g, bCol, alpha
            });

            glyphCount++;

            cursorX += (charInfo.width() + spacing);

            if (boundHeight > 0 && cursorY > boundHeight - charH) break;
        }

        return glyphCount;
    }

}
