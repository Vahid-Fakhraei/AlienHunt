package com.example.alienhunt;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;
import androidx.annotation.NonNull;

/**
 * Arte pixel art del emblema: la silueta del alien (verde ácido, con dientes)
 * enfrentada a la máscara del predator (naranja térmico, con visor
 * brillante y dreadlocks), a partir de una matriz de texto.
 *
 * Códigos de la matriz:
 *   .  vacío
 *   A  alien (color principal)
 *   P  predator (color principal)
 *   M  predator (mechones / sombra de máscara)
 *   T  dientes del alien (brillo)
 *   E  visor del predator (brillo)
 */
public class PixelEmblemView extends View {

    private static final String[] ROWS = {
        "................................",
        "................................",
        ".......AAA......................",
        "......AAAAA.....................",
        ".....AAAAAA.....................",
        ".....AAAAAAA....................",
        "......AAAAAAA...................",
        ".......AAAAAAA..................",
        "........AAAAAA..................",
        "........AAAAAAA.................",
        ".......AAAAAAAA.................",
        "......AAAAAAAA..................",
        ".....AAAAAAA....................",
        "....AAAAAAA.....................",
        "....AAAAAA......................",
        "....AAAAA.......................",
        "....AAAA........................",
        ".....AA..................MMM....",
        ".....TT....................MMM..",
        ".............................MM.",
        "...................PPP..M.......",
        "..................PPPPPP........",
        ".................PPPPPPPP.......",
        "................PPPEPPPPP.......",
        "................PP...PPPP.......",
        "................PP....PPP.......",
        "...............PPP....PPP.......",
        "...............PP.....PPP.......",
        "................PP...PPP........",
        ".................PPPPPP.........",
        "..................PPPP..........",
        "...................PP..........."
    };

    private final Paint acidPaint = new Paint();
    private final Paint acidShadowPaint = new Paint();
    private final Paint thermalPaint = new Paint();
    private final Paint thermalShadowPaint = new Paint();
    private final Paint dreadPaint = new Paint();
    private final Paint teethPaint = new Paint();
    private final Paint visorPaint = new Paint();

    public PixelEmblemView(Context context) {
        super(context);
        init();
    }

    public PixelEmblemView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        acidPaint.setColor(Color.parseColor("#A6FF3D"));
        acidShadowPaint.setColor(Color.parseColor("#2E4A14"));
        thermalPaint.setColor(Color.parseColor("#FF6A1A"));
        thermalShadowPaint.setColor(Color.parseColor("#5C2A0A"));
        dreadPaint.setColor(Color.parseColor("#B34A12"));
        teethPaint.setColor(Color.parseColor("#F2FFE9"));
        visorPaint.setColor(Color.parseColor("#FFE9A6"));
        for (Paint p : new Paint[]{acidPaint, acidShadowPaint, thermalPaint,
                thermalShadowPaint, dreadPaint, teethPaint, visorPaint}) {
            p.setAntiAlias(false);
        }
    }

    private Paint paintFor(char c) {
        switch (c) {
            case 'A': return acidPaint;
            case 'P': return thermalPaint;
            case 'M': return dreadPaint;
            case 'T': return teethPaint;
            case 'E': return visorPaint;
            default:  return null;
        }
    }

    /** Sombra que se usa para dar profundidad a cada tipo de pixel (contorno desfasado). */
    private Paint shadowFor(char c) {
        switch (c) {
            case 'A': return acidShadowPaint;
            case 'P':
            case 'M': return thermalShadowPaint;
            default:  return null; // dientes/visor no llevan sombra: son brillo puro
        }
    }

    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        super.onDraw(canvas);
        int cols = ROWS[0].length();
        int rows = ROWS.length;
        float cell = Math.min(getWidth() / (float) cols, getHeight() / (float) rows);

        float offsetX = (getWidth() - cell * cols) / 2f;
        float offsetY = (getHeight() - cell * rows) / 2f;

        // 1) Capa de sombra: cada pixel proyecta un pixel oscuro 1px abajo-derecha,
        //    técnica clásica de pixel art para dar volumen sin redibujar la silueta.
        for (int y = 0; y < rows; y++) {
            String row = ROWS[y];
            for (int x = 0; x < row.length(); x++) {
                char c = row.charAt(x);
                Paint shadow = shadowFor(c);
                if (shadow == null) continue;
                float left = offsetX + (x + 1) * cell;
                float top = offsetY + (y + 1) * cell;
                canvas.drawRect(left, top, left + cell, top + cell, shadow);
            }
        }

        // 2) Capa principal: color real de cada pixel encima de la sombra.
        for (int y = 0; y < rows; y++) {
            String row = ROWS[y];
            for (int x = 0; x < row.length(); x++) {
                char c = row.charAt(x);
                if (c == '.') continue;
                Paint paint = paintFor(c);
                if (paint == null) continue;
                float left = offsetX + x * cell;
                float top = offsetY + y * cell;
                canvas.drawRect(left, top, left + cell, top + cell, paint);
            }
        }
    }
}
