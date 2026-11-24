package com.example.alienhunt;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

/**
 * Menú principal (después del Login). ENTER lleva a crear el perfil si todavía no existe,
 * o directamente al juego si ya hay un perfil guardado.
 */
public class MenuActivity extends AppCompatActivity {

    private PerfilStorage storage;
    private TextView txtEstado;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        UiUtils.configurarPantalla(this);
        setContentView(R.layout.activity_menu);
        UiUtils.aplicarInsets(findViewById(R.id.main));

        storage = new PerfilStorage(this);
        txtEstado = findViewById(R.id.txtEstado);

        findViewById(R.id.btnEnter).setOnClickListener(v -> {
            Class<?> destino = storage.existePerfil()
                    ? GameActivity.class
                    : CrearPerfilActivity.class;
            startActivity(new Intent(this, destino));
        });

        // Cerrar sesión: vuelve al Login y limpia la pila de pantallas
        findViewById(R.id.btnLogout).setOnClickListener(v -> UiUtils.cerrarSesion(this));
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (storage.existePerfil()) {
            txtEstado.setText(getString(R.string.status_bienvenida,
                    storage.cargarPerfil().getNombre()));
        } else {
            txtEstado.setText(R.string.status);
        }
    }
}
