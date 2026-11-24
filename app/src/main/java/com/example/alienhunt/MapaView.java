package com.example.alienhunt;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

/**
 * Dibuja el mapa de la estación con los jugadores y avanza la simulación en cada fotograma.
 * Tocar una habitación hace que el jugador se mueva hasta ella.
 *
 * Solo se ven los jugadores y cuerpos que están en la misma habitación que tú; el resto de
 * habitaciones se oscurecen.
 */
public class MapaView extends View {

    private static final float RADIO = 15f; // radio de un jugador, en píxeles del mapa

    private JuegoEstacion juego;
    private final Bitmap mapa;
    private final Rect origen;
    private final RectF destino = new RectF();
    private final RectF tmp = new RectF();

    private final Paint pMapa = new Paint(Paint.FILTER_BITMAP_FLAG);
    private final Paint pRelleno = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint pLinea = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint pTexto = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint pNiebla = new Paint();

    private float escala = 1f, offX, offY;
    private long ultimoFotograma;

    public MapaView(Context context) {
        this(context, null);
    }

    public MapaView(Context context, AttributeSet attrs) {
        super(context, attrs);
        BitmapFactory.Options opciones = new BitmapFactory.Options();
        opciones.inScaled = false;
        mapa = BitmapFactory.decodeResource(getResources(), R.drawable.mapa_estacion, opciones);
        origen = new Rect(0, 0, mapa.getWidth(), mapa.getHeight());

        pLinea.setStyle(Paint.Style.STROKE);
        pTexto.setTextAlign(Paint.Align.CENTER);
        pTexto.setFakeBoldText(true);
        pNiebla.setColor(Color.argb(150, 0, 0, 0));
    }

    public void setJuego(JuegoEstacion juego) {
        this.juego = juego;
        invalidate();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        // El mapa se ajusta a la pantalla manteniendo la proporción, centrado
        escala = Math.min(w / (float) Estacion.ANCHO, h / (float) Estacion.ALTO);
        offX = (w - Estacion.ANCHO * escala) / 2f;
        offY = (h - Estacion.ALTO * escala) / 2f;
        destino.set(offX, offY, offX + Estacion.ANCHO * escala, offY + Estacion.ALTO * escala);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        long ahora = System.nanoTime();
        float dt = ultimoFotograma == 0 ? 0f : (ahora - ultimoFotograma) / 1_000_000_000f;
        ultimoFotograma = ahora;
        dt = Math.min(dt, 0.05f); // evita saltos si la app estuvo en pausa

        canvas.drawColor(Color.BLACK);
        canvas.drawBitmap(mapa, origen, destino, pMapa);

        if (juego != null) {
            juego.tick(dt);
            dibujarNiebla(canvas);
            dibujarDestino(canvas);
            dibujarCuerpos(canvas);
            dibujarJugadores(canvas);
        }
        postInvalidateOnAnimation();
    }

    // ------------------------------------------------------------------ dibujo

    private float sx(float x) { return offX + x * escala; }

    private float sy(float y) { return offY + y * escala; }

    /** Oscurece las habitaciones donde no estás (no puedes ver lo que pasa en ellas). */
    private void dibujarNiebla(Canvas canvas) {
        if (juego.getFase() != JuegoEstacion.Fase.JUGANDO || !juego.getHumano().vivo) return;
        int actual = juego.getHumano().habitacion;
        for (int i = 0; i < Estacion.NUM_HAB; i++) {
            if (i == actual) continue;
            int[] r = Estacion.RECTS[i];
            canvas.drawRect(sx(r[0]), sy(r[1]), sx(r[2]), sy(r[3]), pNiebla);
        }
    }

    private void dibujarDestino(Canvas canvas) {
        int nodo = juego.destinoHumano();
        if (nodo < 0) return;
        pLinea.setColor(Color.argb(200, 166, 255, 61));
        pLinea.setStrokeWidth(3f * escala);
        float x = sx(Estacion.NODOS[nodo][0]);
        float y = sy(Estacion.NODOS[nodo][1]);
        canvas.drawCircle(x, y, 22f * escala, pLinea);
        canvas.drawCircle(x, y, 10f * escala, pLinea);
    }

    private void dibujarCuerpos(Canvas canvas) {
        for (JuegoEstacion.Cuerpo c : juego.getCuerpos()) {
            if (!juego.cuerpoVisible(c)) continue;
            float x = sx(c.x), y = sy(c.y);
            pRelleno.setStyle(Paint.Style.FILL);
            pRelleno.setColor(c.jugador.color);
            tmp.set(x - RADIO * 1.2f * escala, y - RADIO * 0.6f * escala,
                    x + RADIO * 1.2f * escala, y + RADIO * 0.6f * escala);
            canvas.drawOval(tmp, pRelleno);
            pLinea.setColor(Color.RED);
            pLinea.setStrokeWidth(3f * escala);
            float d = RADIO * 0.7f * escala;
            canvas.drawLine(x - d, y - d, x + d, y + d, pLinea);
            canvas.drawLine(x - d, y + d, x + d, y - d, pLinea);
        }
    }

    private void dibujarJugadores(Canvas canvas) {
        pTexto.setTextSize(15f * escala);
        for (Jugador p : juego.getJugadores()) {
            if (!p.vivo || !juego.visibleParaHumano(p)) continue;
            float x = sx(p.x), y = sy(p.y);
            float r = RADIO * escala;

            // cuerpo
            pRelleno.setStyle(Paint.Style.FILL);
            pRelleno.setColor(p.color);
            canvas.drawCircle(x, y, r, pRelleno);
            pLinea.setColor(Color.BLACK);
            pLinea.setStrokeWidth(2.5f * escala);
            canvas.drawCircle(x, y, r, pLinea);

            // visor
            pRelleno.setColor(Color.argb(230, 190, 240, 255));
            tmp.set(x - r * 0.1f, y - r * 0.5f, x + r * 0.8f, y + r * 0.1f);
            canvas.drawOval(tmp, pRelleno);

            // el jugador humano lleva un aro blanco
            if (p.humano) {
                pLinea.setColor(Color.WHITE);
                pLinea.setStrokeWidth(3f * escala);
                canvas.drawCircle(x, y, r + 5f * escala, pLinea);
            }

            // nombre (con sombra para que se lea sobre el mapa)
            pTexto.setColor(Color.BLACK);
            canvas.drawText(p.nombre, x + 1.5f, y - r - 6f * escala + 1.5f, pTexto);
            pTexto.setColor(Color.WHITE);
            canvas.drawText(p.nombre, x, y - r - 6f * escala, pTexto);
        }
    }

    // ------------------------------------------------------------------ toques

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        if (e.getAction() == MotionEvent.ACTION_UP && juego != null) {
            float x = (e.getX() - offX) / escala;
            float y = (e.getY() - offY) / escala;
            juego.moverHumano(x, y);
            performClick();
        }
        return true; // hay que devolver true en ACTION_DOWN para recibir el ACTION_UP
    }

    @Override
    public boolean performClick() {
        return super.performClick();
    }
}
