package com.example.alienhunt;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.LinearSnapHelper;
import androidx.recyclerview.widget.RecyclerView;

/**
 * Crear (o editar) el perfil: nombre de jugador + avatar elegido en un carrusel.
 */
public class CrearPerfilActivity extends AppCompatActivity {

    private static final String KEY_SELECCION = "avatar_seleccionado";
    private static final int ANCHO_ITEM_DP = 120; // debe coincidir con item_avatar.xml

    private PerfilStorage storage;
    private EditText edtNombre;
    private TextView txtSeleccionado;
    private RecyclerView recyclerAvatares;
    private LinearLayoutManager layoutManager;
    private LinearSnapHelper snapHelper;
    private AvatarAdapter adapter;

    /** true si al abrir esta pantalla ya existía un perfil (modo edición). */
    private boolean editando;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        UiUtils.configurarPantalla(this);
        setContentView(R.layout.activity_crear_perfil);
        UiUtils.aplicarInsets(findViewById(R.id.main));

        storage = new PerfilStorage(this);
        editando = storage.existePerfil();

        TextView txtTitulo = findViewById(R.id.txtTitulo);
        edtNombre = findViewById(R.id.edtNombre);
        txtSeleccionado = findViewById(R.id.txtSeleccionado);
        recyclerAvatares = findViewById(R.id.recyclerAvatares);
        TextView btnGuardar = findViewById(R.id.btnCrear);

        int seleccionInicial = 0;
        if (savedInstanceState != null) {
            seleccionInicial = savedInstanceState.getInt(KEY_SELECCION, 0);
        } else if (editando) {
            PerfilUsuario perfil = storage.cargarPerfil();
            edtNombre.setText(perfil.getNombre());
            edtNombre.setSelection(edtNombre.length());
            seleccionInicial = Avatares.indiceDe(perfil.getAvatar());
        }

        txtTitulo.setText(editando ? R.string.editar_perfil : R.string.crear_perfil);
        btnGuardar.setText(editando ? R.string.btn_guardar : R.string.btn_crear);

        configurarCarrusel(seleccionInicial);
        btnGuardar.setOnClickListener(v -> guardarPerfil());

        // Volver sin guardar: regresa a la pantalla anterior (menú o juego)
        findViewById(R.id.btnVolver).setOnClickListener(v -> finish());
    }

    private void configurarCarrusel(int seleccionInicial) {
        layoutManager = new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false);
        recyclerAvatares.setLayoutManager(layoutManager);
        recyclerAvatares.setItemAnimator(null); // evita parpadeo al cambiar la selección

        adapter = new AvatarAdapter(Avatares.RECURSOS, seleccionInicial, this::alPulsarAvatar);
        recyclerAvatares.setAdapter(adapter);
        actualizarTextoSeleccion(seleccionInicial);

        // Un elemento queda automáticamente centrado al soltar el dedo
        snapHelper = new LinearSnapHelper();
        snapHelper.attachToRecyclerView(recyclerAvatares);

        // Con padding a ambos lados, el primer y el último avatar también pueden quedar centrados
        recyclerAvatares.post(() -> {
            int padding = Math.max(0,
                    (recyclerAvatares.getWidth() - UiUtils.dp(recyclerAvatares, ANCHO_ITEM_DP)) / 2);
            recyclerAvatares.setPadding(padding, 0, padding, 0);
            layoutManager.scrollToPositionWithOffset(adapter.getSeleccionado(), 0);
        });

        // Cuando el carrusel se detiene, el avatar central pasa a ser el seleccionado
        recyclerAvatares.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
                if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                    View central = snapHelper.findSnapView(layoutManager);
                    if (central != null) {
                        seleccionar(layoutManager.getPosition(central));
                    }
                }
            }
        });
    }

    /** Al tocar un avatar: se selecciona y el carrusel lo lleva al centro. */
    private void alPulsarAvatar(int posicion, View vista) {
        seleccionar(posicion);
        int[] distancia = snapHelper.calculateDistanceToFinalSnap(layoutManager, vista);
        if (distancia != null && distancia[0] != 0) {
            recyclerAvatares.smoothScrollBy(distancia[0], 0);
        }
    }

    private void seleccionar(int posicion) {
        adapter.seleccionar(posicion);
        actualizarTextoSeleccion(adapter.getSeleccionado());
    }

    private void actualizarTextoSeleccion(int posicion) {
        txtSeleccionado.setText(getString(R.string.avatar_seleccionado,
                posicion + 1, Avatares.CLAVES.length));
    }

    private void guardarPerfil() {
        String nombre = edtNombre.getText().toString().trim();
        if (nombre.isEmpty()) {
            edtNombre.setError(getString(R.string.error_nombre_vacio));
            edtNombre.requestFocus();
            return;
        }

        String avatar = Avatares.CLAVES[adapter.getSeleccionado()];
        storage.guardarPerfil(new PerfilUsuario(nombre, avatar));

        if (!editando) {
            // Primera vez: pasamos al juego (el menú queda debajo en la pila)
            startActivity(new Intent(this, GameActivity.class));
        }
        // Si se estaba editando, basta con volver: GameActivity se refresca en onResume
        finish();
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(KEY_SELECCION, adapter.getSeleccionado());
    }
}
