/**
 * Copyright (C) 2024 the original author or authors.
 * See the notice.md file distributed with this work for additional
 * information regarding copyright ownership.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.ancevt.d2d2.engine.lwjgl;

import com.ancevt.d2d2.display.Color;
import com.ancevt.d2d2.display.text.BitmapCharInfo;
import com.ancevt.d2d2.display.text.Font;
import com.ancevt.d2d2.display.text.Text;
import com.ancevt.d2d2.display.texture.Texture;


class BitmapTextDrawHelper {

    static void draw(Text bitmapText,
                     float alpha,
                     float scaleX,
                     float scaleY,
                     DrawCharFunction drawCharFunction,
                     ApplyColorFunction applyColorFunction) {

        Font font = bitmapText.getFont();
        Texture texture = font.getTexture();

        int textureWidth = texture.getWidth();
        int textureHeight = texture.getHeight();

        float lineSpacing = bitmapText.getLineSpacing();
        float spacing = bitmapText.getSpacing();

        float boundWidth = bitmapText.getWidth() * scaleX;
        float boundHeight = bitmapText.getHeight() * scaleY;

        float drawX = 0;
        float drawY = font.getPaddingTop() * scaleY;

        double textureBleedingFix = bitmapText.getTextureBleedingFix();
        double vertexBleedingFix = bitmapText.getVertexBleedingFix();

        boolean wordWrap = bitmapText.isWordWrap();
        boolean multicolor = bitmapText.isMulticolor();

        String text = bitmapText.getText();

        float nextWordWidth;

        Text.ColorTextData colorTextData = multicolor ? bitmapText.getColorTextData() : null;

        for (int i = 0; multicolor ? i < colorTextData.length() : i < text.length(); i++) {
            Text.ColorTextData.Letter letter = null;

            if (multicolor) {
                letter = colorTextData.getColoredLetter(i);

                Color letterColor = letter.getColor();

                if (applyColorFunction != null) {
                    applyColorFunction.applyColor(
                        letterColor.getR() / 255f,
                        letterColor.getG() / 255f,
                        letterColor.getB() / 255f,
                        alpha
                    );
                }
            }

            char c = multicolor ? letter.getCharacter() : text.charAt(i);

            if (wordWrap && isSpecialCharacter(c)) {
                nextWordWidth = getNextWordWidth(bitmapText, i, scaleX);
            } else {
                nextWordWidth = 0f;
            }

            BitmapCharInfo charInfo = font.getCharInfo(c);

            if (charInfo == null) continue;

            if (charInfo.character() == ' ') {
                drawX += font.getZeroCharWidth();
                continue;
            }

            float charWidth = charInfo.width();
            float charHeight = charInfo.height();

            if (c == '\n' || wordWrap && (boundWidth != 0 && drawX >= boundWidth - nextWordWidth - charWidth / 1.5f * scaleX)) {
                drawX = 0;
                drawY += (charHeight + lineSpacing) * scaleY;

                if (boundHeight != 0 && drawY > boundHeight - charHeight) {
                    break;
                }

                if (nextWordWidth > 0) {
                    continue;
                }
            }

            if (!wordWrap && drawX >= boundWidth - charWidth / 1.5f) {
                continue;
            }

            drawCharFunction.drawChar(
                texture,
                c,
                letter, // null if not multicolor
                drawX,
                (drawY + scaleY * charHeight),
                textureWidth,
                textureHeight,
                charInfo,
                scaleX,
                scaleY,
                textureBleedingFix,
                vertexBleedingFix
            );

            drawX += (charWidth + (c != '\n' ? spacing : 0)) * scaleX;
        }


    }

    private static float getNextWordWidth(Text text, int charIndex, float scaleX) {
        String nextWord = getNextWord(text.getPlainText(), charIndex);
        if (nextWord.length() > 0) {
            char firstChar = nextWord.charAt(0);
            if (!Character.isLetterOrDigit(firstChar) && firstChar != '_') return 0f;
        }
        return meterStringWidth(text, nextWord) * scaleX;
    }

    public static String getNextWord(String text, int charIndexFrom) {
        StringBuilder word = new StringBuilder();
        boolean inWord = false;

        // Начинаем поиск слова с указанного индекса
        for (int i = charIndexFrom; i < text.length(); i++) {
            char ch = text.charAt(i);

            // Проверяем, является ли текущий символ допустимым для слова
            if (isWordCharacter(ch)) {
                word.append(ch);
                inWord = true;
            } else {
                // Если уже начали собирать слово и текущий символ не подходит, завершаем сбор слова
                if (inWord) {
                    break;
                }
                // Если еще не начали собирать слово, продолжаем пропускать символы
                continue;
            }
        }

        return word.toString();
    }

    // Метод для проверки символа на принадлежность к допустимым символам слова
    private static boolean isWordCharacter(char ch) {
        return Character.isLetterOrDigit(ch) ||
            ch == '!' || ch == '_' || ch == '.' ||
            ch == ':' || ch == ';' || ch == ',';
    }

    private static float meterStringWidth(Text text, String string) {
        float result = 0f;

        Font font = text.getFont();

        for (char c : string.toCharArray()) {
            BitmapCharInfo charInfo = font.getCharInfo(c);
            result += charInfo.width() + text.getSpacing();
        }

        return result;
    }

    private static boolean isSpecialCharacter(char ch) {
        return !Character.isLetterOrDigit(ch) && ch != '_';
    }

    @FunctionalInterface
    interface DrawCharFunction {

        void drawChar(
            Texture atlas,
            char c,
            Text.ColorTextData.Letter letter,
            float x,
            float y,
            int textureWidth,
            int textureHeight,
            BitmapCharInfo charInfo,
            float scX,
            float scY,
            double textureBleedingFix,
            double vertexBleedingFix);
    }

    @FunctionalInterface
    interface ApplyColorFunction {
        void applyColor(float r, float g, float b, float alpha);
    }
}
