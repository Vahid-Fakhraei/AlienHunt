package com.example.alienhunt;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

/**
 * Pantalla principal del jugador una vez creado el perfil.
 * El botón JUGAR es el punto donde se conectará la partida real.
 */
public class GameActivity extends AppCompatActivity {

    private PerfilStorage storage;
    private ImageView imgAvatar;
    private TextView txtJugador;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        UiUtils.configurarPantalla(this);
        setContentView(R.layout.activity_game);
        UiUtils.aplicarInsets(findViewById(R.id.main));

        storage = new PerfilStorage(this);
        imgAvatar = findViewById(R.id.imgAvatar);
        txtJugador = findViewById(R.id.txtJugador);

        findViewById(R.id.btnJugar).setOnClickListener(v ->
                startActivity(new Intent(this, MapaActivity.class)));

        findViewById(R.id.btnCambiarPerfil).setOnClickListener(v ->
                startActivity(new Intent(this, CrearPerfilActivity.class)));

        // Volver al menú principal
        findViewById(R.id.btnVolverMenu).setOnClickListener(v -> UiUtils.irAlMenu(this));

        // Cerrar sesión: vuelve al Login y limpia la pila de pantallas
        findViewById(R.id.btnLogout).setOnClickListener(v -> UiUtils.cerrarSesion(this));
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Si por algún motivo no hay perfil, se vuelve a pedir
        if (!storage.existePerfil()) {
            startActivity(new Intent(this, CrearPerfilActivity.class));
            finish();
            return;
        }
        PerfilUsuario perfil = storage.cargarPerfil();
        txtJugador.setText(perfil.getNombre());
        imgAvatar.setImageResource(Avatares.recursoDe(perfil.getAvatar()));
    }
}
