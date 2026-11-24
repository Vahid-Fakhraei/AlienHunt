package com.example.alienhunt;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.CheckBox;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;

/**
 * Términos y condiciones, justo después del Login.
 * El botón ACEPTAR solo se activa al marcar la casilla. Rechazar (o el botón atrás)
 * vuelve al Login.
 */
public class TerminosActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        UiUtils.configurarPantalla(this);
        setContentView(R.layout.activity_terminos);
        UiUtils.aplicarInsets(findViewById(R.id.main));

        CheckBox chkAcepto = findViewById(R.id.chkAcepto);
        Button btnAceptar = findViewById(R.id.btnAceptar);
        Button btnRechazar = findViewById(R.id.btnRechazar);

        // El botón ACEPTAR solo está activo mientras la casilla está marcada
        chkAcepto.setOnCheckedChangeListener((boton, marcada) -> {
            btnAceptar.setEnabled(marcada);
            btnAceptar.setAlpha(marcada ? 1f : 0.4f);
        });

        btnAceptar.setOnClickListener(v -> {
            startActivity(new Intent(this, MenuActivity.class));
            finish(); // los términos no deben quedar en la pila al volver atrás
        });

        btnRechazar.setOnClickListener(v -> rechazar());

        // El botón atrás del móvil equivale a rechazar
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                rechazar();
            }
        });
    }

    private void rechazar() {
        UiUtils.cerrarSesion(this);
    }
}
