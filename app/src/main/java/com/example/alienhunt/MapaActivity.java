package com.example.alienhunt;

import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;
import androidx.core.content.ContextCompat;

import java.util.List;
import java.util.Random;

/**
 * Partida: mapa de la estación con 10 jugadores (tú y 9 NPC), uno de ellos el alien.
 * La lógica está en {@link JuegoEstacion}; aquí solo se muestran botones, diálogos y la reunión.
 */
public class MapaActivity extends AppCompatActivity implements JuegoEstacion.Oyente {

    private JuegoEstacion juego;

    private TextView txtHud;
    private TextView txtRegistro;
    private ScrollView scrollRegistro;
    private LinearLayout listaJugadores;
    private View panelReunion;
    private Button btnReportar;
    private Button btnEmergencia;
    private Button btnHablar;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private int dialogosAbiertos;
    private boolean votoEmitido;
    private boolean finMostrado;

    private final Runnable refresco = new Runnable() {
        @Override
        public void run() {
            actualizarHud();
            handler.postDelayed(this, 250);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        UiUtils.configurarPantalla(this);
        setContentView(R.layout.activity_mapa);
        UiUtils.aplicarInsets(findViewById(R.id.main));
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        txtHud = findViewById(R.id.txtHud);
        txtRegistro = findViewById(R.id.txtRegistro);
        scrollRegistro = findViewById(R.id.scrollRegistro);
        listaJugadores = findViewById(R.id.listaJugadores);
        panelReunion = findViewById(R.id.panelReunion);
        btnReportar = findViewById(R.id.btnReportar);
        btnEmergencia = findViewById(R.id.btnEmergencia);
        btnHablar = findViewById(R.id.btnHablar);

        // El jugador usa el nombre de su perfil
        String nombre = new PerfilStorage(this).cargarPerfil().getNombre();
        juego = new JuegoEstacion(nombre, new Random(), this);
        ((MapaView) findViewById(R.id.mapaView)).setJuego(juego);

        btnReportar.setOnClickListener(v -> juego.reportarCuerpo());
        btnEmergencia.setOnClickListener(v -> juego.llamarEmergencia());
        btnHablar.setOnClickListener(v -> abrirHablar());
        findViewById(R.id.btnCuaderno).setOnClickListener(v -> abrirCuaderno());
        findViewById(R.id.btnCuadernoReunion).setOnClickListener(v -> abrirCuaderno());
        findViewById(R.id.btnSaltar).setOnClickListener(v -> votar(-1));

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                mostrar(new AlertDialog.Builder(MapaActivity.this)
                        .setTitle("¿Salir de la partida?")
                        .setMessage("Se perderá el progreso de esta partida.")
                        .setPositiveButton("SALIR", (d, w) -> finish())
                        .setNegativeButton("SEGUIR", null));
            }
        });

        mostrarIntro();
    }

    @Override
    protected void onResume() {
        super.onResume();
        handler.post(refresco);
        if (dialogosAbiertos == 0) juego.setPausado(false);
    }

    @Override
    protected void onPause() {
        super.onPause();
        handler.removeCallbacks(refresco);
        juego.setPausado(true);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacksAndMessages(null);
    }

    // ------------------------------------------------------------------ Oyente (lógica -> pantalla)

    @Override
    public void alIniciarReunion() {
        handler.post(this::mostrarReunion);
    }

    @Override
    public void alTerminar(boolean victoria, String mensaje) {
        handler.post(() -> {
            // Si el final llega al votar, se muestra al cerrar el resultado de la votación
            if (panelReunion.getVisibility() == View.VISIBLE) return;
            mostrarFin();
        });
    }

    // ------------------------------------------------------------------ HUD

    private void actualizarHud() {
        txtHud.setText("EN: " + juego.nombreHabitacionHumano()
                + "   •   TRIPULANTES VIVOS: " + juego.tripulantesVivos() + "/9"
                + "   •   ERES TRIPULANTE");

        btnReportar.setVisibility(juego.hayCuerpoCerca() ? View.VISIBLE : View.GONE);

        boolean emergencia = juego.puedeEmergencia();
        btnEmergencia.setVisibility(emergencia ? View.VISIBLE : View.GONE);
        if (emergencia) {
            btnEmergencia.setText(getString(R.string.btn_emergencia)
                    + " (" + juego.emergenciasRestantes() + ")");
        }

        btnHablar.setVisibility(juego.jugadoresCercaDelHumano().isEmpty() ? View.GONE : View.VISIBLE);
    }

    // ------------------------------------------------------------------ diálogos (pausan la partida)

    private void mostrar(AlertDialog.Builder builder) {
        AlertDialog dialogo = builder.create();
        dialogosAbiertos++;
        juego.setPausado(true);
        dialogo.setOnDismissListener(d -> {
            dialogosAbiertos--;
            if (dialogosAbiertos == 0) juego.setPausado(false);
        });
        dialogo.show();
    }

    private void mostrarIntro() {
        mostrar(new AlertDialog.Builder(this)
                .setTitle("MISIÓN: ENCUENTRA AL ALIEN")
                .setMessage("Entre los 9 tripulantes de la estación se esconde un alien. "
                        + "Ataca a quien se queda a solas con él.\n\n"
                        + "• Toca una habitación para ir hacia ella.\n"
                        + "• Solo ves a quien está en tu misma habitación.\n"
                        + "• HABLAR: pregunta a quien esté contigo. Los tripulantes dicen lo que han "
                        + "visto de verdad; el alien miente.\n"
                        + "• CUADERNO: revisa a quién has visto y qué te han contado.\n"
                        + "• REPORTAR si ves un cuerpo, o EMERGENCIA en el Centro de Control.\n"
                        + "• En la reunión, pregunta a todos y vota para expulsar al alien.")
                .setCancelable(false)
                .setPositiveButton("EMPEZAR", null));
    }

    private void abrirCuaderno() {
        mostrar(new AlertDialog.Builder(this)
                .setTitle("CUADERNO")
                .setMessage(juego.cuaderno())
                .setPositiveButton("Cerrar", null));
    }

    private void abrirHablar() {
        final List<Jugador> cerca = juego.jugadoresCercaDelHumano();
        if (cerca.isEmpty()) {
            Toast.makeText(this, R.string.msg_nadie_cerca, Toast.LENGTH_SHORT).show();
            return;
        }
        String[] nombres = new String[cerca.size()];
        for (int i = 0; i < nombres.length; i++) nombres[i] = cerca.get(i).nombre;
        mostrar(new AlertDialog.Builder(this)
                .setTitle("¿A quién quieres hablar?")
                .setItems(nombres, (d, i) -> elegirPregunta(cerca.get(i)))
                .setNegativeButton("Cancelar", null));
    }

    private void elegirPregunta(final Jugador objetivo) {
        mostrar(new AlertDialog.Builder(this)
                .setTitle(objetivo.nombre + ": ¿qué le preguntas?")
                .setItems(JuegoEstacion.PREGUNTAS, (d, tipo) -> {
                    String respuesta = juego.preguntar(objetivo, tipo);
                    if (juego.getFase() == JuegoEstacion.Fase.REUNION) {
                        actualizarRegistro(); // la respuesta ya está en el registro de la reunión
                    } else {
                        mostrarRespuesta(objetivo, tipo, respuesta);
                    }
                })
                .setNegativeButton("Cancelar", null));
    }

    private void mostrarRespuesta(Jugador objetivo, int tipo, String respuesta) {
        mostrar(new AlertDialog.Builder(this)
                .setTitle(objetivo.nombre)
                .setMessage("«" + JuegoEstacion.PREGUNTAS[tipo] + "»\n\n" + respuesta
                        + "\n\n(Queda anotado en tu cuaderno)")
                .setPositiveButton("Entendido", null));
    }

    // ------------------------------------------------------------------ reunión y votación

    private void mostrarReunion() {
        votoEmitido = false;
        panelReunion.setVisibility(View.VISIBLE);
        actualizarRegistro();
        construirLista();
    }

    private void actualizarRegistro() {
        txtRegistro.setText(TextUtils.join("\n", juego.getRegistro()));
        scrollRegistro.post(() -> scrollRegistro.fullScroll(View.FOCUS_DOWN));
    }

    private void construirLista() {
        listaJugadores.removeAllViews();
        for (final Jugador p : juego.getJugadores()) {
            if (p.humano || !p.vivo) continue;

            LinearLayout fila = new LinearLayout(this);
            fila.setOrientation(LinearLayout.HORIZONTAL);
            fila.setGravity(Gravity.CENTER_VERTICAL);
            int pad = UiUtils.dp(listaJugadores, 3);
            fila.setPadding(0, pad, 0, pad);

            TextView nombre = new TextView(this);
            nombre.setText("● " + p.nombre);
            nombre.setTextColor(p.color);
            nombre.setTextSize(14);
            nombre.setTypeface(nombre.getTypeface(), android.graphics.Typeface.BOLD);
            fila.addView(nombre, new LinearLayout.LayoutParams(0,
                    LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

            fila.addView(crearBoton("PREGUNTAR", Color.WHITE, v -> elegirPregunta(p)));
            fila.addView(crearBoton("VOTAR", ContextCompat.getColor(this, R.color.acid),
                    v -> votar(p.id)));
            listaJugadores.addView(fila);
        }
    }

    private Button crearBoton(String texto, int color, View.OnClickListener accion) {
        AppCompatButton boton = new AppCompatButton(this);
        boton.setText(texto);
        boton.setTextSize(12);
        boton.setTextColor(color);
        boton.setBackgroundResource(R.drawable.bg_button);
        boton.setMinHeight(0);
        boton.setMinimumHeight(0);
        boton.setMinWidth(0);
        boton.setMinimumWidth(0);
        int h = UiUtils.dp(listaJugadores, 12);
        int v = UiUtils.dp(listaJugadores, 6);
        boton.setPadding(h, v, h, v);
        boton.setOnClickListener(accion);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.setMarginStart(UiUtils.dp(listaJugadores, 6));
        boton.setLayoutParams(lp);
        return boton;
    }

    /** idVoto = -1 para saltar el voto. */
    private void votar(int idVoto) {
        if (votoEmitido || juego.getFase() != JuegoEstacion.Fase.REUNION) return;
        votoEmitido = true;
        String resultado = juego.votar(idVoto);
        mostrar(new AlertDialog.Builder(this)
                .setTitle("RESULTADO DE LA VOTACIÓN")
                .setMessage(resultado)
                .setCancelable(false)
                .setPositiveButton("CONTINUAR", (d, w) -> {
                    panelReunion.setVisibility(View.GONE);
                    if (juego.getFase() == JuegoEstacion.Fase.FIN) mostrarFin();
                }));
    }

    // ------------------------------------------------------------------ fin de partida

    private void mostrarFin() {
        if (finMostrado) return;
        finMostrado = true;
        panelReunion.setVisibility(View.GONE);
        boolean victoria = juego.esVictoria();
        mostrar(new AlertDialog.Builder(this)
                .setTitle(victoria ? "¡VICTORIA!" : "DERROTA")
                .setMessage(juego.getMensajeFin())
                .setCancelable(false)
                .setPositiveButton("JUGAR DE NUEVO", (d, w) -> recreate())
                .setNegativeButton("SALIR", (d, w) -> finish()));
    }
}
